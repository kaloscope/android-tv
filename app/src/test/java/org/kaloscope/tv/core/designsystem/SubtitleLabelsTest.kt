package org.kaloscope.tv.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleLabelsTest {
    @Test
    fun `subtitle offset keeps its signed one-decimal format`() {
        assertEquals("0.0s", formatSubtitleOffset(0f))
        assertEquals("+1.2s", formatSubtitleOffset(1.2f))
        assertEquals("-0.5s", formatSubtitleOffset(-0.5f))
    }
}
