package org.kaloscope.tv.core.network

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KaloscopeApiContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: KaloscopeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ApiClientFactory(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        ).create(server.url("/").toString().removeSuffix("/"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `version uses public system endpoint`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"request_id":"test","status":200,"message":"","data":{"version":"1.2.3"}}""",
            ),
        )

        val response = api.getVersion()

        assertEquals("1.2.3", response.data.version)
        assertEquals("/_api/system/version", server.takeRequest().path)
    }

    @Test
    fun `login sends form fields and parses fixture`() = runTest {
        server.enqueue(jsonResponse(fixture("auth-login-success.json")))

        val response = api.login("tv user", "secret+value")
        val request = server.takeRequest()

        assertEquals("/_api/auth/login", request.path)
        assertEquals("POST", request.method)
        assertTrue(
            request.getHeader("Content-Type")
                .orEmpty()
                .startsWith("application/x-www-form-urlencoded"),
        )
        assertEquals("username=tv+user&password=secret%2Bvalue", request.body.readUtf8())
        assertEquals("fixture-token-not-for-production", response.data.token)
        assertEquals("tv_user", response.data.user.username)
    }

    @Test
    fun `current user sends token header`() = runTest {
        server.enqueue(jsonResponse(fixture("auth-current-success.json")))

        val response = api.getCurrentUser("Token fixture-token")
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals("/_api/auth/current", request.path)
        assertEquals("Token fixture-token", request.getHeader("Authorization"))
        assertEquals(1L, response.data.id)
    }

    @Test
    fun `history requests recent videos and parses media`() = runTest {
        server.enqueue(jsonResponse(fixture("history-video-list-success.json")))

        val response = api.getVideoHistory("Token fixture-token")
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        checkNotNull(request)
        assertEquals(
            "/_api/user/history/list?rel_type=video&page_num=1&page_size=20&ordering=-updated_at",
            request.path,
        )
        assertEquals("Token fixture-token", request.getHeader("Authorization"))
        assertEquals(301L, response.data.items.single().media?.id)
        assertEquals(63, response.data.items.single().percentage)
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("fixtures/api/$name")).readText()

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
