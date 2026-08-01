package org.kaloscope.tv.test

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import org.junit.Assert.assertTrue

fun assertSidebarNavigationSurfaces(
    label: String,
    selected: Bitmap,
    unselected: Bitmap,
    sampleInset: Int,
) {
    val selectedPixel = selected.getPixel(
        selected.width - sampleInset,
        selected.height / 2,
    )
    val unselectedPixel = unselected.getPixel(
        unselected.width - sampleInset,
        unselected.height / 2,
    )

    assertColorNear(
        label = "$label transparent resting surface",
        expected = Color.rgb(0x0E, 0x14, 0x21),
        actual = unselectedPixel,
    )
    assertColorNear(
        label = "$label selected surface",
        expected = Color.rgb(0x20, 0x2B, 0x40),
        actual = selectedPixel,
    )
}

private fun assertColorNear(
    label: String,
    expected: Int,
    actual: Int,
    tolerance: Int = 2,
) {
    assertTrue(
        "$label expected ${expected.toHex()} but was ${actual.toHex()}",
        abs(Color.red(expected) - Color.red(actual)) <= tolerance &&
            abs(Color.green(expected) - Color.green(actual)) <= tolerance &&
            abs(Color.blue(expected) - Color.blue(actual)) <= tolerance,
    )
}

private fun Int.toHex(): String = String.format("#%08X", this)
