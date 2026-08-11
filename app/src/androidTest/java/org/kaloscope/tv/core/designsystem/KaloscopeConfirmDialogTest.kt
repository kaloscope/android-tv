package org.kaloscope.tv.core.designsystem

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme

class KaloscopeConfirmDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initiallyFocusesCancelAndTrapsDpadFocus() {
        composeRule.setContent {
            KaloscopeTheme {
                TestDialog()
            }
        }

        val cancel = composeRule.onNodeWithTag("confirm-dialog-cancel")
        val confirm = composeRule.onNodeWithTag("confirm-dialog-confirm")

        cancel.assertIsFocused()
        listOf(Key.DirectionLeft, Key.DirectionUp, Key.DirectionDown).forEach { key ->
            cancel.performKeyInput { pressKey(key) }
            cancel.assertIsFocused()
        }

        cancel.performKeyInput { pressKey(Key.DirectionRight) }
        confirm.assertIsFocused()
        listOf(Key.DirectionRight, Key.DirectionUp, Key.DirectionDown).forEach { key ->
            confirm.performKeyInput { pressKey(key) }
            confirm.assertIsFocused()
        }

        confirm.performKeyInput { pressKey(Key.DirectionLeft) }
        cancel.assertIsFocused()
    }

    @Test
    fun backDismissesIdleDialog() {
        var dismissals = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestDialog(onDismiss = { dismissals += 1 })
            }
        }

        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun busyDialogKeepsConfirmFocusAndIgnoresActions() {
        var dismissals = 0
        var confirmations = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestDialog(
                    busy = true,
                    onDismiss = { dismissals += 1 },
                    onConfirm = { confirmations += 1 },
                )
            }
        }

        composeRule.onNodeWithTag("confirm-dialog-confirm")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("confirm-dialog-confirm").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(0, confirmations)
            assertEquals(0, dismissals)
        }
    }

    @Test
    fun busyIndicatorAppearsAfterDelayWithoutMovingConfirmLabel() {
        composeRule.mainClock.autoAdvance = false
        val busy = mutableStateOf(true)
        composeRule.setContent {
            KaloscopeTheme {
                TestDialog(busy = busy.value)
            }
        }
        val label = composeRule.onNodeWithText("删除")
        val labelBounds = label.getUnclippedBoundsInRoot()
        val confirm = composeRule.onNodeWithTag("confirm-dialog-confirm")
        val confirmBounds = confirm.getUnclippedBoundsInRoot()

        composeRule.mainClock.advanceTimeBy(499)
        composeRule.onNodeWithTag(
            testTag = "confirm-dialog-busy-indicator",
            useUnmergedTree = true,
        ).assertDoesNotExist()

        composeRule.mainClock.advanceTimeBy(1)
        val indicatorBounds = composeRule.onNodeWithTag(
            testTag = "confirm-dialog-busy-indicator",
            useUnmergedTree = true,
        ).assertExists().getUnclippedBoundsInRoot()
        assertEquals(18.dp, indicatorBounds.right - indicatorBounds.left)
        assertEquals(18.dp, indicatorBounds.bottom - indicatorBounds.top)
        assertEquals(labelBounds, label.getUnclippedBoundsInRoot())
        assertEquals(confirmBounds, confirm.getUnclippedBoundsInRoot())
        assertTrue(
            "Busy indicator $indicatorBounds escaped confirm button $confirmBounds",
            indicatorBounds.left >= confirmBounds.left &&
                indicatorBounds.top >= confirmBounds.top &&
                indicatorBounds.right <= confirmBounds.right &&
                indicatorBounds.bottom <= confirmBounds.bottom,
        )
    }

    @Test
    fun busyIndicatorDoesNotAppearWhenBusyEndsBeforeDelay() {
        composeRule.mainClock.autoAdvance = false
        val busy = mutableStateOf(true)
        composeRule.setContent {
            KaloscopeTheme {
                TestDialog(busy = busy.value)
            }
        }

        composeRule.mainClock.advanceTimeBy(499)
        composeRule.runOnIdle { busy.value = false }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(500)

        composeRule.onNodeWithTag(
            testTag = "confirm-dialog-busy-indicator",
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun showsProvidedErrorMessage() {
        composeRule.setContent {
            KaloscopeTheme {
                TestDialog(errorMessage = "无法删除服务器，请重试。")
            }
        }

        composeRule.onNodeWithText("无法删除服务器，请重试。").assertExists()
    }
}

@androidx.compose.runtime.Composable
private fun TestDialog(
    confirmLabel: String = "删除",
    busy: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    KaloscopeConfirmDialog(
        title = "删除服务器？",
        message = "确认删除测试服务器",
        cancelLabel = "取消",
        confirmLabel = confirmLabel,
        confirmTone = KaloscopeControlTone.Danger,
        busy = busy,
        errorMessage = errorMessage,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
