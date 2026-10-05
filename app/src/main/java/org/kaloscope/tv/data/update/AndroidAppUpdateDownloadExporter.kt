package org.kaloscope.tv.data.update

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.common.UpdateFailure

class AndroidAppUpdateDownloadExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppUpdateDownloadExporter {
    @Suppress("DEPRECATION")
    override suspend fun save(apk: File, version: String): AppResult<String> = withContext(Dispatchers.IO) {
        if (!apk.isFile || apk.length() == 0L) {
            return@withContext AppResult.Failure(AppError.Update(UpdateFailure.FileMissing))
        }
        if (ReleaseVersion.parse(version) == null) {
            return@withContext AppResult.Failure(AppError.Update(UpdateFailure.InvalidRelease))
        }
        val name = "kaloscope-tv-${version.removePrefix("v")}.apk"
        try {
            val savedName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(apk, name)
            } else {
                copyUpdateApkToDirectory(
                    apk,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    name,
                )
            }
            AppResult.Success(savedName)
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            AppResult.Failure(AppError.Update(UpdateFailure.DownloadExport))
        } catch (_: SecurityException) {
            AppResult.Failure(AppError.Update(UpdateFailure.DownloadExport))
        } catch (_: IllegalArgumentException) {
            AppResult.Failure(AppError.Update(UpdateFailure.DownloadExport))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveToMediaStore(apk: File, name: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Cannot create download")
        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("Cannot open download")
            output.use { copyUpdateApk(apk, it) }
            val savedName = resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                ?: throw IOException("Cannot find download")
            coroutineContext.ensureActive()
            val published = resolver.update(uri, ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }, null, null)
            if (published != 1) throw IOException("Cannot publish download")
            return savedName
        } catch (error: Exception) {
            // File managers must never see a partially copied APK.
            try {
                resolver.delete(uri, null, null)
            } catch (cleanupError: Exception) {
                error.addSuppressed(cleanupError)
            }
            throw error
        }
    }
}

internal suspend fun copyUpdateApkToDirectory(apk: File, directory: File, name: String): String {
    coroutineContext.ensureActive()
    if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create downloads directory")
    val temporary = File.createTempFile(".kaloscope-update-", ".part", directory)
    var destination: File? = null
    var completed = false
    try {
        temporary.outputStream().use { copyUpdateApk(apk, it) }
        val stem = name.removeSuffix(".apk")
        var suffix = 0
        var reserved: File
        do {
            coroutineContext.ensureActive()
            reserved = File(directory, if (suffix == 0) name else "$stem ($suffix).apk")
            suffix++
        } while (!reserved.createNewFile())
        destination = reserved
        // Replace only our empty reservation, never a pre-existing user file.
        if (!temporary.renameTo(reserved)) throw IOException("Cannot publish download")
        completed = true
        return reserved.name
    } finally {
        temporary.delete()
        if (!completed) destination?.delete()
    }
}

internal suspend fun copyUpdateApk(apk: File, output: OutputStream) {
    val expectedSize = apk.length()
    if (expectedSize == 0L) throw IOException("Empty update package")
    var copied = 0L
    apk.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            coroutineContext.ensureActive()
            val count = input.read(buffer)
            if (count == -1) break
            output.write(buffer, 0, count)
            copied += count
        }
    }
    coroutineContext.ensureActive()
    if (copied != expectedSize) throw IOException("Incomplete update package")
}
