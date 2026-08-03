package org.kaloscope.tv.feature.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.model.NetworkDefinition
import org.kaloscope.tv.core.model.MediaChapter

class PlayerControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playPausePillKeepsItsLabelVisibleAndInvokesItsAction() {
        var playClicks = 0

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = { playClicks += 1 },
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithText("播放").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("播放")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithContentDescription("播放").assertIsFocused()
        composeRule.onNodeWithText("播放").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, playClicks)
        }
    }

    @Test
    fun playPauseContentAndLabelAreCenteredInsidePrimaryPill() {
        lateinit var density: Density

        composeRule.setContent {
            density = LocalDensity.current
            MaterialTheme {
                PlayerControls(
                    state = controlsState().copy(playWhenReady = true),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        val buttonBounds = composeRule.onNodeWithTag("player-play-pause")
            .fetchSemanticsNode()
            .boundsInRoot
        val contentBounds = composeRule.onNodeWithTag(
            testTag = "player-play-pause-content",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val labelBounds = composeRule.onNodeWithTag(
            testTag = "player-play-pause-label",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val tolerance = with(density) { 1.dp.toPx() }

        assertEquals(buttonBounds.center.x, contentBounds.center.x, tolerance)
        assertEquals(buttonBounds.center.y, contentBounds.center.y, tolerance)
        assertEquals(buttonBounds.center.y, labelBounds.center.y, tolerance)
    }

    @Test
    fun transportAndCollapsedAuxiliaryControlsMatchApprovedShapes() {
        lateinit var density: Density

        composeRule.setContent {
            density = LocalDensity.current
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        val playBounds = composeRule.onNodeWithTag("player-play-pause")
            .fetchSemanticsNode()
            .boundsInRoot
        val rewindBounds = composeRule.onNodeWithTag("player-rewind")
            .fetchSemanticsNode()
            .boundsInRoot
        val previousBounds = composeRule.onNodeWithTag("player-previous")
            .fetchSemanticsNode()
            .boundsInRoot
        val subtitleBounds = composeRule.onNodeWithTag("player-subtitles")
            .fetchSemanticsNode()
            .boundsInRoot
        val danmakuBounds = composeRule.onNodeWithTag("player-danmaku")
            .fetchSemanticsNode()
            .boundsInRoot
        val expectedPrimaryHeight = with(density) { 48.dp.toPx() }
        val tolerance = with(density) { 1.dp.toPx() }

        assertEquals(expectedPrimaryHeight, playBounds.height, tolerance)
        assertTrue("Play/pause should be a pill", playBounds.width > playBounds.height)
        assertTrue("Rewind should be a pill", rewindBounds.width > rewindBounds.height)
        assertEquals(previousBounds.height, previousBounds.width, tolerance)
        assertEquals(subtitleBounds.height, subtitleBounds.width, tolerance)
        assertEquals(danmakuBounds.height, danmakuBounds.width, tolerance)
        composeRule.onAllNodesWithText("10").assertCountEquals(2)
        composeRule.onNodeWithText("字幕").assertDoesNotExist()
    }

    @Test
    fun disabledSubtitlesStayIconOnlyAndCannotReceiveFocus() {
        lateinit var density: Density

        composeRule.setContent {
            density = LocalDensity.current
            MaterialTheme {
                PlayerControls(
                    state = controlsState(
                        subtitles = PlayerActionUiState(enabled = false),
                    ),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithText("字幕").assertDoesNotExist()
        val subtitle = composeRule.onNodeWithTag("player-subtitles")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        subtitle.performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsNotFocused()
        val bounds = subtitle.fetchSemanticsNode().boundsInRoot
        val expectedSize = with(density) { 42.dp.toPx() }
        val tolerance = with(density) { 1.dp.toPx() }

        assertEquals(expectedSize, bounds.width, tolerance)
        assertEquals(expectedSize, bounds.height, tolerance)
    }

    @Test
    fun auxiliaryControlsExpandOnlyTheFocusedLabelInDpadOrder() {
        lateinit var density: Density
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            density = LocalDensity.current
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        val collapsedSize = with(density) { 42.dp.toPx() }
        val expandedWidth = with(density) { 92.dp.toPx() }
        val collapsedTolerance = with(density) { 1.dp.toPx() }
        val focusedTolerance = with(density) { 5.dp.toPx() }

        fun assertCollapsed(tag: String) {
            val bounds = composeRule.onNodeWithTag(tag)
                .fetchSemanticsNode()
                .boundsInRoot
            assertEquals(collapsedSize, bounds.width, collapsedTolerance)
            assertEquals(collapsedSize, bounds.height, collapsedTolerance)
        }

        fun assertFocusedAndExpanded(controlTag: String, labelTag: String) {
            val bounds = composeRule.onNodeWithTag(controlTag)
                .assertIsFocused()
                .fetchSemanticsNode()
                .boundsInRoot
            assertEquals(expandedWidth, bounds.width, focusedTolerance)
            composeRule.onNodeWithTag(labelTag, useUnmergedTree = true)
                .assertIsDisplayed()
        }

        composeRule.mainClock.advanceTimeByFrame()
        assertCollapsed("player-subtitles")
        assertCollapsed("player-danmaku")
        assertCollapsed("player-speed")
        assertCollapsed("player-danmaku-settings")
        composeRule.onNodeWithText("字幕").assertDoesNotExist()
        composeRule.onNodeWithText("弹幕").assertDoesNotExist()
        composeRule.onNodeWithText("设置").assertDoesNotExist()

        composeRule.onNodeWithTag("player-subtitles")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(200)
        assertFocusedAndExpanded("player-subtitles", "player-subtitles-label")

        composeRule.onNodeWithTag("player-subtitles")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeBy(200)
        assertCollapsed("player-subtitles")
        composeRule.onNodeWithTag("player-subtitles-label", useUnmergedTree = true)
            .assertDoesNotExist()
        assertFocusedAndExpanded("player-danmaku", "player-danmaku-label")

        composeRule.onNodeWithTag("player-danmaku")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeBy(200)
        assertCollapsed("player-danmaku")
        assertFocusedAndExpanded("player-speed", "player-speed-label")

        composeRule.onNodeWithTag("player-speed")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeBy(200)
        assertCollapsed("player-speed")
        assertFocusedAndExpanded(
            "player-danmaku-settings",
            "player-danmaku-settings-label",
        )
    }

    @Test
    fun controlsShowSeriesAndEpisodeHierarchyWhenAvailable() {
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState().copy(
                        title = "星海纪行",
                        secondaryTitle = "第 4 集 · 穿越静默海",
                    ),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithText("星海纪行").assertIsDisplayed()
        composeRule.onNodeWithText("第 4 集 · 穿越静默海").assertIsDisplayed()
    }

    @Test
    fun qualityControlStaysAboveProgressAndOpensDefinitions() {
        var qualityClicks = 0

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = { qualityClicks += 1 },
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        val qualityBounds = composeRule.onNodeWithTag("player-quality")
            .assertIsDisplayed()
            .assertIsEnabled()
            .fetchSemanticsNode()
            .boundsInRoot
        val progressBounds = composeRule.onNodeWithTag("player-progress-track")
            .fetchSemanticsNode()
            .boundsInRoot
        val controlsBounds = composeRule.onNodeWithTag("player-control-row")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue("Quality should sit above progress", qualityBounds.bottom < progressBounds.top)
        assertTrue("The control row should sit below progress", controlsBounds.top > progressBounds.bottom)
        composeRule.onNodeWithTag("player-quality")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(1, qualityClicks)
        }
    }

    @Test
    fun playbackStatusChipsShareHeightAndVerticalCenter() {
        lateinit var density: Density

        composeRule.setContent {
            density = LocalDensity.current
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        val qualityBounds = composeRule.onNodeWithTag("player-quality")
            .fetchSemanticsNode()
            .boundsInRoot
        val speedBounds = composeRule.onNodeWithTag("player-playback-speed-status")
            .fetchSemanticsNode()
            .boundsInRoot
        val expectedHeight = with(density) { 36.dp.toPx() }
        val tolerance = with(density) { 1.dp.toPx() }

        assertEquals(expectedHeight, qualityBounds.height, tolerance)
        assertEquals(expectedHeight, speedBounds.height, tolerance)
        assertEquals(qualityBounds.center.y, speedBounds.center.y, tolerance)
    }

    @Test
    fun controlsKeepDisabledSlotsAndExposeActiveToggleState() {
        var nextClicks = 0
        var qualityClicks = 0

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(
                        previousEnabled = false,
                        nextEnabled = true,
                        subtitles = PlayerActionUiState(enabled = true, active = true),
                        danmakus = PlayerActionUiState(enabled = true, active = true),
                        quality = PlayerActionUiState(enabled = false),
                    ),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = { nextClicks += 1 },
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = { qualityClicks += 1 },
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("上一集")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("字幕 开")
            .assertIsDisplayed()
            .assertIsSelected()
        composeRule.onNodeWithContentDescription("弹幕 开")
            .assertIsDisplayed()
            .assertIsSelected()
        composeRule.onNodeWithContentDescription("弹幕设置")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeRule.onNodeWithContentDescription("下一集")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithContentDescription("清晰度")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeRule.runOnIdle {
            assertEquals(1, nextClicks)
            assertEquals(0, qualityClicks)
        }
    }

    @Test
    fun failedSupplementaryControlIsFocusableAndRetries() {
        var subtitleRetries = 0
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(
                        subtitles = PlayerActionUiState(enabled = true, error = true),
                    ),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                    onRetrySubtitles = { subtitleRetries += 1 },
                )
            }
        }

        composeRule.onNodeWithContentDescription("重试字幕")
            .assertIsEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "重试字幕",
                ),
            )
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(1, subtitleRetries)
        }
    }

    @Test
    fun progressFocusHidesActionRowAndMovesInformationGroupToBottom() {
        lateinit var density: Density
        composeRule.mainClock.autoAdvance = false
        var actionRowVisible by mutableStateOf(true)

        composeRule.setContent {
            density = LocalDensity.current
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    actionRowVisible = actionRowVisible,
                    onActionRowVisibilityChange = { actionRowVisible = it },
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithContentDescription("播放")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        val timelineBoundsBefore = composeRule.onNodeWithTag("player-progress-track")
            .fetchSemanticsNode()
            .boundsInRoot
        val titleBoundsBefore = composeRule.onNodeWithText("Episode 1")
            .fetchSemanticsNode()
            .boundsInRoot
        composeRule.onNodeWithContentDescription("播放")
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNodeWithTag("player-progress").assertIsFocused()
        composeRule.mainClock.advanceTimeBy(100)
        val timelineBoundsDuringMotion = composeRule.onNodeWithTag("player-progress-track")
            .fetchSemanticsNode()
            .boundsInRoot
        val titleBoundsDuringMotion = composeRule.onNodeWithText("Episode 1")
            .fetchSemanticsNode()
            .boundsInRoot
        composeRule.onAllNodesWithTag("player-control-row").assertCountEquals(1)

        composeRule.mainClock.advanceTimeBy(140)
        composeRule.onAllNodesWithTag("player-control-row").assertCountEquals(0)
        val timelineBoundsAfter = composeRule.onNodeWithTag("player-progress-track")
            .fetchSemanticsNode()
            .boundsInRoot
        val titleBoundsAfter = composeRule.onNodeWithText("Episode 1")
            .fetchSemanticsNode()
            .boundsInRoot
        val expectedShift = with(density) { 60.dp.toPx() }
        val tolerance = with(density) { 1.dp.toPx() }
        assertTrue(
            "Timeline should be moving after 100 ms",
            timelineBoundsDuringMotion.center.y > timelineBoundsBefore.center.y &&
                timelineBoundsDuringMotion.center.y < timelineBoundsAfter.center.y,
        )
        assertTrue(
            "Playback information should move continuously with the timeline",
            titleBoundsDuringMotion.center.y > titleBoundsBefore.center.y &&
                titleBoundsDuringMotion.center.y < titleBoundsAfter.center.y,
        )
        assertEquals(
            "Timeline should move into the hidden action section",
            timelineBoundsBefore.center.y + expectedShift,
            timelineBoundsAfter.center.y,
            tolerance,
        )
        assertEquals(
            "Playback information should move with the timeline",
            titleBoundsBefore.center.y + expectedShift,
            titleBoundsAfter.center.y,
            tolerance,
        )
    }

    @Test
    fun downFromProgressRevealsActionRowAndFocusesPlayPause() {
        var actionRowVisible by mutableStateOf(false)

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    actionRowVisible = actionRowVisible,
                    onActionRowVisibilityChange = { actionRowVisible = it },
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onAllNodesWithTag("player-control-row").assertCountEquals(0)
        composeRule.onNodeWithTag("player-progress")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()

        composeRule.onNodeWithTag("player-progress")
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("player-control-row").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("播放").assertIsFocused()
    }

    @Test
    fun centerOnProgressTogglesPlaybackWithoutRevealingActionRow() {
        var playPauseClicks = 0
        var actionRowVisible by mutableStateOf(false)

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    actionRowVisible = actionRowVisible,
                    onActionRowVisibilityChange = { actionRowVisible = it },
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = { playPauseClicks += 1 },
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onAllNodesWithTag("player-control-row").assertCountEquals(0)
        composeRule.onNodeWithTag("player-progress")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()

        composeRule.onNodeWithTag("player-progress")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("player-progress").assertIsFocused()
        composeRule.onAllNodesWithTag("player-control-row").assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(1, playPauseClicks)
        }
    }

    @Test
    fun progressSubmitsSeekAndReturnsFocusToPlay() {
        val seekTargets = mutableListOf<Long>()
        var actionRowVisible by mutableStateOf(true)
        var displayedPositionMillis by mutableStateOf(10_000L)

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState().copy(positionMillis = displayedPositionMillis),
                    actionRowVisible = actionRowVisible,
                    onActionRowVisibilityChange = { actionRowVisible = it },
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = { offsetMillis ->
                        displayedPositionMillis += offsetMillis
                    },
                    onSeekPreviewFinished = {
                        seekTargets += displayedPositionMillis
                    },
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("播放")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithTag("player-progress").assertIsFocused()
        composeRule.onNodeWithTag("player-progress").performKeyInput {
            keyDown(Key.DirectionRight)
            keyUp(Key.DirectionRight)
        }
        composeRule.runOnIdle {
            assertEquals(listOf(20_000L), seekTargets)
        }
        composeRule.onNodeWithTag("player-progress")
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithContentDescription("播放").assertIsFocused()
    }

    @Test
    fun progressKeepsSubmittedTargetVisibleUntilPlayerReportsIt() {
        val seekTargets = mutableListOf<Long>()
        var displayedPositionMillis by mutableStateOf(10_000L)

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState().copy(positionMillis = displayedPositionMillis),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = { offsetMillis ->
                        displayedPositionMillis += offsetMillis
                    },
                    onSeekPreviewFinished = {
                        seekTargets += displayedPositionMillis
                    },
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithTag("player-progress")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                keyDown(Key.DirectionRight)
            }
        composeRule.onAllNodesWithText("00:20").assertCountEquals(2)
        composeRule.runOnIdle {
            assertTrue(seekTargets.isEmpty())
        }

        composeRule.onNodeWithTag("player-progress")
            .performKeyInput { keyUp(Key.DirectionRight) }

        composeRule.onAllNodesWithText("00:20").assertCountEquals(2)
        composeRule.runOnIdle {
            assertEquals(listOf(20_000L), seekTargets)
        }
    }

    @Test
    fun focusedProgressThumbIsVerticallyCenteredOnTrack() {
        lateinit var density: Density

        composeRule.setContent {
            density = LocalDensity.current
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithTag("player-progress")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        val trackCenterY = composeRule.onNodeWithTag("player-progress-track")
            .fetchSemanticsNode()
            .boundsInRoot
            .center
            .y
        val thumbCenterY = composeRule.onNodeWithTag("player-progress-thumb")
            .fetchSemanticsNode()
            .boundsInRoot
            .center
            .y
        val tolerance = with(density) { 1.dp.toPx() }

        assertEquals(
            "Focused progress thumb and track should share a center line",
            trackCenterY,
            thumbCenterY,
            tolerance,
        )
    }

    @Test
    fun progressThumbHasAContrastingBorderAtRest() {
        lateinit var density: Density

        composeRule.setContent {
            density = LocalDensity.current
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        val thumb = composeRule.onNodeWithTag("player-progress-thumb-ring")
            .captureToImage()
            .asAndroidBitmap()
        val borderInset = with(density) { 1.dp.roundToPx() }
        val borderPixel = thumb.getPixel(borderInset, thumb.height / 2)
        val centerPixel = thumb.getPixel(thumb.width / 2, thumb.height / 2)
        val borderIsNearWhite = android.graphics.Color.red(borderPixel) > 220 &&
            android.graphics.Color.green(borderPixel) > 220 &&
            android.graphics.Color.blue(borderPixel) > 220
        val centerDiffersFromBorder =
            kotlin.math.abs(
                android.graphics.Color.red(borderPixel) - android.graphics.Color.red(centerPixel),
            ) + kotlin.math.abs(
                android.graphics.Color.green(borderPixel) - android.graphics.Color.green(centerPixel),
            ) + kotlin.math.abs(
                android.graphics.Color.blue(borderPixel) - android.graphics.Color.blue(centerPixel),
            ) > 100

        assertTrue("Progress thumb should have a near-white border", borderIsNearWhite)
        assertTrue("Progress thumb border should contrast with its center", centerDiffersFromBorder)
    }

    @Test
    fun progressUsesActiveColorWhilePlayingAndInactiveColorWhilePaused() {
        var playWhenReady by mutableStateOf(true)

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState().copy(playWhenReady = playWhenReady),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        val playingTrack = composeRule.onNodeWithTag("player-progress-played")
            .captureToImage()
            .asAndroidBitmap()
        assertEquals(
            0xFF9B8CFF.toInt(),
            playingTrack.getPixel(playingTrack.width / 2, playingTrack.height / 2),
        )

        composeRule.runOnIdle {
            playWhenReady = false
        }

        val pausedTrack = composeRule.onNodeWithTag("player-progress-played")
            .captureToImage()
            .asAndroidBitmap()
        assertEquals(
            0xFF747E94.toInt(),
            pausedTrack.getPixel(pausedTrack.width / 2, pausedTrack.height / 2),
        )
    }

    @Test
    fun progressThumbRemainsVisibleWhenPlayPauseHasFocus() {
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("播放")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("player-progress-thumb").assertIsDisplayed()
    }

    @Test
    fun downFromTransportOpensSettingsGroupAndUpReturnsToPlay() {
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("播放")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithContentDescription("字幕 关").assertIsFocused()
        composeRule.onNodeWithContentDescription("字幕 关")
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithContentDescription("播放").assertIsFocused()
    }

    @Test
    fun unknownDurationKeepsProgressDisabled() {
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState().copy(durationMillis = 0),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithTag("player-progress")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun infoPreviewKeepsProgressAtBottomAndOmitsTransportActions() {
        lateinit var density: Density

        composeRule.setContent {
            density = LocalDensity.current
            MaterialTheme {
                PlayerInfoPreview(state = controlsState())
            }
        }

        composeRule.onNodeWithTag("player-info-preview").assertIsDisplayed()
        composeRule.onNodeWithText("Episode 1").assertIsDisplayed()
        composeRule.onNodeWithText("Network").assertIsDisplayed()
        composeRule.onNodeWithText("1.0x").assertIsDisplayed()
        composeRule.onNodeWithText("00:10").assertIsDisplayed()
        composeRule.onNodeWithText("−00:50").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("播放").assertDoesNotExist()

        val layerBounds = composeRule.onNodeWithTag("player-info-preview")
            .fetchSemanticsNode()
            .boundsInRoot
        val trackBounds = composeRule.onNodeWithTag("player-preview-progress-track")
            .fetchSemanticsNode()
            .boundsInRoot
        val maximumBottomGap = with(density) { 16.dp.toPx() }
        assertTrue(
            "Preview progress should stay at the bottom edge",
            layerBounds.bottom - trackBounds.bottom <= maximumBottomGap,
        )
    }

    @Test
    fun rebufferingIndicatorWaitsForDelayAndHidesImmediately() {
        composeRule.mainClock.autoAdvance = false
        var rebuffering by mutableStateOf(true)

        composeRule.setContent {
            MaterialTheme {
                PlayerBufferingIndicator(isRebuffering = rebuffering)
            }
        }

        composeRule.mainClock.advanceTimeBy(499)
        composeRule.onNodeWithText("正在缓冲…").assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(1)
        composeRule.onNodeWithText("正在缓冲…").assertIsDisplayed()

        composeRule.runOnIdle {
            rebuffering = false
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("正在缓冲…").assertDoesNotExist()
    }

    @Test
    fun playbackToggleFeedbackRestartsWithLatestIconAndThenDisappears() {
        composeRule.mainClock.autoAdvance = false
        var event by mutableStateOf<PlayerPlaybackToggleEvent?>(
            PlayerPlaybackToggleEvent(
                id = 1,
                playWhenReady = false,
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                PlayerPlaybackToggleFeedback(
                    event = event,
                    onFinished = { finishedId ->
                        if (event?.id == finishedId) {
                            event = null
                        }
                    },
                )
            }
        }

        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("player-playback-toggle-pause").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(600)

        composeRule.runOnIdle {
            event = PlayerPlaybackToggleEvent(
                id = 2,
                playWhenReady = true,
            )
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("player-playback-toggle-play").assertIsDisplayed()
        composeRule.onNodeWithTag("player-playback-toggle-pause").assertDoesNotExist()

        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule.onNodeWithTag("player-playback-toggle-feedback").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(120)
        composeRule.onNodeWithTag("player-playback-toggle-feedback").assertDoesNotExist()
    }

    @Test
    fun definitionDrawerFocusesTheSelectedDefinition() {
        composeRule.setContent {
            MaterialTheme {
                PlayerDefinitionDrawer(
                    definitions = listOf(
                        NetworkDefinition("1080P", "https://example/1080.m3u8"),
                        NetworkDefinition("720P", "https://example/720.m3u8"),
                    ),
                    selectedIndex = 1,
                    onSelect = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("720P").assertIsFocused()
    }

    @Test
    fun subtitleAndSpeedButtonsOpenDrawersAndShowPersistentPlaybackStatus() {
        var subtitlesOpened = 0
        var speedOpened = 0
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState().copy(playbackSpeed = 1.25f),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = { subtitlesOpened += 1 },
                    onOpenSpeed = { speedOpened += 1 },
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithText("1.25x").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("1.25x")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithContentDescription("字幕 关")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(1, subtitlesOpened)
            assertEquals(1, speedOpened)
        }
    }

    @Test
    fun rightSkipsUnavailableNextAndSubtitlesToReachDanmaku() {
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(
                        nextEnabled = false,
                        subtitles = PlayerActionUiState(enabled = false),
                    ),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("+10 秒")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithContentDescription("弹幕 关").assertIsFocused()
    }

    @Test
    fun progressShowsChapterMarkersAndFocusedCurrentChapterWithoutOuterBorder() {
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState().copy(
                        positionMillis = 25_000,
                        chapters = listOf(
                            MediaChapter("opening", "片头", 0, 20_000),
                            MediaChapter("part-a", "第一章", 20_000, 60_000),
                        ),
                    ),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    subtitleFocus = remember { FocusRequester() },
                    speedFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onOpenSubtitles = {},
                    onOpenSpeed = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekPreviewBy = {},
                    onSeekPreviewFinished = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithTag("player-chapter-markers").assertIsDisplayed()
        composeRule.onNodeWithTag("player-current-chapter").assertDoesNotExist()
        composeRule.onNodeWithTag("player-progress")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("player-current-chapter").assertIsDisplayed()
        composeRule.onNodeWithText("第一章").assertIsDisplayed()
    }

    private fun controlsState(
        previousEnabled: Boolean = true,
        nextEnabled: Boolean = true,
        subtitles: PlayerActionUiState = PlayerActionUiState(enabled = true),
        danmakus: PlayerActionUiState = PlayerActionUiState(enabled = true),
        quality: PlayerActionUiState = PlayerActionUiState(enabled = true),
    ): PlayerControlsUiState =
        PlayerControlsUiState(
            title = "Episode 1",
            playWhenReady = false,
            positionMillis = 10_000,
            durationMillis = 60_000,
            playbackModeLabel = "Network",
            playbackSpeed = 1f,
            fallbackInProgress = false,
            progressSaveFailed = false,
            previousEnabled = previousEnabled,
            nextEnabled = nextEnabled,
            subtitles = subtitles,
            danmakus = danmakus,
            danmakuSettings = PlayerActionUiState(enabled = danmakus.enabled),
            quality = quality,
        )
}
