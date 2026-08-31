package org.kaloscope.tv.feature.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme

class PlayerExitConfirmationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstBackShowsConfirmationAndSecondBackExits() {
        var exits = 0
        composeRule.setContent {
            KaloscopeTheme {
                PlayerExitConfirmation(
                    enabled = true,
                    controlsVisible = false,
                    onHideControls = {},
                    onExit = { exits += 1 },
                )
            }
        }

        pressBack()

        composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()
        composeRule.onNodeWithText("再按一次返回键退出播放").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, exits) }

        pressBack()

        composeRule.onNodeWithTag("player-exit-confirmation").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun visibleControlsConsumeBackWithoutArmingExit() {
        var hideControlsCalls = 0
        var exits = 0
        composeRule.setContent {
            KaloscopeTheme {
                PlayerExitConfirmation(
                    enabled = true,
                    controlsVisible = true,
                    onHideControls = { hideControlsCalls += 1 },
                    onExit = { exits += 1 },
                )
            }
        }

        pressBack()

        composeRule.onNodeWithTag("player-exit-confirmation").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, hideControlsCalls)
            assertEquals(0, exits)
        }
    }

    @Test
    fun confirmationExpiresBeforeAnotherBackCanExit() {
        composeRule.mainClock.autoAdvance = false
        var exits = 0
        composeRule.setContent {
            KaloscopeTheme {
                PlayerExitConfirmation(
                    enabled = true,
                    controlsVisible = false,
                    onHideControls = {},
                    onExit = { exits += 1 },
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()

        pressBack()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(2_001)
        composeRule.onNodeWithTag("player-exit-confirmation").assertDoesNotExist()

        pressBack()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, exits) }
    }

    @Test
    fun anotherRemoteInteractionCancelsConfirmation() {
        val cancellationSignal = mutableLongStateOf(0)
        var exits = 0
        composeRule.setContent {
            KaloscopeTheme {
                PlayerExitConfirmation(
                    enabled = true,
                    controlsVisible = false,
                    cancellationSignal = cancellationSignal.longValue,
                    onHideControls = {},
                    onExit = { exits += 1 },
                )
            }
        }

        pressBack()
        composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()

        composeRule.runOnIdle { cancellationSignal.longValue += 1 }
        composeRule.onNodeWithTag("player-exit-confirmation").assertDoesNotExist()

        pressBack()
        composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, exits) }
    }

    @Test
    fun playbackChangeClearsPendingConfirmation() {
        val playbackKey = mutableStateOf("episode-1")
        var exits = 0
        composeRule.setContent {
            KaloscopeTheme {
                PlayerExitConfirmation(
                    enabled = true,
                    controlsVisible = false,
                    resetKey = playbackKey.value,
                    onHideControls = {},
                    onExit = { exits += 1 },
                )
            }
        }

        pressBack()
        composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()

        composeRule.runOnIdle { playbackKey.value = "episode-2" }
        composeRule.onNodeWithTag("player-exit-confirmation").assertDoesNotExist()

        pressBack()
        composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, exits) }
    }

    @Test
    fun disablingHandlerClearsPendingConfirmation() {
        val enabled = mutableStateOf(true)
        composeRule.setContent {
            KaloscopeTheme {
                PlayerExitConfirmation(
                    enabled = enabled.value,
                    controlsVisible = false,
                    onHideControls = {},
                    onExit = {},
                )
            }
        }

        pressBack()
        composeRule.onNodeWithTag("player-exit-confirmation").assertIsDisplayed()

        composeRule.runOnIdle { enabled.value = false }

        composeRule.onNodeWithTag("player-exit-confirmation").assertDoesNotExist()
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
    }
}
