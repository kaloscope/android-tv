package org.kaloscope.tv.feature.player

import android.view.KeyEvent
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackRequest

class PlayerLoadingExitTest {
    @get:Rule
    // Media3 listeners require main-thread cleanup; v2 dispatches cleanup on its test scheduler.
    @Suppress("DEPRECATION")
    val composeRule = createComposeRule()

    @Test
    fun stalledNetworkPreparationAllowsTwoBackPressesToExit() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            server.start()
            var exits = 0
            val request = PlaybackRequest.NetworkVideo(
                requestId = "stalled-request",
                serverId = "fixture-server",
                title = "Stalled video",
                source = NetworkPlaybackSource(
                    indexerId = 1,
                    resourceId = "fixture-resource",
                    title = "Stalled video",
                    url = server.url("/video.mp4").toString(),
                    videoType = NetworkVideoType.Mp4,
                    danmakus = emptyList(),
                ),
            )
            composeRule.setContent {
                KaloscopeTheme {
                    val context = LocalContext.current
                    PlayerScreen(
                        session = Session(
                            server = SavedServer("fixture-server", "Test", "https://server.example"),
                            token = "fixture-token",
                            user = SessionUser(1, "fixture-user", "user"),
                        ),
                        state = PlayerUiState.Content(
                            request = request,
                            subtitles = emptyList(),
                            danmakus = emptyList(),
                            extraFailures = emptyMap(),
                        ),
                        controllerFactory = remember(context) { PlaybackControllerFactory(context) },
                        onProgress = { _, _, _, _ -> },
                        onSelectDefinition = { _, _ -> },
                        onPrevious = {},
                        onNext = {},
                        onSelectEpisode = {},
                        onRetryExtra = {},
                        onBack = { exits += 1 },
                    )
                }
            }
            composeRule.onNodeWithTag("player-loading").assertIsDisplayed()

            pressBack()
            composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()
            pressBack()

            composeRule.runOnIdle { assertEquals(1, exits) }
            composeRule.onNodeWithTag("player-exit-confirmation").assertDoesNotExist()
        }
    }

    private fun pressBack() {
        // A frame between Down and Up reproduces the cancellation that occurs on TV remotes.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeySync(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        composeRule.waitForIdle()
        instrumentation.sendKeySync(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        composeRule.waitForIdle()
    }
}
