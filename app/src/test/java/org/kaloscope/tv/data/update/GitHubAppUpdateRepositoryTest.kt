package org.kaloscope.tv.data.update

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.common.UpdateFailure
import org.kaloscope.tv.core.model.AppUpdateRelease

class GitHubAppUpdateRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var server: MockWebServer
    private lateinit var directory: File
    private lateinit var repository: GitHubAppUpdateRepository
    private val fixture = requireNotNull(
        javaClass.classLoader?.getResource("fixtures/api/github_latest_release.json"),
    ).readText()
    private val digest = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        directory = temporary.newFolder("updates")
        repository = repository()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `latest release contract uses public versioned API without authorization`() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture))
        val release = success(repository.check("0.3.22"))
        assertNotNull(release)
        assertEquals("0.3.23", release?.version)
        assertEquals(digest, release?.sha256)
        val request = server.takeRequest()
        assertEquals("/repos/kaloscope/android-tv/releases/latest", request.path)
        assertEquals("GET", request.method)
        assertEquals("application/vnd.github+json", request.getHeader("Accept"))
        assertEquals("2026-03-10", request.getHeader("X-GitHub-Api-Version"))
        assertNotNull(request.getHeader("User-Agent"))
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun `equal or older latest release never offers a downgrade`() = runBlocking {
        listOf("0.3.23", "0.3.24", "1.0.0").forEach { current ->
            server.enqueue(MockResponse().setBody(fixture))
            assertNull(success(repository.check(current)))
        }
    }

    @Test
    fun `invalid metadata assets and external URLs fail safely`() = runBlocking {
        listOf(
            fixture.replace("\"draft\": false", "\"draft\": true"),
            fixture.replace("\"prerelease\": false", "\"prerelease\": true"),
            fixture.replace("v0.3.23", "v0.3.23-beta"),
            fixture.replace("kaloscope-tv-", "other-app-"),
            fixture.replace("https://github.com/", "https://example.com/"),
            fixture.replace("https://github.com/", "http://github.com/"),
            fixture.replace("/kaloscope/android-tv/", "/other/android-tv/"),
            fixture.replace("\"size\": 3", "\"size\": 0"),
            "{invalid json",
        ).forEach { body ->
            server.enqueue(MockResponse().setBody(body))
            assertEquals(AppError.Update(UpdateFailure.InvalidRelease), failure(repository.check("0.3.22")))
        }
    }

    @Test
    fun `GitHub errors stay separate from server authentication`() = runBlocking {
        mapOf(
            403 to UpdateFailure.RateLimited,
            429 to UpdateFailure.RateLimited,
            404 to UpdateFailure.NoRelease,
            503 to UpdateFailure.ServiceUnavailable,
            401 to UpdateFailure.ServiceUnavailable,
        ).forEach { (status, reason) ->
            server.enqueue(MockResponse().setResponseCode(status))
            assertEquals(AppError.Update(reason), failure(repository.check("0.3.22")))
        }
    }

    @Test
    fun `check timeout is recoverable`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertEquals(AppError.Timeout, failure(repository(timeoutMillis = 100).check("0.3.22")))
        server.enqueue(MockResponse().setBody(fixture))
        assertNotNull(success(repository.check("0.3.22")))
    }

    @Test
    fun `verified download reports progress and stores only expected bytes`() = runBlocking {
        server.enqueue(MockResponse().setBody("abc"))
        val progress = mutableListOf<Int>()
        val file = success(repository.download(release(), progress::add))
        assertEquals("abc", file.readText())
        assertEquals(100, progress.last())
        assertEquals(directory.canonicalPath, requireNotNull(file.parentFile).canonicalPath)
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `checksum asset is used when release digest is absent`() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture.replace("sha256:$digest", "")))
        assertNull(success(repository.check("0.3.22"))?.sha256)
        server.enqueue(MockResponse().setBody("$digest  kaloscope-tv-0.3.23.apk\n"))
        server.enqueue(MockResponse().setBody("abc"))
        val file = success(repository.download(release().copy(sha256 = null)) {})
        assertEquals("abc", file.readText())
    }

    @Test
    fun `missing digest and checksum never allow an unverified APK`() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture.replace("sha256:$digest", "").replace(".apk.sha256", ".txt")))
        assertEquals(AppError.Update(UpdateFailure.InvalidRelease), failure(repository.check("0.3.22")))
    }

    @Test
    fun `incorrect digest or length deletes partial APK and permits retry`() = runBlocking {
        listOf("bad", "ab", "abcd").forEach { body ->
            server.enqueue(MockResponse().setBody(body))
            assertEquals(AppError.Update(UpdateFailure.Integrity), failure(repository.download(release()) {}))
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        }
        server.enqueue(MockResponse().setBody("abc"))
        assertTrue(success(repository.download(release()) {}).isFile)
    }

    @Test
    fun `checksum for a different file is rejected before download`() = runBlocking {
        server.enqueue(MockResponse().setBody("$digest  other.apk\n"))
        assertEquals(
            AppError.Update(UpdateFailure.Integrity),
            failure(repository.download(release().copy(sha256 = null)) {}),
        )
        assertEquals(1, server.requestCount)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `interrupted download removes incomplete file`() = runBlocking {
        server.enqueue(MockResponse().setBody("abc").setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        val result = repository.download(release()) {}
        assertTrue(result is AppResult.Failure)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `cancellation closes a stalled download and removes temporary APK`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val downloading = async { repository.download(release()) {} }
        // Let the repository reach IO before waiting on the blocking server request queue.
        kotlinx.coroutines.yield()
        assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
        downloading.cancelAndJoin()
        assertTrue(downloading.isCancelled)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `cancellation immediately before opening output cannot recreate a partial APK`() = runBlocking {
        val headersRead = CountDownLatch(1)
        val continueResponse = CountDownLatch(1)
        val responseClosed = CountDownLatch(1)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val source = object : ForwardingSource(Buffer().writeUtf8("abc")) {
                override fun close() {
                    super.close()
                    responseClosed.countDown()
                }
            }.buffer()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body(object : ResponseBody() {
                    override fun contentType() = null
                    override fun source() = source
                    override fun contentLength(): Long {
                        headersRead.countDown()
                        check(continueResponse.await(3, TimeUnit.SECONDS))
                        return 3
                    }
                }).build()
        }.build()
        val repository = GitHubAppUpdateRepository(client, Json, directory, server.url("/"))
        val downloading = async { repository.download(release()) {} }
        kotlinx.coroutines.yield()
        try {
            assertTrue(headersRead.await(3, TimeUnit.SECONDS))
            downloading.cancelAndJoin()
        } finally {
            continueResponse.countDown()
        }
        assertTrue(responseClosed.await(3, TimeUnit.SECONDS))
        kotlinx.coroutines.withTimeout(3000) {
            while (directory.listFiles().orEmpty().isNotEmpty()) kotlinx.coroutines.delay(10)
        }
    }

    @Test
    fun `unwritable download directory reports storage failure`() = runBlocking {
        val file = temporary.newFile("not-a-directory")
        val repository = GitHubAppUpdateRepository(OkHttpClient(), Json, file, server.url("/"))
        assertEquals(AppError.Update(UpdateFailure.Storage), failure(repository.download(release()) {}))
        assertFalse(file.isDirectory)
    }

    private fun repository(timeoutMillis: Long = 1000) = GitHubAppUpdateRepository(
        OkHttpClient.Builder().readTimeout(timeoutMillis, TimeUnit.MILLISECONDS).build(),
        Json { ignoreUnknownKeys = true },
        directory,
        server.url("/"),
    )

    private fun release() = AppUpdateRelease(
        "0.3.23", server.url("/app.apk").toString(), 3, digest, server.url("/app.apk.sha256").toString(),
    )

    private fun <T> success(result: AppResult<T>): T {
        assertTrue("Expected success, got $result", result is AppResult.Success)
        return (result as AppResult.Success).value
    }

    private fun failure(result: AppResult<*>): AppError {
        assertTrue("Expected failure, got $result", result is AppResult.Failure)
        return (result as AppResult.Failure).error
    }
}
