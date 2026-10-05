package org.kaloscope.tv.feature.player

import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.R
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.ProgressReason

class PlayerRemoteKeyTest {
    @get:Rule
    // Media3 listeners require main-thread cleanup.
    @Suppress("DEPRECATION")
    val composeRule = createComposeRule()

    private val progress = CopyOnWriteArrayList<Progress>()

    @Test
    fun heldConfirmationOnProgressTogglesOncePerPress() = withPlayer {
        assertHeldConfirmationTogglesOnce(hideControls = false)
    }

    @Test
    fun heldConfirmationFromHiddenControlsTogglesOncePerPress() = withPlayer {
        assertHeldConfirmationTogglesOnce(hideControls = true)
    }

    @Test
    fun heldDirectionsKeepRepeatedSeekingAndSubmitOnceOnRelease() = withPlayer {
        focusProgress()
        composeRule.onNodeWithTag("player-progress")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitUntil(10_000) { progress.any { it.reason == ProgressReason.Paused } }
        val pausedPosition = progress.last { it.reason == ProgressReason.Paused }.positionMillis
        // Let the displayed position catch up to the paused Media3 position.
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()

        holdKey(AndroidKeyEvent.KEYCODE_DPAD_RIGHT, repeats = 3)
        composeRule.waitUntil(10_000) { progress.any { it.reason == ProgressReason.Seeked } }
        val forwardSeek = progress.single { it.reason == ProgressReason.Seeked }
        assertEquals((pausedPosition + 40_000).toDouble(), forwardSeek.positionMillis.toDouble(), 500.0)

        holdKey(AndroidKeyEvent.KEYCODE_DPAD_LEFT, repeats = 2)
        composeRule.waitUntil(10_000) { progress.count { it.reason == ProgressReason.Seeked } == 2 }
        val backwardSeek = progress.last { it.reason == ProgressReason.Seeked }
        assertEquals(
            (forwardSeek.positionMillis - 30_000).toDouble(),
            backwardSeek.positionMillis.toDouble(),
            500.0,
        )
        composeRule.onNodeWithTag("player-progress").assertIsFocused()
    }

    private fun assertHeldConfirmationTogglesOnce(hideControls: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf(
            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            AndroidKeyEvent.KEYCODE_ENTER,
            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
        ).forEachIndexed { index, keyCode ->
            focusProgress()
            if (hideControls) {
                InstrumentationRegistry.getInstrumentation()
                    .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
                composeRule.onNodeWithTag("player-progress").assertDoesNotExist()
            }

            holdKey(keyCode, repeats = 3)

            composeRule.onNodeWithTag("player-progress")
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionDown) }
            composeRule.onNodeWithTag("player-play-pause")
                .assertIsFocused()
                .assertContentDescriptionEquals(
                    context.getString(if (index % 2 == 0) R.string.play else R.string.pause),
                )
        }
    }

    private fun focusProgress() {
        composeRule.onNodeWithTag("player-progress")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
    }

    private fun holdKey(keyCode: Int, repeats: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()
        try {
            for (repeatCount in 0..repeats) {
                instrumentation.sendKeySync(
                    AndroidKeyEvent(
                        downTime, SystemClock.uptimeMillis(), AndroidKeyEvent.ACTION_DOWN,
                        keyCode, repeatCount,
                    ),
                )
                composeRule.waitForIdle()
            }
        } finally {
            instrumentation.sendKeySync(
                AndroidKeyEvent(
                    downTime, SystemClock.uptimeMillis(), AndroidKeyEvent.ACTION_UP,
                    keyCode, 0,
                ),
            )
            if (keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT || keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT) {
                // Submit the preview on the Compose clock before waiting for Media3's seek event.
                composeRule.mainClock.advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS)
            }
            composeRule.waitForIdle()
        }
    }

    private fun withPlayer(block: () -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audio = File.createTempFile("player-remote-", ".wav", context.cacheDir)
        val visible = mutableStateOf(true)
        try {
            audio.writeBytes(silentAudio())
            val request = PlaybackRequest.NetworkVideo(
                requestId = "remote-key-request",
                serverId = "fixture-server",
                title = "Remote key fixture",
                source = NetworkPlaybackSource(
                    indexerId = 1,
                    resourceId = "fixture-resource",
                    title = "Remote key fixture",
                    url = Uri.fromFile(audio).toString(),
                    videoType = NetworkVideoType.Unknown,
                    danmakus = emptyList(),
                ),
                resumePositionMillis = 5_000,
            )
            composeRule.setContent {
                if (visible.value) {
                    KaloscopeTheme {
                        val playerContext = LocalContext.current
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
                            controllerFactory = remember(playerContext) {
                                PlaybackControllerFactory(playerContext)
                            },
                            onProgress = { _, position, _, reason ->
                                progress += Progress(position, reason)
                            },
                            onSelectDefinition = { _, _ -> },
                            onPrevious = {},
                            onNext = {},
                            onSelectEpisode = {},
                            onRetryExtra = {},
                            onBack = {},
                        )
                    }
                }
            }
            composeRule.waitUntil(10_000) { progress.any { it.reason == ProgressReason.Started } }
            block()
        } finally {
            composeRule.runOnIdle { visible.value = false }
            composeRule.waitForIdle()
            audio.delete()
        }
    }

    private data class Progress(val positionMillis: Long, val reason: ProgressReason)

    private fun silentAudio(): ByteArray {
        // A local PCM stream exercises real Media3 playback without a server or video decoder.
        val sampleRate = 8_000
        val byteCount = sampleRate * 2 * 60
        return ByteBuffer.allocate(44 + byteCount).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray(Charsets.US_ASCII))
            .putInt(36 + byteCount)
            .put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            .putInt(16)
            .putShort(1)
            .putShort(1)
            .putInt(sampleRate)
            .putInt(sampleRate * 2)
            .putShort(2)
            .putShort(16)
            .put("data".toByteArray(Charsets.US_ASCII))
            .putInt(byteCount)
            .array()
    }
}
