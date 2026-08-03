package org.kaloscope.tv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.core.player.PlaybackRequest

class PlayerRequestSessionStateTest {
    @Test
    fun `same request retains settings and speed while source identity changes`() {
        val initialRequest = request("request-1", "data:video/mp4;base64,first")
        val changed = PlayerRequestSessionState.initial(
            request = initialRequest,
            tracks = listOf(track("zh")),
        ).copy(
            sessionSettings = PlayerRequestSessionState.initial(
                request = initialRequest,
                tracks = listOf(track("zh")),
            ).sessionSettings.copy(
                subtitleSettings = initialRequest.subtitleSettings.copy(enabled = false),
                selectedSubtitleTrackId = null,
                danmakuSettings = DanmakuSettings(enabled = false),
            ),
            playbackSpeed = 1.5f,
        )

        val updated = changed.updateRequest(
            request = request("request-1", "data:video/MP4;base64,first"),
            tracks = listOf(track("zh"), track("en")),
        )

        assertEquals(1.5f, updated.playbackSpeed, 0f)
        assertEquals(null, updated.sessionSettings.selectedSubtitleTrackId)
        assertFalse(updated.sessionSettings.danmakuSettings.enabled)
        assertEquals("zh", updated.sessionSettings.rememberedSubtitleTrackId)
    }

    @Test
    fun `new request resets settings and speed from request defaults`() {
        val changed = PlayerRequestSessionState.initial(
            request = request("request-1", "data:video/mp4;base64,first"),
            tracks = listOf(track("zh")),
        ).copy(
            sessionSettings = PlayerRequestSessionState.initial(
                request = request("request-1", "data:video/mp4;base64,first"),
                tracks = listOf(track("zh")),
            ).sessionSettings.copy(
                subtitleSettings = SubtitleSettings(enabled = false),
                selectedSubtitleTrackId = null,
                danmakuSettings = DanmakuSettings(enabled = false),
            ),
            playbackSpeed = 1.5f,
        )

        val updated = changed.updateRequest(
            request = request("request-2", "data:video/mp4;base64,second"),
            tracks = listOf(track("en")),
        )

        assertEquals(1f, updated.playbackSpeed, 0f)
        assertEquals("en", updated.sessionSettings.selectedSubtitleTrackId)
        assertTrue(updated.sessionSettings.danmakuSettings.enabled)
    }
}

private fun request(requestId: String, url: String) = PlaybackRequest.NetworkVideo(
    requestId = requestId,
    serverId = "server-id",
    title = "Fixture video",
    source = NetworkPlaybackSource(
        indexerId = 1,
        resourceId = "fixture-video",
        title = "Fixture video",
        url = url,
        videoType = NetworkVideoType.Mp4,
        danmakus = emptyList(),
    ),
    danmakuSettings = DanmakuSettings(enabled = true),
    subtitleSettings = SubtitleSettings(enabled = true, languagePreference = "en"),
)

private fun track(id: String) = SubtitleTrack(
    id = id,
    label = id,
    url = "/$id.vtt",
    language = id,
)
