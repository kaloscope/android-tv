package org.kaloscope.tv.test.performance

import android.util.SparseIntArray

internal data class FrameDurationSummary(
    val totalFrames: Int,
    val p50Millis: Int,
    val p95Millis: Int,
    val over100MillisRatio: Double,
)

internal fun summarizeFrames(histogram: SparseIntArray): FrameDurationSummary {
    val total = (0 until histogram.size()).sumOf { histogram.valueAt(it) }
    require(total > 0) { "Frame histogram is empty" }

    fun percentile(percent: Double): Int {
        val target = (total * percent).toInt().coerceAtLeast(1)
        var cumulative = 0
        for (index in 0 until histogram.size()) {
            cumulative += histogram.valueAt(index)
            if (cumulative >= target) return histogram.keyAt(index)
        }
        return histogram.keyAt(histogram.size() - 1)
    }

    val over100 = (0 until histogram.size())
        .filter { histogram.keyAt(it) > 100 }
        .sumOf { histogram.valueAt(it) }
    return FrameDurationSummary(
        totalFrames = total,
        p50Millis = percentile(0.50),
        p95Millis = percentile(0.95),
        over100MillisRatio = over100.toDouble() / total,
    )
}
