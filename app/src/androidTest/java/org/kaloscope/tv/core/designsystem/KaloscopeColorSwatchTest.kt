package org.kaloscope.tv.core.designsystem

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KaloscopeColorSwatchTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun darkColorSwatchHasALightCircularBorder() {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PanelElevated),
                contentAlignment = Alignment.Center,
            ) {
                KaloscopeColorSwatch(
                    color = Color(0xFF123456),
                    modifier = Modifier.testTag("dark-swatch"),
                )
            }
        }

        val bitmap = composeRule.onNodeWithTag("dark-swatch")
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

        assertTrue(
            "Expected a light border around the dark swatch; " +
                "border=$borderBrightness, center=$centerBrightness",
            borderBrightness >= 140.0 && centerBrightness <= 70.0,
        )
    }

    private fun brightness(color: Int): Double =
        (AndroidColor.red(color) + AndroidColor.green(color) + AndroidColor.blue(color)) / 3.0
}
