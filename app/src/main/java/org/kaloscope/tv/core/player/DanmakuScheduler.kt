package org.kaloscope.tv.core.player

import org.kaloscope.tv.core.model.DanmakuComment

enum class DanmakuMode {
    Scroll,
    Top,
    Bottom,
}

data class DanmakuFrame(
    val comment: DanmakuComment,
    val mode: DanmakuMode,
    val lane: Int,
    val progress: Float,
)

class DanmakuTimeline private constructor(
    private val entries: List<Entry>,
) {
    data class Config(
        val scrollDurationMillis: Long = 8_000,
        val fixedDurationMillis: Long = 4_000,
        val scrollLaneCount: Int = 8,
        val topLaneCount: Int = 2,
        val bottomLaneCount: Int = 2,
    )

    fun framesAt(positionMillis: Long): List<DanmakuFrame> {
        if (positionMillis < 0) {
            return emptyList()
        }
        return entries.mapNotNull { entry ->
            if (positionMillis < entry.startMillis || positionMillis >= entry.endMillis) {
                null
            } else {
                DanmakuFrame(
                    comment = entry.comment,
                    mode = entry.mode,
                    lane = entry.lane,
                    progress = (
                        (positionMillis - entry.startMillis).toFloat() /
                            (entry.endMillis - entry.startMillis)
                        ).coerceIn(0f, 1f),
                )
            }
        }
    }

    companion object {
        fun create(
            comments: List<DanmakuComment>,
            config: Config = Config(),
        ): DanmakuTimeline {
            val laneAvailability = mapOf(
                DanmakuMode.Scroll to LongArray(config.scrollLaneCount.coerceAtLeast(0)),
                DanmakuMode.Top to LongArray(config.topLaneCount.coerceAtLeast(0)),
                DanmakuMode.Bottom to LongArray(config.bottomLaneCount.coerceAtLeast(0)),
            )
            val entries = comments
                .withIndex()
                .sortedWith(compareBy<IndexedValue<DanmakuComment>> { it.value.startMillis }
                    .thenBy { it.index })
                .mapNotNull { indexed ->
                    val comment = indexed.value
                    val mode = comment.mode.toDanmakuMode()
                    val duration = when (mode) {
                        DanmakuMode.Scroll -> config.scrollDurationMillis
                        DanmakuMode.Top, DanmakuMode.Bottom -> config.fixedDurationMillis
                    }
                    if (comment.startMillis < 0 || duration <= 0) {
                        return@mapNotNull null
                    }
                    val lanes = checkNotNull(laneAvailability[mode])
                    val lane = lanes.indexOfFirst { it <= comment.startMillis }
                    if (lane < 0) {
                        return@mapNotNull null
                    }
                    val endMillis = comment.startMillis + duration
                    // Lane ownership is fixed once so active comments never jump after a seek.
                    lanes[lane] = endMillis
                    Entry(
                        comment = comment,
                        mode = mode,
                        lane = lane,
                        startMillis = comment.startMillis,
                        endMillis = endMillis,
                    )
                }
            return DanmakuTimeline(entries)
        }
    }

    private data class Entry(
        val comment: DanmakuComment,
        val mode: DanmakuMode,
        val lane: Int,
        val startMillis: Long,
        val endMillis: Long,
    )
}

private fun String.toDanmakuMode(): DanmakuMode =
    when (lowercase()) {
        "top" -> DanmakuMode.Top
        "bottom" -> DanmakuMode.Bottom
        else -> DanmakuMode.Scroll
    }
