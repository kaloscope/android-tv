package org.kaloscope.tv.core.designsystem

import android.graphics.Color as AndroidColor
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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

class KaloscopeSidePanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactPanelUsesSharedWidthAndEndAlignment() {
        setPanel(
            side = KaloscopeSidePanelSide.End,
            size = KaloscopeSidePanelSize.Compact,
        )

        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val panel = composeRule.onNodeWithTag("side-panel")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(dpToPx(400f), panel.width, dpToPx(1f))
        assertEquals(root.right, panel.right, 1f)
    }

    @Test
    fun standardPanelUsesSharedWidthAndStartAlignment() {
        setPanel(
            side = KaloscopeSidePanelSide.Start,
            size = KaloscopeSidePanelSize.Standard,
        )

        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val panel = composeRule.onNodeWithTag("side-panel")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(dpToPx(500f), panel.width, dpToPx(1f))
        assertEquals(root.left, panel.left, 1f)
    }

    @Test
    fun enabledDismissHandlerReceivesOneBackEvent() {
        var dismissCount = 0
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanel(
                    title = "Panel",
                    palette = testPalette(),
                    onDismiss = { dismissCount += 1 },
                    modifier = Modifier.testTag("side-panel"),
                ) {}
            }
        }

        pressBack()

        composeRule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    @Test
    fun disabledDismissHandlerLetsTheParentHandleBack() {
        var dismissCount = 0
        var parentBackCount = 0
        composeRule.setContent {
            KaloscopeTheme {
                BackHandler { parentBackCount += 1 }
                KaloscopeSidePanel(
                    title = "Panel",
                    palette = testPalette(),
                    onDismiss = { dismissCount += 1 },
                    dismissEnabled = false,
                    modifier = Modifier.testTag("side-panel"),
                ) {}
            }
        }

        pressBack()

        composeRule.runOnIdle {
            assertEquals(0, dismissCount)
            assertEquals(1, parentBackCount)
        }
    }

    @Test
    fun adjustmentArrowsUseFixedGeometryCenteredOnValue() {
        setAdjustmentRow()

        val decrease = composeRule.onNodeWithTag(
            testTag = "sample-decrease",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val increase = composeRule.onNodeWithTag(
            testTag = "sample-increase",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val value = composeRule.onNodeWithText("One", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        listOf(decrease, increase).forEach { arrow ->
            assertEquals(dpToPx(14f), arrow.width, 0.5f)
            assertEquals(dpToPx(18f), arrow.height, 0.5f)
            assertEquals(value.center.y, arrow.center.y, dpToPx(1f))
        }
    }

    @Test
    fun unavailableAdjustmentArrowHasClearlyLowerContrast() {
        setAdjustmentRow()

        assertAdjustmentArrowContrast()

        composeRule.onNodeWithTag("sample-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(500)

        assertAdjustmentArrowContrast()
    }

    @Test
    fun adjustmentRowDisablesBoundaryArrowAndConsumesInvalidDirection() {
        var decreaseCount = 0
        var increaseCount = 0
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanelAdjustmentRow(
                    title = "Setting",
                    value = "One",
                    canDecrease = false,
                    canIncrease = true,
                    onDecrease = { decreaseCount += 1 },
                    onIncrease = { increaseCount += 1 },
                    modifier = Modifier.testTag("sample-row"),
                    adjustmentTestTagPrefix = "sample",
                )
            }
        }

        composeRule.onNodeWithTag("sample-decrease", useUnmergedTree = true)
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("sample-increase", useUnmergedTree = true)
            .assertIsEnabled()

        composeRule.onNodeWithTag("sample-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
            }

        composeRule.runOnIdle {
            assertEquals(0, decreaseCount)
            assertEquals(1, increaseCount)
        }
    }

    @Test
    fun adjustmentRowIgnoresCenter() {
        var decreaseCount = 0
        var increaseCount = 0
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanelAdjustmentRow(
                    title = "Setting",
                    value = "One",
                    canDecrease = true,
                    canIncrease = true,
                    onDecrease = { decreaseCount += 1 },
                    onIncrease = { increaseCount += 1 },
                    modifier = Modifier.testTag("sample-row"),
                )
            }
        }

        composeRule.onNodeWithTag("sample-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(0, decreaseCount)
            assertEquals(0, increaseCount)
        }
    }

    @Test
    fun choiceRowDisplaysModalIndicatorWithoutAdjustmentArrows() {
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanelChoiceRow(
                    title = "Setting",
                    value = "One",
                    onClick = {},
                    modifier = Modifier.testTag("choice-row"),
                )
            }
        }

        composeRule.onNodeWithTag(
            testTag = "choice-setting-indicator",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag("sample-decrease", useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag("sample-increase", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun choiceRowConsumesHorizontalDirectionsWithoutOpening() {
        var openCount = 0
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanelChoiceRow(
                    title = "Setting",
                    value = "One",
                    onClick = { openCount += 1 },
                    modifier = Modifier.testTag("choice-row"),
                )
            }
        }

        composeRule.onNodeWithTag("choice-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
            }
            .assertIsFocused()

        composeRule.runOnIdle {
            assertEquals(0, openCount)
        }
    }

    @Test
    fun choiceRowOpensOnCenter() {
        var openCount = 0
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanelChoiceRow(
                    title = "Setting",
                    value = "One",
                    onClick = { openCount += 1 },
                    modifier = Modifier.testTag("choice-row"),
                )
            }
        }

        composeRule.onNodeWithTag("choice-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, openCount)
        }
    }

    @Test
    fun adjustmentRowDoesNotInvokeIncreaseAtUpperBoundary() {
        var increaseCount = 0
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanelAdjustmentRow(
                    title = "Setting",
                    value = "Last",
                    canDecrease = true,
                    canIncrease = false,
                    onDecrease = {},
                    onIncrease = { increaseCount += 1 },
                    modifier = Modifier.testTag("sample-row"),
                    adjustmentTestTagPrefix = "sample",
                )
            }
        }

        composeRule.onNodeWithTag("sample-increase", useUnmergedTree = true)
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("sample-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.Enter)
            }

        composeRule.runOnIdle {
            assertEquals(0, increaseCount)
        }
    }

    @Test
    fun panelRowsTrapFocusAtEveryOuterEdge() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(Modifier.fillMaxSize()) {
                    KaloscopeButton(
                        onClick = {},
                        modifier = Modifier.testTag("outside-control"),
                    ) {}
                    KaloscopeSidePanel(
                        title = "Panel",
                        palette = testPalette(),
                        onDismiss = {},
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            KaloscopeSidePanelSelectionRow(
                                title = "First",
                                onClick = {},
                                modifier = Modifier.testTag("first-row"),
                            )
                            KaloscopeSidePanelSelectionRow(
                                title = "Last",
                                onClick = {},
                                modifier = Modifier.testTag("last-row"),
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("first-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionUp)
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
            }
            .assertIsFocused()
        composeRule.onNodeWithTag("last-row")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
            .assertIsFocused()
    }

    @Test
    fun sessionHintUsesSharedSizeAndVerticalAlignment() {
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanelSessionHint(
                    text = "Session only",
                    color = Color.Gray,
                    iconTestTag = "hint-icon",
                    textTestTag = "hint-text",
                )
            }
        }

        val icon = composeRule.onNodeWithTag("hint-icon", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val hintText = composeRule.onNodeWithTag("hint-text", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertEquals(dpToPx(14f), icon.width, 0.5f)
        assertEquals(hintText.center.y, icon.center.y, dpToPx(1f))
    }

    private fun setPanel(
        side: KaloscopeSidePanelSide,
        size: KaloscopeSidePanelSize,
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanel(
                    title = "Panel",
                    palette = testPalette(),
                    onDismiss = {},
                    side = side,
                    size = size,
                    modifier = Modifier.testTag("side-panel"),
                ) {}
            }
        }
    }

    private fun testPalette() = KaloscopeSidePanelPalette(
        panelColor = Color(0xFF121212),
        textColor = Color.White,
        mutedColor = Color.Gray,
    )

    private fun setAdjustmentRow() {
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeSidePanelAdjustmentRow(
                    title = "Setting",
                    value = "One",
                    canDecrease = false,
                    canIncrease = true,
                    onDecrease = {},
                    onIncrease = {},
                    modifier = Modifier.testTag("sample-row"),
                    adjustmentTestTagPrefix = "sample",
                )
            }
        }
    }

    private fun assertAdjustmentArrowContrast() {
        val unavailable = composeRule.onNodeWithTag(
            testTag = "sample-decrease",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()
        val available = composeRule.onNodeWithTag(
            testTag = "sample-increase",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()
        val unavailableContrast = maxLuminanceContrast(unavailable)
        val availableContrast = maxLuminanceContrast(available)

        assertTrue(
            "Unavailable contrast $unavailableContrast must be at most 30% of " +
                "available contrast $availableContrast",
            unavailableContrast <= availableContrast * 0.3f,
        )
    }

    private fun maxLuminanceContrast(bitmap: android.graphics.Bitmap): Float {
        val background = listOf(
            bitmap.getPixel(0, 0),
            bitmap.getPixel(bitmap.width - 1, 0),
            bitmap.getPixel(0, bitmap.height - 1),
            bitmap.getPixel(bitmap.width - 1, bitmap.height - 1),
        ).map(::luminance).average().toFloat()
        var maxContrast = 0f
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                maxContrast = maxOf(
                    maxContrast,
                    kotlin.math.abs(luminance(bitmap.getPixel(x, y)) - background),
                )
            }
        }
        return maxContrast
    }

    private fun luminance(color: Int): Float =
        AndroidColor.red(color) * 0.2126f +
            AndroidColor.green(color) * 0.7152f +
            AndroidColor.blue(color) * 0.0722f

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()
    }

    private fun dpToPx(value: Float): Float =
        value * InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

}
