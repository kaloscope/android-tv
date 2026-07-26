package org.kaloscope.tv.test.golden

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

internal data class GoldenComparison(
    val passed: Boolean,
    val changedPixelRatio: Double,
    val diff: Bitmap,
)

internal fun compareGolden(
    expected: Bitmap,
    actual: Bitmap,
    channelTolerance: Int = 8,
    maxChangedPixelRatio: Double = 0.005,
): GoldenComparison {
    require(expected.width == actual.width && expected.height == actual.height) {
        "Golden dimensions differ: ${expected.width}x${expected.height} vs " +
            "${actual.width}x${actual.height}"
    }
    val diff = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
    var changed = 0L
    for (y in 0 until actual.height) {
        for (x in 0 until actual.width) {
            val expectedColor = expected.getPixel(x, y)
            val actualColor = actual.getPixel(x, y)
            val differs = abs(Color.red(expectedColor) - Color.red(actualColor)) > channelTolerance ||
                abs(Color.green(expectedColor) - Color.green(actualColor)) > channelTolerance ||
                abs(Color.blue(expectedColor) - Color.blue(actualColor)) > channelTolerance ||
                abs(Color.alpha(expectedColor) - Color.alpha(actualColor)) > channelTolerance
            if (differs) changed += 1
            diff.setPixel(x, y, if (differs) Color.MAGENTA else Color.TRANSPARENT)
        }
    }
    val ratio = changed.toDouble() / (actual.width.toLong() * actual.height).toDouble()
    return GoldenComparison(ratio <= maxChangedPixelRatio, ratio, diff)
}
