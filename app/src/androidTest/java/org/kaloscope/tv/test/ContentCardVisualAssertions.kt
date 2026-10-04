package org.kaloscope.tv.test

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

internal fun assertContentCardFocusOutline(
    label: String,
    bitmap: Bitmap,
    density: Float,
    visible: Boolean = true,
) {
    val outline = Color.rgb(0xE8, 0xED, 0xF4)
    val searchHeight = (12f * density).roundToInt().coerceIn(1, bitmap.height)
    val expectedWidth = (2f * density).roundToInt()
    listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
        val x = (bitmap.width * fraction).roundToInt().coerceIn(0, bitmap.width - 1)
        val borderPixels = (0 until searchHeight).count { y ->
            bitmap.getPixel(x, y).isNear(outline)
        }
        if (visible) {
            assertTrue(
                "$label expected a 2dp focus outline at x=$x but found $borderPixels pixels",
                borderPixels in (expectedWidth - 1).coerceAtLeast(1)..(expectedWidth + 1),
            )
        } else {
            assertEquals("$label should have no focus outline at x=$x", 0, borderPixels)
        }
    }
}

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
    val focusOutline = Color.rgb(0xE8, 0xED, 0xF4)
    val centerX = cardBounds.center.x.roundToInt().coerceIn(0, bitmap.width - 1)
    val searchPadding = (12f * density).roundToInt()
    val startY = (floor(cardBounds.top).toInt() - searchPadding)
        .coerceIn(0, bitmap.height - 1)
    val viewportBottomExclusive = floor(viewportBounds.bottom).toInt()
        .coerceIn(startY + 1, bitmap.height)
    val lastSurfacePixel = (startY until viewportBottomExclusive).lastOrNull { y ->
        val pixel = bitmap.getPixel(centerX, y)
        pixel.isNear(focusedSurface) || pixel.isNear(focusOutline)
    }
    assertTrue(
        "$label expected the focused surface on its vertical center line",
        lastSurfacePixel != null,
    )

    val minimumClearance = density.roundToInt().coerceAtLeast(1)
    val actualClearance = viewportBottomExclusive - 1 - checkNotNull(lastSurfacePixel)
    assertTrue(
        "$label focused card must stay at least 1dp above the grid clip boundary, " +
            "but clearance was ${actualClearance}px",
        actualClearance >= minimumClearance,
    )
}

private fun Int.isNear(target: Int): Boolean {
    val distance = abs(Color.red(target) - Color.red(this)) +
        abs(Color.green(target) - Color.green(this)) +
        abs(Color.blue(target) - Color.blue(this))
    return Color.alpha(this) >= 250 && distance <= 9
}
