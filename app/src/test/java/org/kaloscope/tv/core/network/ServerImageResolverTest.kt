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
    fun `root relative server image preserves its API path`() {
        val request = ServerImageResolver.resolve(
            session = session(),
            rawValue = "/_api/image/content?id=poster",
        )

        checkNotNull(request)
        assertEquals(
            "https://media.example/_api/image/content?id=poster",
            request.url,
        )
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
    fun `auto policy proxies unmarked remote image on Android`() {
        val request = ServerImageResolver.resolve(
            session = session(),
            rawValue = "https://covers.example/a.webp",
        )

        checkNotNull(request)
        assertEquals(
            "https://media.example/_api/image/proxy?store=false&url=" +
                "https%3A%2F%2Fcovers.example%2Fa.webp",
            request.url,
        )
        assertEquals("Token token-one", request.authorization)
    }

    @Test
    fun `direct policy keeps remote image off the Kaloscope server`() {
        val request = ServerImageResolver.resolve(
            session = session(),
            rawValue = "https://covers.example/a.webp?proxy=store",
            policy = ServerImagePolicy.Direct,
        )

        checkNotNull(request)
        assertEquals(
            "https://covers.example/a.webp?proxy=store",
            request.url,
        )
        assertNull(request.authorization)
    }

    @Test
    fun `proxy policy routes remote image without server storage`() {
        val request = ServerImageResolver.resolve(
            session = session(),
            rawValue = "https://covers.example/a.webp",
            policy = ServerImagePolicy.Proxy,
        )

        checkNotNull(request)
        assertEquals(
            "https://media.example/_api/image/proxy?store=false&url=" +
                "https%3A%2F%2Fcovers.example%2Fa.webp",
            request.url,
        )
        assertEquals("Token token-one", request.authorization)
    }

    @Test
    fun `store policy routes remote image with server storage`() {
        val request = ServerImageResolver.resolve(
            session = session(),
            rawValue = "https://covers.example/a.webp",
            policy = ServerImagePolicy.Store,
        )

        checkNotNull(request)
        assertEquals(
            "https://media.example/_api/image/proxy?store=true&url=" +
                "https%3A%2F%2Fcovers.example%2Fa.webp",
            request.url,
        )
        assertEquals("Token token-one", request.authorization)
    }

    @Test
    fun `server avatar bypasses remote proxy for every policy`() {
        val request = ServerImageResolver.resolve(
            session = session(),
            rawValue = "avatars/user.webp",
            policy = ServerImagePolicy.Store,
        )

        checkNotNull(request)
        assertEquals(
            "https://media.example/_api/avatars/user.webp",
            request.url,
        )
        assertEquals("Token token-one", request.authorization)
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
