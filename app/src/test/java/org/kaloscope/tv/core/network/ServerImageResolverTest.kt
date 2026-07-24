package org.kaloscope.tv.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser

class ServerImageResolverTest {
    @Test
    fun `relative server image receives current token`() {
        val request = ServerImageResolver.resolve(
            session = session(),
            rawValue = "posters/series.webp",
        )

        checkNotNull(request)
        assertEquals("https://media.example/_api/posters/series.webp", request.url)
        assertEquals("Token token-one", request.authorization)
    }

    @Test
    fun `proxied remote image is routed through current server`() {
        val request = ServerImageResolver.resolve(
            session = session(),
            rawValue = "https://covers.example/a.webp?proxy=store",
        )

        checkNotNull(request)
        assertEquals(
            "https://media.example/_api/image/proxy?store=true&url=" +
                "https%3A%2F%2Fcovers.example%2Fa.webp%3Fproxy%3Dstore",
            request.url,
        )
        assertEquals("Token token-one", request.authorization)
    }

    @Test
    fun `third party image never receives Kaloscope token`() {
        val request = ServerImageResolver.resolve(
            session = session(),
            rawValue = "https://covers.example/a.webp",
        )

        checkNotNull(request)
        assertEquals("https://covers.example/a.webp", request.url)
        assertNull(request.authorization)
    }

    @Test
    fun `blank image is ignored`() {
        assertNull(ServerImageResolver.resolve(session(), "  "))
    }

    private fun session() = Session(
        server = SavedServer("server-one", "家庭服务器", "https://media.example"),
        token = "token-one",
        user = SessionUser(1, "tv_user", "user"),
    )
}
