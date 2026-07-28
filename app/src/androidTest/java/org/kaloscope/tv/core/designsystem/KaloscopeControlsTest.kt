package org.kaloscope.tv.core.designsystem

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme

class KaloscopeControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedButtonExposesSelectionAndInvokesClick() {
        val clicks = mutableIntStateOf(0)
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeButton(
                    onClick = { clicks.intValue += 1 },
                    selected = true,
                    modifier = Modifier.testTag("selected-button"),
                ) {
                    Text("Selected")
                }
            }
        }

        composeRule.onNodeWithTag("selected-button")
            .assertIsSelected()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(1, clicks.intValue) }
    }

    @Test
    fun focusedIconButtonAcceptsDpadCenter() {
        val clicks = mutableIntStateOf(0)
        lateinit var focus: FocusRequester
        composeRule.setContent {
            KaloscopeTheme {
                focus = remember { FocusRequester() }
                KaloscopeIconButton(
                    onClick = { clicks.intValue += 1 },
                    modifier = Modifier
                        .focusRequester(focus)
                        .testTag("focused-icon"),
                ) {
                    Text("I")
                }
            }
        }

        composeRule.runOnIdle { focus.requestFocus() }
        composeRule.onNodeWithTag("focused-icon")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(1, clicks.intValue) }
    }

    @Test
    fun focusedRowHasNoSharpTopStroke() {
        lateinit var focus: FocusRequester
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                focus = remember { FocusRequester() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background),
                    contentAlignment = Alignment.Center,
                ) {
                    KaloscopeButton(
                        onClick = {},
                        size = KaloscopeControlSize.Row,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(width = 330.dp, height = 62.dp)
                            .focusRequester(focus)
                            .testTag("focused-row"),
                    ) {
                        Text("Focused")
                    }
                }
            }
        }

        composeRule.runOnIdle { focus.requestFocus() }
        composeRule.mainClock.advanceTimeBy(220)
        val bitmap = composeRule.onNodeWithTag("focused-row")
            .captureToImage()
            .asAndroidBitmap()
        val density = Resources.getSystem().displayMetrics.density
        val highlightY = density.toInt().coerceIn(0, bitmap.height - 1)
        val nearbyY = (density * 4f).toInt().coerceIn(0, bitmap.height - 1)
        val highlightBrightness = bitmap.averageBrightness(highlightY)
        val nearbyBrightness = bitmap.averageBrightness(nearbyY)
        val contrast = highlightBrightness - nearbyBrightness

        assertTrue(
            "Focused row has a sharp top stroke with contrast $contrast",
            contrast <= 8.0,
        )
    }

    @Test
    fun disabledControlIsSkippedAndCannotInvokeCallback() {
        val clicks = mutableIntStateOf(0)
        lateinit var firstFocus: FocusRequester
        composeRule.setContent {
            KaloscopeTheme {
                firstFocus = remember { FocusRequester() }
                Row {
                    KaloscopeButton(
                        onClick = {},
                        modifier = Modifier
                            .focusRequester(firstFocus)
                            .testTag("enabled-before"),
                    ) {
                        Text("Before")
                    }
                    KaloscopeButton(
                        onClick = { clicks.intValue += 1 },
                        enabled = false,
                        modifier = Modifier.testTag("disabled"),
                    ) {
                        Text("Disabled")
                    }
                    KaloscopeButton(
                        onClick = {},
                        modifier = Modifier.testTag("enabled-after"),
                    ) {
                        Text("After")
                    }
                }
            }
        }

        composeRule.runOnIdle { firstFocus.requestFocus() }
        composeRule.onNodeWithTag("enabled-before")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("disabled")
            .assertIsNotEnabled()
            .assertIsNotFocused()
        composeRule.onNodeWithTag("enabled-after").assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, clicks.intValue) }
    }
}

private fun Bitmap.averageBrightness(y: Int): Double {
    val startX = (width * 0.3f).toInt()
    val endX = (width * 0.7f).toInt()
    return (startX until endX)
        .map { x ->
            val pixel = getPixel(x, y)
            (
                AndroidColor.red(pixel) * 299 +
                    AndroidColor.green(pixel) * 587 +
                    AndroidColor.blue(pixel) * 114
                ) / 1_000.0
        }
        .average()
}
