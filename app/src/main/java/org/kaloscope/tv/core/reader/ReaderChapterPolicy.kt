package org.kaloscope.tv.core.reader

import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderChapterOrder

data class ReaderChapterGroup(
    val volume: String?,
    val chapters: List<ReaderChapter>,
)

object ReaderChapterPolicy {
    fun previousIndex(
        chapters: List<ReaderChapter>,
        selectedIndex: Int?,
    ): Int? = selectedIndex
        ?.minus(1)
        ?.takeIf(chapters.indices::contains)

    fun nextIndex(
        chapters: List<ReaderChapter>,
        selectedIndex: Int?,
    ): Int? = selectedIndex
        ?.plus(1)
        ?.takeIf(chapters.indices::contains)

    fun displayGroups(
        chapters: List<ReaderChapter>,
        order: ReaderChapterOrder,
    ): List<ReaderChapterGroup> {
        if (chapters.isEmpty()) return emptyList()
        if (chapters.any { it.volume.isNullOrBlank() }) {
            val displayed = when (order) {
                ReaderChapterOrder.Ascending -> chapters
                ReaderChapterOrder.Descending -> chapters.asReversed()
            }
            return listOf(ReaderChapterGroup(volume = null, chapters = displayed))
        }
        val groups = chapters
            .groupBy { checkNotNull(it.volume).trim() }
            .map { (volume, groupedChapters) ->
                ReaderChapterGroup(volume = volume, chapters = groupedChapters)
            }
        return when (order) {
            ReaderChapterOrder.Ascending -> groups
            ReaderChapterOrder.Descending -> groups.asReversed()
        }
    }
}
