package org.kaloscope.tv.test.performance

import android.util.SparseIntArray
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameDurationSummaryTest {
    @Test
    fun summaryCalculatesPercentilesAndSlowRatio() {
        val histogram = SparseIntArray().apply {
            put(16, 50)
            put(32, 44)
            put(120, 6)
        }

        val summary = summarizeFrames(histogram)

        assertEquals(100, summary.totalFrames)
        assertEquals(16, summary.p50Millis)
        assertEquals(120, summary.p95Millis)
        assertEquals(0.06, summary.over100MillisRatio, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyHistogramIsRejected() {
        summarizeFrames(SparseIntArray())
    }
}
