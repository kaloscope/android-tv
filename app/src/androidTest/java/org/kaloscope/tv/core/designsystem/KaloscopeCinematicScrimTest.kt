package org.kaloscope.tv.core.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.test.captureToImage

class KaloscopeCinematicScrimTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeProtectsTextAndRevealsArtworkOnTheRight() {
        val bitmap = captureScrim(protectFullWidth = false)
        val textBackground = bitmap.colorAt(0.35f, 0.48f)
        val artwork = bitmap.colorAt(0.95f, 0.48f)

        assertTrue(artwork.luminance() > textBackground.luminance() + 0.1f)
        assertTrue(contrast(OnBackground, textBackground) >= 4.5f)
        for (accent in AccentColor.entries) {
            assertTrue(
                "$accent remains readable over a bright backdrop",
                contrast(accent.accentPalette().primary, textBackground) >= 4.5f,
            )
        }
    }

    @Test
    fun detailProtectsWideTextOverABrightBackdrop() {
        val bitmap = captureScrim(protectFullWidth = true)

        for (x in listOf(0.35f, 0.65f, 0.95f)) {
            val background = bitmap.colorAt(x, 0.48f)
            assertTrue(contrast(OnBackground, background) >= 4.5f)
            assertTrue(contrast(Muted, background) >= 4.5f)
        }
    }

    private fun captureScrim(protectFullWidth: Boolean): Bitmap {
        composeRule.setContent {
            KaloscopeTheme {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    KaloscopeCinematicScrim(protectFullWidth = protectFullWidth)
                }
            }
        }
        composeRule.waitForIdle()
        return composeRule.onRoot().captureToImage().asAndroidBitmap()
    }

    private fun Bitmap.colorAt(x: Float, y: Float): Color =
        Color(getPixel((width * x).toInt(), (height * y).toInt()))

    private fun contrast(first: Color, second: Color): Float =
        (max(first.luminance(), second.luminance()) + 0.05f) /
            (min(first.luminance(), second.luminance()) + 0.05f)
}
