package org.kaloscope.tv.core.player

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.NetworkVideoType

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
    fun `transcode stream URL includes selected quality without resolution override`() {
        val source = PlaybackSourceResolver.localMediaSource(
            session = session(),
            path = "/媒体/Season 01/Episode 1.mkv",
            sourceKind = PlaybackSourceKind.HlsTranscode,
            quality = TranscodeQuality.High,
        )

        assertEquals(
            "http://127.0.0.1:8000/_api/media/stream" +
                "?path=%2F%E5%AA%92%E4%BD%93%2FSeason%2001%2FEpisode%201.mkv" +
                "&transcode=true&quality=high",
            source.url,
        )
        assertEquals("application/x-mpegURL", source.mimeType)
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

    @Test
    fun `network HLS resolves same server path without changing its media type`() {
        val source = PlaybackSourceResolver.networkMediaSource(
            session = session(),
            rawUrl = "/_api/media/proxy?id=1",
            videoType = NetworkVideoType.Hls,
        )

        assertEquals("http://127.0.0.1:8000/_api/media/proxy?id=1", source.url)
        assertEquals("application/x-mpegURL", source.mimeType)
    }

    @Test
    fun `third party MP4 remains absolute and unaffiliated with server origin`() {
        val source = PlaybackSourceResolver.networkMediaSource(
            session = session(),
            rawUrl = "https://cdn.example/video.mp4",
            videoType = NetworkVideoType.Mp4,
        )

        assertEquals("https://cdn.example/video.mp4", source.url)
        assertEquals("video/mp4", source.mimeType)
    }

    @Test
    fun `network DASH URL declares DASH media type`() {
        val source = PlaybackSourceResolver.networkMediaSource(
            session = session(),
            rawUrl = "https://cdn.example/manifest.mpd",
            videoType = NetworkVideoType.Dash,
        )

        assertEquals("https://cdn.example/manifest.mpd", source.url)
        assertEquals("application/dash+xml", source.mimeType)
    }

    @Test
    fun `inline DASH rewrites server API base and becomes data URI`() {
        val source = PlaybackSourceResolver.networkMediaSource(
            session = session(),
            rawUrl = """
                <?xml version="1.0"?>
                <MPD><Period><BaseURL>/_api/media/proxy/</BaseURL></Period></MPD>
            """.trimIndent(),
            videoType = NetworkVideoType.Dash,
        )

        assertEquals("application/dash+xml", source.mimeType)
        val encodedManifest = source.url.substringAfter("base64,")
        assertEquals(
            """
                <?xml version="1.0"?>
                <MPD><Period><BaseURL>http://127.0.0.1:8000/_api/media/proxy/</BaseURL></Period></MPD>
            """.trimIndent(),
            String(Base64.getDecoder().decode(encodedManifest), Charsets.UTF_8),
        )
    }
}

private fun session() = Session(
    server = SavedServer("server-1", "Home", "http://127.0.0.1:8000"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
