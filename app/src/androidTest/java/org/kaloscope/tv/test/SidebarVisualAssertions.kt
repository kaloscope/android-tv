package org.kaloscope.tv.test

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.accentPalette
import org.kaloscope.tv.core.model.AccentColor

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

    val restingSurface = Panel.copy(alpha = 0.72f).compositeOver(Background).toArgb()
    assertColorNear(
        label = "$label transparent resting surface",
        expected = restingSurface,
        actual = unselectedPixel,
    )
    val expectedBlue = Color.blue(restingSurface)
    assertTrue(
        "$label transparent resting surface blue channel expected $expectedBlue ± 1 " +
            "but was ${Color.blue(unselectedPixel)}",
        Color.blue(unselectedPixel) in (expectedBlue - 1)..(expectedBlue + 1),
    )
    assertColorNear(
        label = "$label selected surface",
        expected = AccentColor.Blue.accentPalette().panelSelected.toArgb(),
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
