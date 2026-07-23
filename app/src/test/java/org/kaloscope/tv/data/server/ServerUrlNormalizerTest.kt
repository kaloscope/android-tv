package org.kaloscope.tv.data.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ServerUrlNormalizerTest {
    @Test
    fun `normalizes whitespace and trailing slashes`() {
        assertEquals(
            "http://192.168.1.2:8000",
            ServerUrlNormalizer.normalize("  http://192.168.1.2:8000///  "),
        )
    }

    @Test
    fun `rejects paths beyond the server origin`() {
        assertThrows(InvalidServerUrl::class.java) {
            ServerUrlNormalizer.normalize("https://media.example.com/dashboard")
        }
    }

    @Test
    fun `rejects unsupported schemes`() {
        assertThrows(InvalidServerUrl::class.java) {
            ServerUrlNormalizer.normalize("ftp://media.example.com")
        }
    }
}
