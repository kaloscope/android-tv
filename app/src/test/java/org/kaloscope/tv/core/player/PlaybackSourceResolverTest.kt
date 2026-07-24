package org.kaloscope.tv.core.player

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
    fun `transcode stream URL includes the fixed quality and selected resolution`() {
        val source = PlaybackSourceResolver.localMediaSource(
            session = session(),
            path = "/媒体/Season 01/Episode 1.mkv",
            sourceKind = PlaybackSourceKind.HlsTranscode,
            resolution = TranscodeResolution.P1080,
        )

        assertEquals(
            "http://127.0.0.1:8000/_api/media/stream" +
                "?path=%2F%E5%AA%92%E4%BD%93%2FSeason%2001%2FEpisode%201.mkv" +
                "&transcode=true&quality=medium&resolution=1080p",
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
}

private fun session() = Session(
    server = SavedServer("server-1", "Home", "http://127.0.0.1:8000"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
