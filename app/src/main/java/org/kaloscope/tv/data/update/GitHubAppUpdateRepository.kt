package org.kaloscope.tv.data.update

import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.common.UpdateFailure
import org.kaloscope.tv.core.model.AppUpdateRelease
import org.kaloscope.tv.data.update.remote.GitHubReleaseDto

class GitHubAppUpdateRepository(
    private val client: OkHttpClient,
    private val json: Json,
    private val downloadDirectory: File,
    private val apiBaseUrl: HttpUrl = "https://api.github.com/".toHttpUrl(),
) : AppUpdateRepository {
    // Contract: https://docs.github.com/en/rest/releases/releases#get-the-latest-release
    // API version 2026-03-10; asset names match .github/workflows/release.yml (v0.3.22 verified).
    override suspend fun check(currentVersion: String): AppResult<AppUpdateRelease?> = updateCall {
        val installed = ReleaseVersion.parse(currentVersion) ?: fail(UpdateFailure.InvalidRelease)
        val url = apiBaseUrl.newBuilder()
            .addPathSegments("repos/kaloscope/android-tv/releases/latest")
            .build()
        val release = request(url, api = true) { response ->
            json.decodeFromString<GitHubReleaseDto>(response.readText(1024 * 1024))
        }
        if (release.draft || release.prerelease) fail(UpdateFailure.InvalidRelease)
        val latest = ReleaseVersion.parse(release.tagName) ?: fail(UpdateFailure.InvalidRelease)
        if (latest <= installed) return@updateCall null

        val version = release.tagName.removePrefix("v")
        val apkName = "kaloscope-tv-$version.apk"
        val apk = release.assets.singleOrNull { it.name == apkName && it.state == "uploaded" }
            ?: fail(UpdateFailure.InvalidRelease)
        if (apk.size !in 1..MAX_APK_BYTES) fail(UpdateFailure.InvalidRelease)
        val digest = apk.digest?.removePrefix("sha256:")?.takeIf { SHA256.matches(it) }
        val checksum = release.assets.singleOrNull {
            it.name == "$apkName.sha256" && it.state == "uploaded"
        }
        val apkUrl = releaseAssetUrl(apk.downloadUrl, release.tagName, apkName)
        val checksumUrl = checksum?.let {
            releaseAssetUrl(it.downloadUrl, release.tagName, "$apkName.sha256")
        }
        if (digest == null && checksumUrl == null) fail(UpdateFailure.InvalidRelease)
        AppUpdateRelease(version, apkUrl, apk.size, digest, checksumUrl)
    }

    override suspend fun download(
        release: AppUpdateRelease,
        onProgress: (Int) -> Unit,
    ): AppResult<File> = updateCall {
        val expectedDigest = release.sha256 ?: run {
            val checksumUrl = release.checksumUrl ?: fail(UpdateFailure.InvalidRelease)
            val checksum = request(checksumUrl.toHttpUrl()) { it.readText(4096) }
            val parts = checksum.trim().split(Regex("\\s+"))
            if (parts.size != 2 || !SHA256.matches(parts[0]) ||
                parts[1].removePrefix("*") != "kaloscope-tv-${release.version}.apk"
            ) {
                fail(UpdateFailure.Integrity)
            }
            parts[0]
        }
        withContext(Dispatchers.IO) {
            if (!downloadDirectory.isDirectory && !downloadDirectory.mkdirs()) {
                fail(UpdateFailure.Storage)
            }
            if (downloadDirectory.usableSpace < release.sizeBytes + 8 * 1024 * 1024) {
                fail(UpdateFailure.Storage)
            }
            // Only this private directory is shared with the installer. Bound retained APKs.
            downloadDirectory.listFiles()?.filter { it.name.startsWith("update-") }
                ?.forEach { it.delete() }
            val file = try {
                File.createTempFile("update-", ".apk", downloadDirectory)
            } catch (_: IOException) {
                fail(UpdateFailure.Storage)
            }
            var completed = false
            try {
                val verifiedFile = downloadVerifiedApk(
                    release = release,
                    expectedDigest = expectedDigest,
                    file = file,
                    onProgress = onProgress,
                )
                completed = true
                verifiedFile
            } finally {
                if (!completed) file.delete()
            }
        }
    }

    private suspend fun downloadVerifiedApk(
        release: AppUpdateRelease,
        expectedDigest: String,
        file: File,
        onProgress: (Int) -> Unit,
    ): File {
        val context = coroutineContext
        return request(release.apkUrl.toHttpUrl(), onCancelledResult = { it.delete() }) { response ->
            var verified = false
            try {
                val body = response.body
                val length = body.contentLength()
                if (length >= 0 && length != release.sizeBytes) fail(UpdateFailure.Integrity)
                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                var lastProgress = -1
                val output = try {
                    file.outputStream()
                } catch (_: IOException) {
                    fail(UpdateFailure.Storage)
                }
                output.use { sink ->
                    body.byteStream().use { source ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            context.ensureActive()
                            val count = source.read(buffer)
                            if (count == -1) break
                            downloaded += count
                            if (downloaded > release.sizeBytes) fail(UpdateFailure.Integrity)
                            try {
                                sink.write(buffer, 0, count)
                            } catch (_: IOException) {
                                fail(UpdateFailure.Storage)
                            }
                            digest.update(buffer, 0, count)
                            val progress = (downloaded * 100 / release.sizeBytes).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
                val actualDigest = digest.digest().joinToString("") { "%02x".format(it) }
                if (downloaded != release.sizeBytes ||
                    !actualDigest.equals(expectedDigest, ignoreCase = true)
                ) {
                    fail(UpdateFailure.Integrity)
                }
                verified = true
                file
            } finally {
                // Cancellation can finish the caller before this callback opens the file.
                if (!verified) file.delete()
            }
        }
    }

    private suspend fun <T> request(
        url: HttpUrl,
        api: Boolean = false,
        onCancelledResult: (T) -> Unit = {},
        read: (Response) -> T,
    ): T = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url)
            .header("User-Agent", "Kaloscope-Android-TV")
            .apply {
                if (api) {
                    header("Accept", "application/vnd.github+json")
                    header("X-GitHub-Api-Version", "2026-03-10")
                }
            }
            .build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val value = response.use {
                        if (!it.isSuccessful) {
                            fail(when (it.code) {
                                403, 429 -> UpdateFailure.RateLimited
                                404 -> if (api) UpdateFailure.NoRelease else UpdateFailure.InvalidRelease
                                else -> UpdateFailure.ServiceUnavailable
                            })
                        }
                        read(it)
                    }
                    continuation.resume(value) { _, result, _ -> onCancelledResult(result) }
                } catch (error: Exception) {
                    continuation.resumeWithException(error)
                }
            }
        })
    }

    private fun Response.readText(limit: Long): String {
        val source = body.source()
        source.request(limit + 1)
        if (source.buffer.size > limit) fail(UpdateFailure.InvalidRelease)
        return source.readUtf8()
    }

    private fun releaseAssetUrl(value: String, tag: String, name: String): String {
        val url = value.toHttpUrl()
        if (url.scheme != "https" || url.host != "github.com" || url.port != 443 ||
            url.username.isNotEmpty() || url.password.isNotEmpty() ||
            url.query != null || url.fragment != null ||
            url.pathSegments != listOf("kaloscope", "android-tv", "releases", "download", tag, name)
        ) {
            fail(UpdateFailure.InvalidRelease)
        }
        return url.toString()
    }

    private suspend fun <T> updateCall(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: UpdateException) {
        AppResult.Failure(AppError.Update(error.reason))
    } catch (_: SocketTimeoutException) {
        AppResult.Failure(AppError.Timeout)
    } catch (_: IOException) {
        AppResult.Failure(AppError.Offline)
    } catch (_: SerializationException) {
        AppResult.Failure(AppError.Update(UpdateFailure.InvalidRelease))
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError.Update(UpdateFailure.InvalidRelease))
    } catch (_: SecurityException) {
        AppResult.Failure(AppError.Update(UpdateFailure.Storage))
    }

    private class UpdateException(val reason: UpdateFailure) : IOException()

    private fun fail(reason: UpdateFailure): Nothing = throw UpdateException(reason)

    private companion object {
        const val MAX_APK_BYTES = 512L * 1024 * 1024
        val SHA256 = Regex("[a-fA-F0-9]{64}")
    }
}
