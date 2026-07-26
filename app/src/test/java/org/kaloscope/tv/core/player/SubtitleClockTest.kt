package org.kaloscope.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleClockTest {
    @Test
    fun `positive offset delays and negative offset advances text clock`() {
        val clock = SubtitleClock()

        clock.setOffsetSeconds(0.5f)
        assertEquals(9_500_000L, clock.adjustedPositionUs(10_000_000L))
        clock.setOffsetSeconds(-0.5f)
        assertEquals(10_500_000L, clock.adjustedPositionUs(10_000_000L))
    }

    @Test
    fun `clock clamps before zero and increments version only on change`() {
        val clock = SubtitleClock()
        val initialVersion = clock.version

        clock.setOffsetSeconds(2f)
        assertEquals(0L, clock.adjustedPositionUs(1_000_000L))
        assertTrue(clock.version > initialVersion)
        val changedVersion = clock.version
        clock.setOffsetSeconds(2f)
        assertEquals(changedVersion, clock.version)
    }
}
