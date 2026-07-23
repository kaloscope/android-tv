package org.kaloscope.tv.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginAuthPolicyTest {
    @Test
    fun `attaches token to the same origin`() {
        assertTrue(
            OriginAuthPolicy.shouldAttachToken(
                serverOrigin = "http://192.168.1.2:8000",
                requestUrl = "http://192.168.1.2:8000/_api/auth/current",
            ),
        )
    }

    @Test
    fun `treats implicit and explicit default ports as the same origin`() {
        assertTrue(
            OriginAuthPolicy.shouldAttachToken(
                serverOrigin = "https://media.example.com",
                requestUrl = "https://media.example.com:443/_api/media/stream",
            ),
        )
    }

    @Test
    fun `does not attach token to a subdomain`() {
        assertFalse(
            OriginAuthPolicy.shouldAttachToken(
                serverOrigin = "https://media.example.com",
                requestUrl = "https://cdn.media.example.com/video.m3u8",
            ),
        )
    }

    @Test
    fun `does not attach token when scheme differs`() {
        assertFalse(
            OriginAuthPolicy.shouldAttachToken(
                serverOrigin = "https://media.example.com",
                requestUrl = "http://media.example.com/_api/image/proxy",
            ),
        )
    }
}
