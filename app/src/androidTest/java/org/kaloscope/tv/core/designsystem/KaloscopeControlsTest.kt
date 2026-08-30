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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.test.captureToImage

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
    fun selectedAndFocusedButtonsUseApprovedNeutralSurfaces() {
        lateinit var focusedControl: FocusRequester
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                focusedControl = remember { FocusRequester() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Row {
                        KaloscopeButton(
                            onClick = {},
                            selected = true,
                            size = KaloscopeControlSize.Row,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(width = 330.dp, height = 62.dp)
                                .testTag("selected-surface"),
                        ) {
                            Text("Selected")
                        }
                        KaloscopeButton(
                            onClick = {},
                            size = KaloscopeControlSize.Row,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(width = 330.dp, height = 62.dp)
                                .focusRequester(focusedControl)
                                .testTag("focused-surface"),
                        ) {
                            Text("Focused")
                        }
                    }
                }
            }
        }

        composeRule.runOnIdle { focusedControl.requestFocus() }
        composeRule.mainClock.advanceTimeBy(500)
        val selected = composeRule.onNodeWithTag("selected-surface")
            .captureToImage()
            .asAndroidBitmap()
        val focused = composeRule.onNodeWithTag("focused-surface")
            .captureToImage()
            .asAndroidBitmap()
        val density = Resources.getSystem().displayMetrics.density
        val sampleX = (24 * density).toInt()

        assertColorNear(
            label = "selected surface",
            expected = AccentColor.Blue.accentPalette().controlSelected.toArgb(),
            actual = selected.getPixel(sampleX, selected.height / 2),
        )
        assertColorNear(
            label = "focused surface",
            expected = AndroidColor.rgb(0xE8, 0xED, 0xF4),
            actual = focused.getPixel(sampleX, focused.height / 2),
        )
        assertTrue(
            "Selected content did not inherit the light control color",
            selected.countPixelsNear(AndroidColor.rgb(0xF7, 0xF8, 0xFC)) >= 12,
        )
        assertTrue(
            "Focused content did not inherit the dark control color",
            focused.countPixelsNear(AndroidColor.rgb(0x10, 0x17, 0x25)) >= 12,
        )
    }

    @Test
    fun focusTransitionAnimatesContentWhileSurfaceIsAnimating() {
        lateinit var restingControl: FocusRequester
        lateinit var transitioningControl: FocusRequester
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                restingControl = remember { FocusRequester() }
                transitioningControl = remember { FocusRequester() }
                Row {
                    KaloscopeButton(
                        onClick = {},
                        modifier = Modifier
                            .size(width = 180.dp, height = 62.dp)
                            .focusRequester(restingControl),
                    ) {
                        Text("Resting")
                    }
                    KaloscopeButton(
                        onClick = {},
                        size = KaloscopeControlSize.Row,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(width = 330.dp, height = 62.dp)
                            .focusRequester(transitioningControl)
                            .testTag("transitioning-control"),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(LocalContentColor.current)
                                .testTag("transitioning-content"),
                        )
                    }
                }
            }
        }

        composeRule.runOnIdle { restingControl.requestFocus() }
        composeRule.mainClock.advanceTimeBy(KaloscopeMotion.FocusMillis.toLong() + 20)
        composeRule.runOnIdle { transitioningControl.requestFocus() }
        composeRule.mainClock.advanceTimeBy(KaloscopeMotion.FocusMillis.toLong() / 4)

        val control = composeRule.onNodeWithTag("transitioning-control")
            .captureToImage()
            .asAndroidBitmap()
        val content = composeRule.onNodeWithTag(
            testTag = "transitioning-content",
            useUnmergedTree = true,
        )
            .captureToImage()
            .asAndroidBitmap()
        val density = Resources.getSystem().displayMetrics.density

        assertRedChannelBetween(
            label = "surface transition",
            start = 0x18,
            end = 0xE8,
            actual = control.getPixel((24 * density).toInt(), control.height / 2),
        )
        assertRedChannelBetween(
            label = "content transition",
            start = 0xF7,
            end = 0x10,
            actual = content.getPixel(content.width / 2, content.height / 2),
        )
    }

    @Test
    fun iconFocusTransitionAnimatesContentWhileSurfaceIsAnimating() {
        lateinit var restingControl: FocusRequester
        lateinit var transitioningControl: FocusRequester
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                restingControl = remember { FocusRequester() }
                transitioningControl = remember { FocusRequester() }
                Row {
                    KaloscopeButton(
                        onClick = {},
                        modifier = Modifier
                            .size(width = 180.dp, height = 62.dp)
                            .focusRequester(restingControl),
                    ) {
                        Text("Resting")
                    }
                    KaloscopeIconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(62.dp)
                            .focusRequester(transitioningControl)
                            .testTag("transitioning-icon-control"),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(LocalContentColor.current)
                                .testTag("transitioning-icon-content"),
                        )
                    }
                }
            }
        }

        composeRule.runOnIdle { restingControl.requestFocus() }
        composeRule.mainClock.advanceTimeBy(KaloscopeMotion.FocusMillis.toLong() + 20)
        composeRule.runOnIdle { transitioningControl.requestFocus() }
        composeRule.mainClock.advanceTimeBy(KaloscopeMotion.FocusMillis.toLong() / 4)

        val control = composeRule.onNodeWithTag("transitioning-icon-control")
            .captureToImage()
            .asAndroidBitmap()
        val content = composeRule.onNodeWithTag(
            testTag = "transitioning-icon-content",
            useUnmergedTree = true,
        )
            .captureToImage()
            .asAndroidBitmap()
        val density = Resources.getSystem().displayMetrics.density

        assertRedChannelBetween(
            label = "icon surface transition",
            start = 0x18,
            end = 0xE8,
            actual = control.getPixel((10 * density).toInt(), control.height / 2),
        )
        assertRedChannelBetween(
            label = "icon content transition",
            start = 0xF7,
            end = 0x10,
            actual = content.getPixel(content.width / 2, content.height / 2),
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

private fun assertColorNear(
    label: String,
    expected: Int,
    actual: Int,
    tolerance: Int = 3,
) {
    val channelDifferences = listOf(
        kotlin.math.abs(AndroidColor.red(expected) - AndroidColor.red(actual)),
        kotlin.math.abs(AndroidColor.green(expected) - AndroidColor.green(actual)),
        kotlin.math.abs(AndroidColor.blue(expected) - AndroidColor.blue(actual)),
    )
    assertTrue(
        "$label expected ${Integer.toHexString(expected)} but was " +
            "${Integer.toHexString(actual)}",
        channelDifferences.all { it <= tolerance },
    )
}

private fun assertRedChannelBetween(
    label: String,
    start: Int,
    end: Int,
    actual: Int,
) {
    val actualRed = AndroidColor.red(actual)
    val range = (minOf(start, end) + 3)..(maxOf(start, end) - 3)
    assertTrue(
        "$label should be in progress between $start and $end, but red was $actualRed",
        actualRed in range,
    )
}

private fun Bitmap.countPixelsNear(
    expected: Int,
    tolerance: Int = 3,
): Int {
    var matches = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = getPixel(x, y)
            if (
                kotlin.math.abs(AndroidColor.red(expected) - AndroidColor.red(pixel)) <= tolerance &&
                kotlin.math.abs(AndroidColor.green(expected) - AndroidColor.green(pixel)) <= tolerance &&
                kotlin.math.abs(AndroidColor.blue(expected) - AndroidColor.blue(pixel)) <= tolerance
            ) {
                matches += 1
            }
        }
    }
    return matches
}
