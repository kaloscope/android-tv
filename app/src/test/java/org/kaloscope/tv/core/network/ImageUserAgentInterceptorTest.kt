package org.kaloscope.tv.core.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUserAgentInterceptorTest {
    @Test
    fun `default image agent is accepted by a browser-only image host`() {
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    if (request.getHeader("User-Agent")?.startsWith("Mozilla/5.0") == true) {
                        MockResponse().setResponseCode(200)
                    } else {
                        MockResponse().setResponseCode(500)
                    }
            }
            server.start()
            val client = OkHttpClient.Builder()
                .addInterceptor(ImageUserAgentInterceptor)
                .build()

            client.newCall(Request.Builder().url(server.url("/cover.png")).build())
                .execute().use { response -> assertEquals(200, response.code) }

            val request = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertNull(request.getHeader("Authorization"))
        }
    }

    @Test
    fun `explicit image agent and authorization are preserved`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse())
            server.start()
            val client = OkHttpClient.Builder()
                .addInterceptor(ImageUserAgentInterceptor)
                .build()
            val request = Request.Builder()
                .url(server.url("/_api/image/proxy"))
                .header("User-Agent", "Custom image agent")
                .header("Authorization", "Token fixture-token")
                .build()

            client.newCall(request).execute().close()

            val recorded = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertEquals("Custom image agent", recorded.getHeader("User-Agent"))
            assertEquals("Token fixture-token", recorded.getHeader("Authorization"))
        }
    }
}
