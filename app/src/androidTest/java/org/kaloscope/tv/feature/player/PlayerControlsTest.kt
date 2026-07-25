package org.kaloscope.tv.feature.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.model.NetworkDefinition

class PlayerControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun focusedIconShowsItsLabelAndInvokesItsAction() {
        var playClicks = 0

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = { playClicks += 1 },
                    onForward = {},
                    onNext = {},
                    onToggleSubtitles = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekTo = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithText("播放").assertDoesNotExist()
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
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = { nextClicks += 1 },
                    onToggleSubtitles = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = { qualityClicks += 1 },
                    onSeekTo = {},
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
    fun failedSupplementaryControlExposesErrorSemantics() {
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(
                        subtitles = PlayerActionUiState(enabled = false, error = true),
                    ),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onToggleSubtitles = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekTo = {},
                    onHideControls = {},
                    onInteraction = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("字幕不可用")
            .assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "字幕不可用",
                ),
            )
    }

    @Test
    fun progressSubmitsSeekAndReturnsFocusToPlay() {
        val seekTargets = mutableListOf<Long>()

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState(),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onToggleSubtitles = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekTo = seekTargets::add,
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
    fun unknownDurationKeepsProgressDisabled() {
        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    state = controlsState().copy(durationMillis = 0),
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    danmakuSettingsFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = {},
                    onToggleSubtitles = {},
                    onToggleDanmakus = {},
                    onOpenDanmakuSettings = {},
                    onOpenDefinitions = {},
                    onSeekTo = {},
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

    private fun controlsState(
        previousEnabled: Boolean = true,
        nextEnabled: Boolean = true,
        subtitles: PlayerActionUiState = PlayerActionUiState(enabled = true),
        danmakus: PlayerActionUiState = PlayerActionUiState(enabled = true),
        quality: PlayerActionUiState = PlayerActionUiState(enabled = true),
    ): PlayerControlsUiState =
        PlayerControlsUiState(
            title = "Episode 1",
            isPlaying = false,
            positionMillis = 10_000,
            durationMillis = 60_000,
            playbackModeLabel = "Network",
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
