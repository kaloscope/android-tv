package org.kaloscope.tv.test.golden

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoldenImageComparatorTest {
    @Test
    fun toleranceAndChangedRatioAreAppliedDeterministically() {
        val expected = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val actual = expected.copy(Bitmap.Config.ARGB_8888, true)
        actual.setPixel(0, 0, Color.rgb(255, 0, 0))

        val result = compareGolden(expected, actual)

        assertFalse(result.passed)
        assertEquals(0.01, result.changedPixelRatio, 0.0001)
        assertEquals(Color.MAGENTA, result.diff.getPixel(0, 0))
    }

    @Test
    fun channelDifferenceWithinTolerancePasses() {
        val expected = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val actual = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        expected.setPixel(0, 0, Color.rgb(20, 20, 20))
        actual.setPixel(0, 0, Color.rgb(28, 28, 28))

        assertTrue(compareGolden(expected, actual).passed)
    }
}
