package org.kaloscope.tv.core.designsystem

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme

class KaloscopeColorSwatchTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun restingDarkSwatchUsesASubtleLightBorder() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PanelElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides OnBackground) {
                        KaloscopeColorSwatch(
                            color = Color(0xFF123456),
                            modifier = Modifier.testTag("dark-swatch"),
                        )
                    }
                }
            }
        }

        val (borderBrightness, centerBrightness) = swatchBrightness("dark-swatch")

        assertTrue(
            "Expected a subtle light border around the dark swatch; " +
                "border=$borderBrightness, center=$centerBrightness",
            borderBrightness in 125.0..160.0 && centerBrightness <= 70.0,
        )
    }

    @Test
    fun focusedSelectedSwatchUsesASubtleDarkBorder() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeButton(
                    onClick = {},
                    selected = true,
                    modifier = Modifier.testTag("selected-button"),
                ) {
                    KaloscopeColorSwatch(
                        color = Color.White,
                        modifier = Modifier.testTag("selected-swatch"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("selected-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(500)

        val (borderBrightness, centerBrightness) = swatchBrightness("selected-swatch")

        assertTrue(
            "Expected a subtle dark border around the selected swatch; " +
                "border=$borderBrightness, center=$centerBrightness",
            borderBrightness in 130.0..180.0 && centerBrightness >= 240.0,
        )
    }

    private fun swatchBrightness(testTag: String): Pair<Double, Double> {
        val bitmap = composeRule.onNodeWithTag(
            testTag = testTag,
            useUnmergedTree = true,
        )
            .captureToImage()
            .asAndroidBitmap()
        val edgeInset = (bitmap.width / 20).coerceAtLeast(1)
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val borderBrightness = listOf(
            bitmap.getPixel(centerX, edgeInset),
            bitmap.getPixel(bitmap.width - edgeInset - 1, centerY),
            bitmap.getPixel(centerX, bitmap.height - edgeInset - 1),
            bitmap.getPixel(edgeInset, centerY),
        ).map(::brightness).average()
        val centerBrightness = brightness(bitmap.getPixel(centerX, centerY))
        return borderBrightness to centerBrightness
    }

    private fun brightness(color: Int): Double =
        (AndroidColor.red(color) + AndroidColor.green(color) + AndroidColor.blue(color)) / 3.0
}
