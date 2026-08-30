package org.kaloscope.tv.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.R
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.test.captureToImage

class KaloscopeBrandTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun brandUsesCanonicalLogoPalette() {
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeBrand(
                    name = "Kaloscope",
                    caption = "TV EXPERIENCE",
                    modifier = Modifier
                        .background(Background)
                        .testTag("kaloscope-brand"),
                )
            }
        }

        val bitmap = composeRule.onNodeWithTag("kaloscope-brand")
            .captureToImage()
            .asAndroidBitmap()

        canonicalLogoColors.forEach { color ->
            assertTrue(
                "Canonical logo color ${color.toHexRgb()} was not rendered",
                bitmap.contains(color),
            )
        }
    }

    @Test
    fun compactLogoMatchesVisibleTextHeight() {
        assertLogoMatchesVisibleTextHeight(compact = true)
    }

    @Test
    fun standardLogoMatchesVisibleTextHeight() {
        assertLogoMatchesVisibleTextHeight(compact = false)
    }

    @Test
    fun launcherIconUsesOemCompatibleArtwork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val drawableRes = context.applicationInfo.icon
        assertEquals(
            "Launcher icons must use mipmap resources for OEM launcher compatibility",
            "mipmap",
            context.resources.getResourceTypeName(drawableRes),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue(
                "API 26+ launchers must receive an adaptive icon",
                context.getDrawable(drawableRes) is AdaptiveIconDrawable,
            )
        }

        val bitmap = renderDrawable(drawableRes, widthDp = 108, heightDp = 108)
        assertUsesCanonicalLogoPalette(bitmap)
        val logoBounds = bitmap.canonicalLogoBounds()
        val backgroundSamples =
            listOf(
                bitmap.getPixel(bitmap.width / 2, bitmap.height / 10),
                bitmap.getPixel(bitmap.width / 2, bitmap.height * 9 / 10),
                bitmap.getPixel(bitmap.width / 10, bitmap.height / 2),
                bitmap.getPixel(bitmap.width * 9 / 10, bitmap.height / 2),
            )
        assertTrue(
            "Launcher icon background must be white; sampled " +
                backgroundSamples.joinToString { "#%08X".format(it) } +
                ", logo bounds=$logoBounds",
            backgroundSamples.all { it == AndroidColor.WHITE },
        )

        assertTrue(
            "Launcher logo must be horizontally centered",
            abs(logoBounds.left + logoBounds.right - (bitmap.width - 1)) <= 2,
        )
        assertTrue(
            "Launcher logo must be vertically centered",
            abs(logoBounds.top + logoBounds.bottom - (bitmap.height - 1)) <= 2,
        )
    }

    @Test
    fun tvBannerUsesCanonicalLogoPalette() {
        assertDrawableUsesCanonicalLogoPalette(
            drawableRes = R.drawable.tv_banner,
            widthDp = 320,
            heightDp = 180,
        )
    }

    private fun assertLogoMatchesVisibleTextHeight(compact: Boolean) {
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeBrand(
                    name = "Kaloscope",
                    caption = "TV EXPERIENCE",
                    compact = compact,
                    modifier = Modifier
                        .background(Background)
                        .testTag("kaloscope-brand"),
                )
            }
        }

        val bitmap = composeRule.onNodeWithTag("kaloscope-brand")
            .captureToImage()
            .asAndroidBitmap()
        val logoHeight = bitmap.canonicalLogoHeight()
        val textHeight = bitmap.visibleTextHeight()

        assertTrue(
            "Logo height was $logoHeight px; visible text height was $textHeight px",
            abs(logoHeight - textHeight) <= 1,
        )
    }

    private fun assertDrawableUsesCanonicalLogoPalette(
        drawableRes: Int,
        widthDp: Int,
        heightDp: Int,
    ) {
        assertUsesCanonicalLogoPalette(renderDrawable(drawableRes, widthDp, heightDp))
    }

    private fun renderDrawable(
        drawableRes: Int,
        widthDp: Int,
        heightDp: Int,
    ): Bitmap {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val drawable = context.getDrawable(drawableRes)
            ?: error("Drawable $drawableRes was not found")
        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).roundToInt()
        val height = (heightDp * density).roundToInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun assertUsesCanonicalLogoPalette(bitmap: Bitmap) {
        canonicalLogoColors.forEach { color ->
            assertTrue(
                "Canonical logo color ${color.toHexRgb()} was not rendered",
                bitmap.contains(color),
            )
        }
    }
}

private val canonicalLogoColors = listOf(
    AndroidColor.rgb(0x51, 0x70, 0xFF),
    AndroidColor.rgb(0xCB, 0x6C, 0xE6),
    AndroidColor.rgb(0xFF, 0x91, 0x4D),
    AndroidColor.rgb(0xFF, 0xDE, 0x59),
    AndroidColor.rgb(0x00, 0xBF, 0x63),
)

private fun Bitmap.contains(expected: Int): Boolean =
    (0 until width).any { x ->
        (0 until height).any { y -> getPixel(x, y) == expected }
    }

private fun Bitmap.canonicalLogoHeight(): Int {
    val bounds = canonicalLogoBounds()
    return bounds.bottom - bounds.top + 1
}

private fun Bitmap.visibleTextHeight(): Int {
    val logoBounds = canonicalLogoBounds()
    val background = Background.toArgb()
    val rows = (0 until height).filter { y ->
        (logoBounds.right + 1 until width).any { x -> getPixel(x, y) != background }
    }
    return rows.last() - rows.first() + 1
}

private fun Bitmap.canonicalLogoBounds(): PixelBounds {
    val columns = (0 until width).filter { x ->
        (0 until height).any { y -> getPixel(x, y) in canonicalLogoColors }
    }
    val rows = (0 until height).filter { y ->
        (0 until width).any { x -> getPixel(x, y) in canonicalLogoColors }
    }
    return PixelBounds(
        left = columns.first(),
        top = rows.first(),
        right = columns.last(),
        bottom = rows.last(),
    )
}

private data class PixelBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

private fun Int.toHexRgb(): String = "#%06X".format(this and 0xFFFFFF)
