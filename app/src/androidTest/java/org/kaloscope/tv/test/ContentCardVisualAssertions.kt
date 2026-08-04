package org.kaloscope.tv.test

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import org.junit.Assert.assertTrue

internal fun assertFocusedContentCardSurface(
    label: String,
    bitmap: Bitmap,
    sampleX: Int,
    sampleY: Int,
) {
    val x = sampleX.coerceIn(0, bitmap.width - 1)
    val y = sampleY.coerceIn(0, bitmap.height - 1)
    val actual = bitmap.getPixel(x, y)
    val expected = Color.rgb(0x25, 0x32, 0x4A)
    val distance = abs(Color.red(expected) - Color.red(actual)) +
        abs(Color.green(expected) - Color.green(actual)) +
        abs(Color.blue(expected) - Color.blue(actual))
    assertTrue(
        "$label focused surface expected #25324A but was " +
            "#${String.format("%06X", actual and 0xFFFFFF)}",
        distance <= 9,
    )
}

internal fun assertFocusedContentCardScale(
    label: String,
    resting: Bitmap,
    focused: Bitmap,
) {
    val target = Color.rgb(0x25, 0x33, 0x4D)
    val restingBounds = resting.findColorBounds(target)
    val focusedBounds = focused.findColorBounds(target)
    val widthScale = focusedBounds.width.toFloat() / restingBounds.width
    val heightScale = focusedBounds.height.toFloat() / restingBounds.height
    // The short cover height rounds a 3% transform to only two pixels; width
    // remains large enough to distinguish the former 1.04 scale from 1.03.
    assertTrue(
        "$label focused scale expected 1.03 but was " +
            "width=$widthScale height=$heightScale",
        abs(widthScale - 1.03f) <= 0.004f,
    )
}

internal fun assertFocusedContentCardCornerRadius(
    label: String,
    bitmap: Bitmap,
    density: Float,
) {
    val probeY = density.toInt().coerceIn(0, bitmap.height - 1)
    val expectedFirstBorderPixel =
        (6f * density).toInt()..(13f * density).toInt()
    val searchEnd = (20f * density).toInt().coerceAtMost(bitmap.width / 2)
    val firstBorderPixel = (0..searchEnd).firstOrNull { x ->
        bitmap.getPixel(x, probeY).isNearWhite()
    }

    assertTrue(
        "$label expected a white border near the top-left corner",
        firstBorderPixel != null,
    )
    assertTrue(
        "$label expected a 15dp corner radius but the first white border pixel was " +
            "$firstBorderPixel at density $density",
        firstBorderPixel in expectedFirstBorderPixel,
    )
}

private data class PixelBounds(
    val width: Int,
    val height: Int,
)

private fun Bitmap.findColorBounds(target: Int): PixelBounds {
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (getPixel(x, y) == target) {
                minX = minOf(minX, x)
                minY = minOf(minY, y)
                maxX = maxOf(maxX, x)
                maxY = maxOf(maxY, y)
            }
        }
    }
    assertTrue("Expected color #25334D in captured screen", maxX >= minX && maxY >= minY)
    return PixelBounds(
        width = maxX - minX + 1,
        height = maxY - minY + 1,
    )
}

private fun Int.isNearWhite(): Boolean {
    val channels = listOf(Color.red(this), Color.green(this), Color.blue(this))
    // Focused-disabled content alpha composites the white stroke into a neutral gray.
    return Color.alpha(this) >= 220 &&
        channels.min() >= 100 &&
        channels.max() - channels.min() <= 24
}
