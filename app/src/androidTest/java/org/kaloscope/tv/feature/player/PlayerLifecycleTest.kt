package org.kaloscope.tv.feature.player

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.R
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.NetworkDefinition
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.core.player.PlaybackController
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestNavigator
import org.kaloscope.tv.core.player.PlaybackResumeState
import org.kaloscope.tv.core.player.ProgressReason

class PlayerLifecycleTest {
    @get:Rule
    // Media3 listeners require main-thread cleanup.
    @Suppress("DEPRECATION")
    val composeRule = createComposeRule()

    private val progress = CopyOnWriteArrayList<Progress>()
    private lateinit var request: MutableState<PlaybackRequest.NetworkVideo>
    private lateinit var subtitles: MutableState<List<SubtitleTrack>>

    @Test
    fun endedPlaybackRestartsWithCenterOnProgress() {
        assertEndedPlaybackRestarts("player-progress")
    }

    @Test
    fun endedPlaybackRestartsWithCenterOnPlayButton() {
        assertEndedPlaybackRestarts("player-play-pause")
    }

    @Test
    fun endedReplayCancelsQueuedSeek() =
        withPlayer(resumePositionMillis = 59_000) { owner ->
            pressProgressKey(Key.DirectionDown)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodes(
                    hasTestTag("player-play-pause") and
                        hasContentDescription(context.getString(R.string.play)),
                ).fetchSemanticsNodes().isNotEmpty()
            }
            val startCount = progress.count { it.reason == ProgressReason.Started }
            composeRule.mainClock.autoAdvance = false
            try {
                // Keep the seek's settling delay pending until Center starts replay.
                pressProgressKey(Key.DirectionLeft)
                pressProgressKey(Key.DirectionCenter)
                composeRule.mainClock.advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS + 500)
                awaitStartAfter(startCount)

                assertEquals(
                    listOf(0L),
                    progress.filter { it.reason == ProgressReason.Seeked }.map { it.positionMillis },
                )
                assertTrue(stop(owner) < 10_000)
            } finally {
                composeRule.mainClock.autoAdvance = true
            }
        }

    @Test
    fun pausingKeepsQueuedSeek() = withPlayer { owner ->
        val seekCount = progress.count { it.reason == ProgressReason.Seeked }
        composeRule.mainClock.autoAdvance = false
        try {
            pressProgressKey(Key.DirectionRight)
            pressProgressKey(Key.DirectionCenter)
            composeRule.mainClock.advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS + 500)
            composeRule.waitUntil(10_000) {
                progress.count { it.reason == ProgressReason.Seeked } > seekCount
            }

            val position = progress.last { it.reason == ProgressReason.Seeked }.positionMillis
            assertTrue(position >= 10_000)
            assertPositionNear(position, Progress(stop(owner), ProgressReason.Exit))
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun delayedPositionSamplingDoesNotLeaveSeekPreviewStuck() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val factory = ObservedPlaybackControllerFactory(instrumentation.targetContext)
        withPlayer(factory = factory) { _ ->
            // Use Media3's playback clock without the emulator AudioTrack's output latency.
            composeRule.runOnUiThread {
                val player = factory.controller.player
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
            }
            composeRule.waitForIdle()
            composeRule.mainClock.autoAdvance = false
            try {
                pressProgressKey(Key.DirectionRight)
                // Stop at submission before another position poll can acknowledge this seek.
                composeRule.mainClock.advanceTimeBy(
                    PlayerSeekCoordinator.SETTLE_DELAY_MILLIS,
                    ignoreFrameDuration = true,
                )
                composeRule.waitUntil(10_000) {
                    progress.any { it.reason == ProgressReason.Seeked }
                }
                val firstSeek = progress.last { it.reason == ProgressReason.Seeked }.positionMillis

                // Observe Media3 directly while Compose's periodic position polling stays frozen.
                val samplingTime = composeRule.mainClock.currentTime
                var nativePosition = firstSeek
                try {
                    composeRule.waitUntil(10_000) {
                        SystemClock.sleep(100)
                        instrumentation.runOnMainSync {
                            assertTrue(factory.controller.player.playWhenReady)
                            nativePosition = factory.controller.player.currentPosition
                        }
                        nativePosition - firstSeek > 1_500
                    }
                } catch (timeout: ComposeTimeoutException) {
                    throw AssertionError(
                        "Native playback stalled at $nativePosition after seek to $firstSeek: " +
                            factory.controller.status.value,
                        timeout,
                    )
                }
                val pauseCount = progress.count { it.reason == ProgressReason.Paused }
                instrumentation.sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_DPAD_CENTER)
                composeRule.waitUntil(10_000) {
                    progress.count { it.reason == ProgressReason.Paused } > pauseCount
                }
                val pausedPosition = progress.last { it.reason == ProgressReason.Paused }.positionMillis
                assertTrue(
                    "Playback did not advance beyond the seek: $progress",
                    pausedPosition - firstSeek > 1_500,
                )
                assertEquals(samplingTime, composeRule.mainClock.currentTime)
                composeRule.mainClock.advanceTimeBy(500)

                pressProgressKey(Key.DirectionRight)
                composeRule.mainClock.advanceTimeBy(
                    PlayerSeekCoordinator.SETTLE_DELAY_MILLIS,
                    ignoreFrameDuration = true,
                )
                composeRule.waitUntil(10_000) {
                    progress.count { it.reason == ProgressReason.Seeked } == 2
                }

                assertPositionNear(
                    pausedPosition + 10_000,
                    progress.last { it.reason == ProgressReason.Seeked },
                )
            } finally {
                composeRule.mainClock.autoAdvance = true
            }
        }
    }

    @Test
    fun resumingAtEndExposesControlsAndAllowsReplay() =
        withPlayer(resumePositionMillis = 60_000, awaitInitialStart = false) { owner ->
            // Starting at the exact duration need not emit the initial Started progress event.
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithTag("player-progress").fetchSemanticsNodes().isNotEmpty()
            }
            pressProgressKey(Key.DirectionDown)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodes(
                    hasTestTag("player-play-pause") and
                        hasContentDescription(context.getString(R.string.play)),
                ).fetchSemanticsNodes().isNotEmpty()
            }
            val startCount = progress.count { it.reason == ProgressReason.Started }

            composeRule.onNodeWithTag("player-play-pause")
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionCenter) }
            awaitStartAfter(startCount)

            assertEquals(0L, progress.last { it.reason == ProgressReason.Seeked }.positionMillis)
            composeRule.onNodeWithTag("player-play-pause")
                .assertContentDescriptionEquals(context.getString(R.string.pause))
            assertTrue(stop(owner) < 10_000)
        }

    @Test
    fun failureWithHiddenControlsKeepsRetryFocusAndAcceptsCenter() {
        assertFailureAllowsRemoteRetry(showPreview = false, confirmKey = Key.DirectionCenter)
    }

    @Test
    fun failureWithPreviewKeepsRetryFocusAndAcceptsEnter() {
        assertFailureAllowsRemoteRetry(showPreview = true, confirmKey = Key.Enter)
    }

    @Test
    fun failureDuringSeekPreviewDoesNotOffsetNextSeekAfterRetry() = withPlayer { _ ->
        pressProgressKey(Key.Enter)
        composeRule.mainClock.advanceTimeBy(500)
        val seekCount = progress.count { it.reason == ProgressReason.Seeked }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audio = File(checkNotNull(Uri.parse(request.value.source.url).path))
        val audioBytes = audio.readBytes()
        val subtitle = File.createTempFile("player-seek-retry-", ".vtt", context.cacheDir)
        var rightHeld = false
        try {
            subtitle.writeText("WEBVTT\n\n00:00:00.000 --> 00:01:00.000\nSeek retry fixture\n")
            composeRule.onNodeWithTag("player-progress")
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .performKeyInput { keyDown(Key.DirectionRight) }
            rightHeld = true

            // Fail the current controller while the seek preview still awaits key-up.
            assertTrue(audio.delete())
            composeRule.runOnIdle {
                subtitles.value = listOf(
                    SubtitleTrack(
                        id = "seek-retry-fixture",
                        label = "English",
                        url = Uri.fromFile(subtitle).toString(),
                        language = "en",
                    ),
                )
            }
            composeRule.waitUntil(10_000) { progress.any { it.reason == ProgressReason.Error } }
            val retry = composeRule.onNodeWithText(context.getString(R.string.retry))
            retry.assertIsDisplayed().assertIsFocused()
            composeRule.onRoot().performKeyInput { keyUp(Key.DirectionRight) }
            rightHeld = false
            assertEquals(seekCount, progress.count { it.reason == ProgressReason.Seeked })

            audio.writeBytes(audioBytes)
            val startCount = progress.count { it.reason == ProgressReason.Started }
            retry.performKeyInput { pressKey(Key.DirectionCenter) }
            awaitStartAfter(startCount)
            pressProgressKey(Key.Enter)
            val pausedPosition = progress.last { it.reason == ProgressReason.Paused }.positionMillis
            composeRule.mainClock.advanceTimeBy(500)

            pressProgressKey(Key.DirectionRight)
            composeRule.waitUntil(10_000) {
                progress.count { it.reason == ProgressReason.Seeked } > seekCount
            }
            assertPositionNear(
                pausedPosition + 10_000,
                progress.last { it.reason == ProgressReason.Seeked },
            )
        } finally {
            if (rightHeld) composeRule.onRoot().performKeyInput { keyUp(Key.DirectionRight) }
            subtitle.delete()
        }
    }

    private fun assertFailureAllowsRemoteRetry(showPreview: Boolean, confirmKey: Key) =
        withPlayer { _ ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            pressProgressKey(Key.Enter)
            instrumentation.sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
            composeRule.onNodeWithTag("player-progress").assertDoesNotExist()
            if (showPreview) {
                composeRule.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
                composeRule.onNodeWithTag("player-info-preview").assertIsDisplayed()
            }

            val audio = File(checkNotNull(Uri.parse(request.value.source.url).path))
            val audioBytes = audio.readBytes()
            val subtitle = File.createTempFile("player-retry-", ".vtt", context.cacheDir)
            try {
                subtitle.writeText("WEBVTT\n\n00:00:00.000 --> 00:01:00.000\nRetry fixture\n")
                // Reload the same source after removing the fixture so failure occurs after
                // hiding controls, without replacing the playback session or its control layer.
                assertTrue(audio.delete())
                composeRule.runOnIdle {
                    subtitles.value = listOf(
                        SubtitleTrack(
                            id = "retry-fixture",
                            label = "English",
                            url = Uri.fromFile(subtitle).toString(),
                            language = "en",
                        ),
                    )
                }
                composeRule.waitUntil(10_000) { progress.any { it.reason == ProgressReason.Error } }
                val retry = composeRule.onNodeWithText(context.getString(R.string.retry))
                retry.assertIsDisplayed().assertIsFocused()

                instrumentation.sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
                composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()
                for (key in listOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)) {
                    retry.performKeyInput { pressKey(key) }.assertIsFocused()
                }
                composeRule.onNodeWithTag("player-exit-confirmation").assertDoesNotExist()

                audio.writeBytes(audioBytes)
                val startsBefore = progress.count { it.reason == ProgressReason.Started }
                retry.performKeyInput { pressKey(confirmKey) }
                awaitStartAfter(startsBefore)

                retry.assertDoesNotExist()
                // Retry can briefly lose duration and focus Play/Pause until it is known again.
                composeRule.onAllNodes(
                    (hasTestTag("player-progress") or hasTestTag("player-play-pause")) and isFocused(),
                ).assertCountEquals(1)
            } finally {
                subtitle.delete()
            }
        }

    private fun assertEndedPlaybackRestarts(controlTag: String) =
        withPlayer(resumePositionMillis = 59_000) { owner ->
            pressProgressKey(Key.DirectionDown)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodes(
                    hasTestTag("player-play-pause") and
                        hasContentDescription(context.getString(R.string.play)),
                ).fetchSemanticsNodes().isNotEmpty()
            }
            val startCount = progress.count { it.reason == ProgressReason.Started }

            composeRule.onNodeWithTag(controlTag)
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .performKeyInput { pressKey(Key.DirectionCenter) }

            awaitStartAfter(startCount)
            assertEquals(0L, progress.last { it.reason == ProgressReason.Seeked }.positionMillis)
            pressProgressKey(Key.DirectionDown)
            composeRule.onNodeWithTag("player-play-pause").assertContentDescriptionEquals(
                context.getString(R.string.pause),
            )
            assertTrue(stop(owner) < 10_000)
        }

    @Test
    fun pausedPlaybackRestoresLatestPositionAndStaysPaused() = withPlayer { owner ->
        val pauseCount = progress.count { it.reason == ProgressReason.Paused }
        pressProgressKey(Key.Enter)
        assertEquals(pauseCount + 1, progress.count { it.reason == ProgressReason.Paused })
        pressProgressKey(Key.DirectionRight)
        composeRule.waitUntil(10_000) {
            progress.any { it.reason == ProgressReason.Seeked && it.positionMillis >= 10_000 }
        }

        val stoppedPosition = stop(owner)
        val startCount = progress.count { it.reason == ProgressReason.Started }
        composeRule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        val resumed = awaitStartAfter(startCount)

        assertPositionNear(stoppedPosition, resumed)
        pressProgressKey(Key.DirectionDown)
        composeRule.onNodeWithTag("player-play-pause").assertContentDescriptionEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.play),
        )
        assertPositionNear(stoppedPosition, Progress(stop(owner), ProgressReason.Exit))
    }

    @Test
    fun playingPlaybackRestoresLatestPositionAndKeepsPlaying() = withPlayer { owner ->
        pressProgressKey(Key.DirectionRight)
        composeRule.waitUntil(10_000) {
            progress.any { it.reason == ProgressReason.Seeked && it.positionMillis >= 10_000 }
        }

        val stoppedPosition = stop(owner)
        val startCount = progress.count { it.reason == ProgressReason.Started }
        composeRule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        val resumed = awaitStartAfter(startCount)

        assertPositionNear(stoppedPosition, resumed)
        pressProgressKey(Key.DirectionDown)
        composeRule.onNodeWithTag("player-play-pause").assertContentDescriptionEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pause),
        )
    }

    @Test
    fun changingQualityWhilePausedRetainsPositionAndStaysPaused() = withPlayer { owner ->
        pressProgressKey(Key.Enter)
        pressProgressKey(Key.DirectionRight)
        composeRule.waitUntil(10_000) {
            progress.any { it.reason == ProgressReason.Seeked && it.positionMillis >= 10_000 }
        }
        val pausedPosition = progress.last { it.reason == ProgressReason.Seeked }.positionMillis

        val resumed = selectAlternateDefinition()

        assertPositionNear(pausedPosition, resumed)
        pressProgressKey(Key.DirectionDown)
        composeRule.onNodeWithTag("player-play-pause").assertContentDescriptionEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.play),
        )
        assertPositionNear(pausedPosition, Progress(stop(owner), ProgressReason.Exit))
    }

    @Test
    fun changingQualityWhilePlayingRetainsPositionAndKeepsPlaying() = withPlayer { _ ->
        pressProgressKey(Key.DirectionRight)
        composeRule.waitUntil(10_000) {
            progress.any { it.reason == ProgressReason.Seeked && it.positionMillis >= 10_000 }
        }

        val resumed = selectAlternateDefinition()

        assertPositionNear(
            progress.last { it.reason == ProgressReason.Exit }.positionMillis,
            resumed,
        )
        pressProgressKey(Key.DirectionDown)
        composeRule.onNodeWithTag("player-play-pause").assertContentDescriptionEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pause),
        )
    }

    @Test
    fun changingQualityWhileStoppedRetainsPausedResumeState() = withPlayer { owner ->
        pressProgressKey(Key.Enter)
        val stoppedPosition = stop(owner)
        val startCount = progress.count { it.reason == ProgressReason.Started }
        composeRule.runOnIdle {
            request.value = requireNotNull(
                PlaybackRequestNavigator.selectDefinition(request.value, 1, stoppedPosition),
            )
        }
        composeRule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        val resumed = awaitStartAfter(startCount)

        assertPositionNear(stoppedPosition, resumed)
        pressProgressKey(Key.DirectionDown)
        composeRule.onNodeWithTag("player-play-pause").assertContentDescriptionEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.play),
        )
        assertPositionNear(stoppedPosition, Progress(stop(owner), ProgressReason.Exit))
    }

    @Test
    fun changingEpisodeWhileStoppedDiscardsPreviousResumeState() = withPlayer { owner ->
        pressProgressKey(Key.Enter)
        stop(owner)
        val startCount = progress.count { it.reason == ProgressReason.Started }
        composeRule.runOnIdle {
            request.value = request.value.copy(
                source = request.value.source.copy(resourceId = "next-episode"),
                resumePositionMillis = 2_000,
            )
        }
        composeRule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        val resumed = awaitStartAfter(startCount)

        assertPositionNear(2_000, resumed)
        pressProgressKey(Key.DirectionDown)
        composeRule.onNodeWithTag("player-play-pause").assertContentDescriptionEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pause),
        )
    }

    @Test
    fun retriedSubtitlesLoadWithoutResumingPausedPlayback() = withPlayer { owner ->
        pressProgressKey(Key.Enter)
        pressProgressKey(Key.DirectionRight)
        composeRule.waitUntil(10_000) {
            progress.any { it.reason == ProgressReason.Seeked && it.positionMillis >= 10_000 }
        }
        val pausedPosition = progress.last { it.reason == ProgressReason.Seeked }.positionMillis

        loadRetriedSubtitles()

        assertPositionNear(pausedPosition, progress.last { it.reason == ProgressReason.Started })
        pressProgressKey(Key.DirectionDown)
        composeRule.onNodeWithTag("player-play-pause").assertContentDescriptionEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.play),
        )
        assertPositionNear(pausedPosition, Progress(stop(owner), ProgressReason.Exit))
    }

    @Test
    fun retriedSubtitlesLoadWhilePlaybackContinues() = withPlayer { _ ->
        pressProgressKey(Key.DirectionRight)
        composeRule.waitUntil(10_000) {
            progress.any { it.reason == ProgressReason.Seeked && it.positionMillis >= 10_000 }
        }
        val positionBeforeRetry = progress.last { it.reason == ProgressReason.Seeked }.positionMillis

        loadRetriedSubtitles()

        assertTrue(
            progress.last { it.reason == ProgressReason.Started }.positionMillis >= positionBeforeRetry,
        )
        pressProgressKey(Key.DirectionDown)
        composeRule.onNodeWithTag("player-play-pause").assertContentDescriptionEquals(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pause),
        )
    }

    private fun selectAlternateDefinition(): Progress {
        val exitCount = progress.count { it.reason == ProgressReason.Exit }
        pressProgressKey(Key.DirectionDown)
        composeRule.onNodeWithTag("player-quality")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("720p")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(1, request.value.source.selectedDefinitionIndex)
            assertEquals(exitCount + 1, progress.count { it.reason == ProgressReason.Exit })
        }
        // The old player can become ready while the quality drawer is opening.
        val releasedIndex = progress.indexOfLast { it.reason == ProgressReason.Exit }
        return awaitStartAfter(
            progress.take(releasedIndex + 1).count { it.reason == ProgressReason.Started },
        )
    }

    private fun loadRetriedSubtitles() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody("WEBVTT\n\n00:00:00.000 --> 00:01:00.000\nRetried subtitle\n"),
            )
            server.start()
            val startCount = progress.count { it.reason == ProgressReason.Started }
            val exitCount = progress.count { it.reason == ProgressReason.Exit }
            composeRule.runOnIdle {
                subtitles.value = listOf(
                    SubtitleTrack(
                        id = "retried-subtitle",
                        label = "English",
                        url = server.url("/subtitle.vtt").toString(),
                        language = "en",
                    ),
                )
            }
            composeRule.waitForIdle()
            composeRule.waitUntil(10_000) { server.requestCount > 0 }
            awaitStartAfter(startCount)
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithTag("player-subtitle-overlay")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(exitCount, progress.count { it.reason == ProgressReason.Exit })
        }
    }

    private fun pressProgressKey(key: Key) {
        composeRule.onNodeWithTag("player-progress")
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(key) }
        if (composeRule.mainClock.autoAdvance && key in listOf(Key.DirectionLeft, Key.DirectionRight)) {
            // Seek settling uses Compose time; Media3 callbacks use the Android main looper.
            composeRule.mainClock.advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS)
        }
    }

    private fun stop(owner: PlayerLifecycleOwner): Long {
        val exitCount = progress.count { it.reason == ProgressReason.Exit }
        composeRule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        composeRule.waitUntil(10_000) {
            progress.count { it.reason == ProgressReason.Exit } > exitCount
        }
        return progress.last { it.reason == ProgressReason.Exit }.positionMillis
    }

    private fun awaitStartAfter(count: Int): Progress {
        composeRule.waitForIdle()
        composeRule.waitUntil(10_000) {
            progress.count { it.reason == ProgressReason.Started } > count
        }
        // Buffering may emit more Started events while assertions wait for Compose to settle.
        return progress.filter { it.reason == ProgressReason.Started }[count]
    }

    private fun assertPositionNear(expected: Long, actual: Progress) {
        assertTrue(
            "Expected resume near $expected ms, got ${actual.positionMillis} ms",
            abs(actual.positionMillis - expected) < 500,
        )
    }

    private fun withPlayer(
        resumePositionMillis: Long = 5_000,
        awaitInitialStart: Boolean = true,
        factory: PlaybackControllerFactory? = null,
        block: (PlayerLifecycleOwner) -> Unit,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audio = File.createTempFile("player-lifecycle-", ".wav", context.cacheDir)
        val alternateAudio = File.createTempFile("player-quality-", ".wav", context.cacheDir)
        val visible = mutableStateOf(true)
        lateinit var owner: PlayerLifecycleOwner
        try {
            val audioBytes = silentAudio()
            audio.writeBytes(audioBytes)
            alternateAudio.writeBytes(audioBytes)
            subtitles = mutableStateOf(emptyList())
            request = mutableStateOf(
                PlaybackRequest.NetworkVideo(
                    requestId = "lifecycle-request",
                    serverId = "fixture-server",
                    title = "Lifecycle fixture",
                    source = NetworkPlaybackSource(
                        indexerId = 1,
                        resourceId = "fixture-resource",
                        title = "Lifecycle fixture",
                        url = Uri.fromFile(audio).toString(),
                        videoType = NetworkVideoType.Unknown,
                        danmakus = emptyList(),
                        definitions = listOf(
                            NetworkDefinition("1080p", Uri.fromFile(audio).toString()),
                            NetworkDefinition("720p", Uri.fromFile(alternateAudio).toString()),
                        ),
                        selectedDefinitionIndex = 0,
                    ),
                    resumePositionMillis = resumePositionMillis,
                ),
            )
            composeRule.runOnUiThread {
                owner = PlayerLifecycleOwner().apply {
                    lifecycle.currentState = Lifecycle.State.RESUMED
                }
            }
            composeRule.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
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
                                    request = request.value,
                                    subtitles = subtitles.value,
                                    danmakus = emptyList(),
                                    extraFailures = emptyMap(),
                                ),
                                controllerFactory = remember(playerContext) {
                                    factory ?: PlaybackControllerFactory(playerContext)
                                },
                                onProgress = { _, position, _, reason ->
                                    progress += Progress(position, reason)
                                },
                                onSelectDefinition = { index, position ->
                                    PlaybackRequestNavigator.selectDefinition(
                                        request.value,
                                        index,
                                        position,
                                    )?.let { request.value = it }
                                },
                                onPrevious = {},
                                onNext = {},
                                onSelectEpisode = {},
                                onRetryExtra = {},
                                onBack = {},
                            )
                        }
                    }
                }
            }
            if (awaitInitialStart) awaitStartAfter(0)
            block(owner)
        } finally {
            composeRule.runOnIdle { visible.value = false }
            composeRule.waitForIdle()
            audio.delete()
            alternateAudio.delete()
        }
    }

    private data class Progress(val positionMillis: Long, val reason: ProgressReason)
}

private class ObservedPlaybackControllerFactory(context: Context) : PlaybackControllerFactory(context) {
    lateinit var controller: PlaybackController
        private set

    override fun create(
        session: Session,
        request: PlaybackRequest,
        subtitles: List<SubtitleTrack>,
        probeDurationMillis: Long,
        resumeState: PlaybackResumeState?,
        onProgress: (PlaybackRequest, Long, Long, ProgressReason) -> Unit,
    ): PlaybackController = super.create(
        session = session,
        request = request,
        subtitles = subtitles,
        probeDurationMillis = probeDurationMillis,
        resumeState = resumeState,
        onProgress = onProgress,
    ).also { controller = it }
}

private class PlayerLifecycleOwner : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this)
}

private fun silentAudio(): ByteArray {
    // A local PCM stream exercises real Media3 seek/lifecycle behavior without a server or codecs.
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
