package org.kaloscope.tv.feature.player

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.model.NetworkDefinition
import org.kaloscope.tv.core.player.PlaybackSourceKind

class PlayerControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun controlsExposeOnlyAvailableEpisodeActionsAndQuality() {
        var nextClicks = 0
        var qualityClicks = 0

        composeRule.setContent {
            MaterialTheme {
                PlayerControls(
                    title = "Episode 1",
                    isPlaying = true,
                    positionMillis = 10_000,
                    durationMillis = 60_000,
                    subtitlesAvailable = false,
                    subtitlesEnabled = false,
                    danmakusAvailable = false,
                    danmakusEnabled = false,
                    extraErrors = emptySet(),
                    progressSaveFailed = false,
                    playbackMode = null,
                    sourceKind = PlaybackSourceKind.Network,
                    transcodeResolution = null,
                    fallbackInProgress = false,
                    hasPrevious = false,
                    hasNext = true,
                    definitions = listOf(NetworkDefinition("1080P", "https://example/1080.m3u8")),
                    switchingItem = false,
                    playFocus = remember { FocusRequester() },
                    definitionFocus = remember { FocusRequester() },
                    onPrevious = {},
                    onRewind = {},
                    onPlayPause = {},
                    onForward = {},
                    onNext = { nextClicks += 1 },
                    onToggleSubtitles = {},
                    onToggleDanmakus = {},
                    onOpenDefinitions = { qualityClicks += 1 },
                )
            }
        }

        composeRule.onNodeWithText("上一集").assertDoesNotExist()
        composeRule.onNodeWithText("下一集")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("清晰度")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(1, nextClicks)
            assertEquals(1, qualityClicks)
        }
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
}
