package org.kaloscope.tv.core.player

class PlaybackProgressRecorder(
    private val intervalMillis: Long = 15_000,
) {
    private var lastRecordedAtMillis: Long? = null
    private var lastPositionSeconds: Long? = null
    private var lastPercentage: Int? = null

    fun shouldRecord(
        positionMillis: Long,
        durationMillis: Long,
        nowMillis: Long,
        reason: ProgressReason,
    ): Boolean {
        if (durationMillis <= 0 || positionMillis < 0) {
            return false
        }
        val safePosition = positionMillis.coerceAtMost(durationMillis)
        val positionSeconds = safePosition / 1_000
        val percentage = ((safePosition * 100) / durationMillis).toInt().coerceIn(0, 100)
        val changed = positionSeconds != lastPositionSeconds || percentage != lastPercentage
        if (!changed) {
            return false
        }
        val intervalElapsed =
            lastRecordedAtMillis?.let { nowMillis - it >= intervalMillis } ?: true
        // Lifecycle events bypass throttling, but unchanged progress still stays deduplicated.
        if (reason == ProgressReason.Periodic && !intervalElapsed) {
            return false
        }
        lastRecordedAtMillis = nowMillis
        lastPositionSeconds = positionSeconds
        lastPercentage = percentage
        return true
    }
}

enum class ProgressReason {
    Started,
    Periodic,
    Paused,
    Seeked,
    Exit,
    Error,
}
