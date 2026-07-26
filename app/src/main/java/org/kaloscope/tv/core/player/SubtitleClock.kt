package org.kaloscope.tv.core.player

import kotlin.math.roundToLong

class SubtitleClock {
    @Volatile
    var offsetUs: Long = 0L
        private set

    @Volatile
    var version: Long = 0L
        private set

    fun setOffsetSeconds(seconds: Float) {
        val safeSeconds = seconds
            .takeIf(Float::isFinite)
            ?.coerceIn(-MAX_OFFSET_SECONDS, MAX_OFFSET_SECONDS)
            ?: 0f
        val updatedOffsetUs = (safeSeconds * MICROS_PER_SECOND).roundToLong()
        if (updatedOffsetUs == offsetUs) {
            return
        }
        offsetUs = updatedOffsetUs
        version += 1
    }

    fun adjustedPositionUs(videoPositionUs: Long): Long =
        (videoPositionUs - offsetUs).coerceAtLeast(0L)

    private companion object {
        const val MAX_OFFSET_SECONDS = 3_600f
        const val MICROS_PER_SECOND = 1_000_000f
    }
}
