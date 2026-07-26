package org.kaloscope.tv.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatingDisplayPolicyTest {
    @Test
    fun `format returns one decimal for valid ratings`() {
        assertEquals("8.1", RatingDisplayPolicy.format(8.14))
        assertEquals("0.0", RatingDisplayPolicy.format(0.0))
        assertEquals("10.0", RatingDisplayPolicy.format(10.0))
    }

    @Test
    fun `format rejects missing non finite and out of range ratings`() {
        assertNull(RatingDisplayPolicy.format(null))
        assertNull(RatingDisplayPolicy.format(Double.NaN))
        assertNull(RatingDisplayPolicy.format(Double.POSITIVE_INFINITY))
        assertNull(RatingDisplayPolicy.format(-0.1))
        assertNull(RatingDisplayPolicy.format(10.1))
    }
}
