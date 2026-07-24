package org.kaloscope.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser

class PlaybackSourceResolverTest {
    @Test
    fun `direct stream URL safely encodes the server media path`() {
        val url = PlaybackSourceResolver.directStreamUrl(
            session = session(),
            path = "/媒体/Season 01/Episode 1.mkv",
        )

        assertEquals(
            "http://127.0.0.1:8000/_api/media/stream" +
                "?path=%2F%E5%AA%92%E4%BD%93%2FSeason%2001%2FEpisode%201.mkv",
            url,
        )
    }

    @Test
    fun `relative subtitle URL resolves against the current server`() {
        assertEquals(
            "http://127.0.0.1:8000/_api/subtitle/content?path=fixture",
            PlaybackSourceResolver.resolveServerResource(
                session(),
                "/_api/subtitle/content?path=fixture",
            ),
        )
    }
}

private fun session() = Session(
    server = SavedServer("server-1", "Home", "http://127.0.0.1:8000"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
