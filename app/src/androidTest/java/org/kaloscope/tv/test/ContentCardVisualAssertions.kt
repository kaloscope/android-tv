package org.kaloscope.tv.test

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
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
    expectedScale: Float,
    searchBounds: Rect? = null,
    searchPadding: Int = 0,
) {
    val target = Color.rgb(0x25, 0x33, 0x4D)
    val restingBounds = resting.findColorBounds(target, searchBounds, searchPadding)
    val focusedBounds = focused.findColorBounds(target, searchBounds, searchPadding)
    val widthScale = focusedBounds.width.toFloat() / restingBounds.width
    val heightScale = focusedBounds.height.toFloat() / restingBounds.height
    // Width is less sensitive than height to pixel rounding on short cards.
    assertTrue(
        "$label focused scale expected $expectedScale but was " +
            "width=$widthScale height=$heightScale",
        abs(widthScale - expectedScale) <= 0.004f,
    )
}

internal fun assertFocusedContentCardTopClearance(
    label: String,
    cardBounds: Rect,
    viewportBounds: Rect,
    density: Float,
    focusScale: Float,
) {
    val actualClearance = cardBounds.top - viewportBounds.top
    val scaledOverhang = cardBounds.height * (focusScale - 1f) / 2f
    val minimumClearance = scaledOverhang + density
    assertTrue(
        "$label must reserve the focused scale overhang plus 1dp above the first row, " +
            "but clearance was ${actualClearance}px and required ${minimumClearance}px",
        actualClearance >= minimumClearance,
    )
}

internal fun assertGridRowReservesFocusedScaleHeight(
    label: String,
    firstRowCardBounds: Rect,
    nextRowCardBounds: Rect,
    rowSpacingPixels: Float,
) {
    val expectedRowStride = firstRowCardBounds.height + rowSpacingPixels
    val actualRowStride = nextRowCardBounds.top - firstRowCardBounds.top
    assertTrue(
        "$label focus bounds must expose the full reserved row height; " +
            "expected $expectedRowStride px but was $actualRowStride px",
        abs(actualRowStride - expectedRowStride) <= 1f,
    )
}

internal fun assertFocusedContentCardBottomInsideViewport(
    label: String,
    bitmap: Bitmap,
    cardBounds: Rect,
    viewportBounds: Rect,
    density: Float,
) {
    val focusedSurface = Color.rgb(0x25, 0x32, 0x4A)
    val centerX = cardBounds.center.x.roundToInt().coerceIn(0, bitmap.width - 1)
    val searchPadding = (12f * density).roundToInt()
    val startY = (floor(cardBounds.top).toInt() - searchPadding)
        .coerceIn(0, bitmap.height - 1)
    val viewportBottomExclusive = floor(viewportBounds.bottom).toInt()
        .coerceIn(startY + 1, bitmap.height)
    val lastSurfacePixel = (startY until viewportBottomExclusive).lastOrNull { y ->
        bitmap.getPixel(centerX, y).isNear(focusedSurface)
    }
    assertTrue(
        "$label expected the focused surface on its vertical center line",
        lastSurfacePixel != null,
    )

    val minimumClearance = density.roundToInt().coerceAtLeast(1)
    val actualClearance = viewportBottomExclusive - 1 - checkNotNull(lastSurfacePixel)
    assertTrue(
        "$label focused surface must stay at least 1dp above the grid clip boundary, " +
            "but clearance was ${actualClearance}px",
        actualClearance >= minimumClearance,
    )
}

private data class PixelBounds(
    val width: Int,
    val height: Int,
)

private fun Bitmap.findColorBounds(
    target: Int,
    searchBounds: Rect?,
    searchPadding: Int,
): PixelBounds {
    val startX = searchBounds?.let { floor(it.left).toInt() - searchPadding }
        ?.coerceIn(0, width - 1) ?: 0
    val endX = searchBounds?.let { ceil(it.right).toInt() + searchPadding - 1 }
        ?.coerceIn(startX, width - 1) ?: width - 1
    val startY = searchBounds?.let { floor(it.top).toInt() - searchPadding }
        ?.coerceIn(0, height - 1) ?: 0
    val endY = searchBounds?.let { ceil(it.bottom).toInt() + searchPadding - 1 }
        ?.coerceIn(startY, height - 1) ?: height - 1
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1
    for (y in startY..endY) {
        for (x in startX..endX) {
            if (getPixel(x, y) == target) {
                minX = minOf(minX, x)
                minY = minOf(minY, y)
                maxX = maxOf(maxX, x)
                maxY = maxOf(maxY, y)
            }
        }
    }
    assertTrue("Expected color #25334D in captured card", maxX >= minX && maxY >= minY)
    return PixelBounds(
        width = maxX - minX + 1,
        height = maxY - minY + 1,
    )
}

private fun Int.isNear(target: Int): Boolean {
    val distance = abs(Color.red(target) - Color.red(this)) +
        abs(Color.green(target) - Color.green(this)) +
        abs(Color.blue(target) - Color.blue(this))
    return Color.alpha(this) >= 250 && distance <= 9
}
