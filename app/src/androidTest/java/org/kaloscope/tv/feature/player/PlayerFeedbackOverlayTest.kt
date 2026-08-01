package org.kaloscope.tv.feature.player

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.player.PlaybackFailure
import org.kaloscope.tv.core.player.PlaybackFeedback
import org.kaloscope.tv.core.player.PlaybackSourceKind

class PlayerFeedbackOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun preparingUsesCenteredIndicatorWithoutMessage() {
        composeRule.setContent {
            KaloscopeTheme {
                PlayerFeedbackOverlay(
                    feedback = PlaybackFeedback.Preparing,
                    failure = null,
                    sourceKind = PlaybackSourceKind.Direct,
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag("player-loading-indicator").assertExists()
        composeRule.onNodeWithText("正在准备播放").assertDoesNotExist()
    }

    @Test
    fun switchingUsesBlockingCenterMessage() {
        composeRule.setContent {
            KaloscopeTheme {
                PlayerFeedbackOverlay(
                    feedback = PlaybackFeedback.SwitchingItem,
                    failure = null,
                    sourceKind = PlaybackSourceKind.Direct,
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("正在切换剧集…").assertIsDisplayed()
    }

    @Test
    fun fallbackIsAPlainBanner() {
        composeRule.setContent {
            KaloscopeTheme {
                PlayerFeedbackOverlay(
                    feedback = PlaybackFeedback.FallingBack,
                    failure = null,
                    sourceKind = PlaybackSourceKind.HlsTranscode,
                    onRetry = {},
                )
            }
        }
        composeRule.onNodeWithText("直连失败，正在切换转码…").assertIsDisplayed()
    }

    @Test
    fun failureFocusesRetryAndHandlesRemoteClick() {
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                PlayerFeedbackOverlay(
                    feedback = PlaybackFeedback.Failed,
                    failure = PlaybackFailure.Decoder,
                    sourceKind = PlaybackSourceKind.Direct,
                    onRetry = { retries += 1 },
                )
            }
        }
        composeRule.onNodeWithText("重试")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }
}
