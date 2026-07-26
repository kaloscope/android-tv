package org.kaloscope.tv.core.player

import org.kaloscope.tv.core.model.MediaChapter

object ChapterTimelinePolicy {
    fun markers(
        chapters: List<MediaChapter>,
        durationMillis: Long,
    ): List<Float> {
        if (durationMillis <= 0) {
            return emptyList()
        }
        return chapters
            .asSequence()
            .map(MediaChapter::startMillis)
            .filter { it > 0 && it < durationMillis }
            .distinct()
            .sorted()
            .map { start -> (start.toFloat() / durationMillis).coerceIn(0f, 1f) }
            .toList()
    }

    fun currentTitle(
        chapters: List<MediaChapter>,
        positionMillis: Long,
    ): String? =
        chapters
            .asSequence()
            .filter { it.startMillis <= positionMillis }
            .maxByOrNull(MediaChapter::startMillis)
            ?.title
}
