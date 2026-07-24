package org.kaloscope.tv.core.player

import org.kaloscope.tv.core.model.DanmakuComment

fun visibleDanmakusAt(
    comments: List<DanmakuComment>,
    positionMillis: Long,
    visibilityMillis: Long = 5_000,
    limit: Int = 4,
): List<DanmakuComment> {
    if (positionMillis < 0 || visibilityMillis <= 0 || limit <= 0) {
        return emptyList()
    }
    // Deriving from position makes seek and episode changes rebuild the window automatically.
    return comments
        .asSequence()
        .filter {
            positionMillis >= it.startMillis &&
                positionMillis < it.startMillis + visibilityMillis
        }
        .sortedBy(DanmakuComment::startMillis)
        .take(limit)
        .toList()
}
