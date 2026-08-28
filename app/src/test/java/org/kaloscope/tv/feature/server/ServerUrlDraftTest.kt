package org.kaloscope.tv.feature.server

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerUrlDraftTest {
    @Test
    fun `blank url defaults to http and composes a bare address`() {
        val draft = ServerUrlDraft.from("")

        assertEquals(ServerUrlScheme.Http, draft.scheme)
        assertEquals("", draft.address)
        assertEquals(
            "http://192.168.1.2:8000",
            draft.replaceAddress("192.168.1.2:8000"),
        )
    }

    @Test
    fun `existing https url is separated for editing`() {
        assertEquals(
            ServerUrlDraft(
                scheme = ServerUrlScheme.Https,
                address = "media.example:8443",
            ),
            ServerUrlDraft.from("https://media.example:8443"),
        )
    }

    @Test
    fun `switching scheme preserves the current address`() {
        val draft = ServerUrlDraft.from("https://media.example:8443")

        assertEquals(
            "http://media.example:8443",
            draft.replaceScheme(ServerUrlScheme.Http),
        )
    }

    @Test
    fun `pasted full url overrides the selected scheme`() {
        val draft = ServerUrlDraft.from("http://192.168.1.2:8000")

        assertEquals(
            "https://media.example",
            draft.replaceAddress("https://media.example"),
        )
    }

    @Test
    fun `pasted full url ignores surrounding whitespace`() {
        val draft = ServerUrlDraft.from("http://192.168.1.2:8000")

        assertEquals(
            "https://media.example",
            draft.replaceAddress("  https://media.example  "),
        )
    }
}
