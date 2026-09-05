package org.kaloscope.tv.core.player

import androidx.media3.common.Player
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser

class PlaybackControllerTimeoutTest {
    @Test
    fun emptyLivePlaylistTimesOutStopsAndCanBeRetried() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        MockWebServer().use { server ->
            // Successful HTTP with no playable segments must not leave preparation unbounded.
            val playlist = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:600\n#EXT-X-MEDIA-SEQUENCE:0\n"
            repeat(2) {
                server.enqueue(MockResponse().setBody(playlist))
            }
            server.start()
            lateinit var controller: PlaybackController
            instrumentation.runOnMainSync {
                controller = PlaybackControllerFactory(instrumentation.targetContext).create(
                    session = Session(
                        server = SavedServer("fixture-server", "Test", "https://server.example"),
                        token = "fixture-token",
                        user = SessionUser(1, "fixture-user", "user"),
                    ),
                    request = PlaybackRequest.NetworkVideo(
                        requestId = "empty-playlist",
                        serverId = "fixture-server",
                        title = "Empty live playlist",
                        source = NetworkPlaybackSource(
                            indexerId = 1,
                            resourceId = "fixture-resource",
                            title = "Empty live playlist",
                            url = server.url("/live.m3u8").toString(),
                            videoType = NetworkVideoType.Hls,
                            danmakus = emptyList(),
                        ),
                    ),
                    subtitles = emptyList(),
                    onProgress = { _, _, _, _ -> },
                )
            }
            try {
                val failure = runBlocking {
                    withTimeout(75_000) {
                        controller.status.first { it.failure != null }.failure
                    }
                }
                assertEquals(PlaybackFailure.Timeout, failure)
                instrumentation.runOnMainSync {
                    assertEquals(Player.STATE_IDLE, controller.player.playbackState)
                    controller.retry()
                    assertNull(controller.status.value.failure)
                    assertTrue(controller.player.playWhenReady)
                    assertEquals(Player.STATE_BUFFERING, controller.player.playbackState)
                }
            } finally {
                instrumentation.runOnMainSync { controller.release() }
            }
        }
    }
}
