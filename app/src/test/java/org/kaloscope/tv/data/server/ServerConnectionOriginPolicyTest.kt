package org.kaloscope.tv.data.server

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerConnectionOriginPolicyTest {
    @Test
    fun `keeps the requested origin when no origin redirect occurs`() {
        assertEquals(
            "http://192.168.1.2:8000",
            ServerConnectionOriginPolicy.resolve(
                requestedOrigin = "http://192.168.1.2:8000",
                finalUrl = "http://192.168.1.2:8000/_api/system/version".toHttpUrl(),
            ),
        )
    }

    @Test
    fun `accepts an http to https upgrade on the same host`() {
        assertEquals(
            "https://demo.example:8443",
            ServerConnectionOriginPolicy.resolve(
                requestedOrigin = "http://demo.example",
                finalUrl = "https://demo.example:8443/_api/system/version".toHttpUrl(),
            ),
        )
    }

    @Test
    fun `rejects a redirect to another host`() {
        assertNull(
            ServerConnectionOriginPolicy.resolve(
                requestedOrigin = "http://media.example",
                finalUrl = "https://other.example/_api/system/version".toHttpUrl(),
            ),
        )
    }

    @Test
    fun `rejects an https to http downgrade`() {
        assertNull(
            ServerConnectionOriginPolicy.resolve(
                requestedOrigin = "https://media.example",
                finalUrl = "http://media.example/_api/system/version".toHttpUrl(),
            ),
        )
    }
}
