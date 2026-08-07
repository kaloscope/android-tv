package org.kaloscope.tv.core.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderChapterOrder

class ReaderChapterPolicyTest {
    private val chapters = listOf(
        ReaderChapter("a1", "A1", "Volume A"),
        ReaderChapter("a2", "A2", "Volume A"),
        ReaderChapter("b1", "B1", "Volume B"),
    )

    @Test
    fun `previous and next always follow source order`() {
        assertEquals(0, ReaderChapterPolicy.previousIndex(chapters, 1))
        assertEquals(2, ReaderChapterPolicy.nextIndex(chapters, 1))
        assertNull(ReaderChapterPolicy.previousIndex(chapters, 0))
        assertNull(ReaderChapterPolicy.nextIndex(chapters, 2))
    }

    @Test
    fun `complete volumes group chapters in selected display order`() {
        val ascending = ReaderChapterPolicy.displayGroups(
            chapters,
            ReaderChapterOrder.Ascending,
        )
        val descending = ReaderChapterPolicy.displayGroups(
            chapters,
            ReaderChapterOrder.Descending,
        )

        assertEquals(listOf("Volume A", "Volume B"), ascending.map { it.volume })
        assertEquals(listOf("a1", "a2", "b1"), ascending.flatMap { it.chapters }.map { it.id })
        assertEquals(listOf("Volume B", "Volume A"), descending.map { it.volume })
        assertEquals(listOf("b1", "a1", "a2"), descending.flatMap { it.chapters }.map { it.id })
    }

    @Test
    fun `any missing volume uses one ungrouped section`() {
        val incomplete = chapters.mapIndexed { index, chapter ->
            if (index == 1) chapter.copy(volume = null) else chapter
        }

        val groups = ReaderChapterPolicy.displayGroups(
            incomplete,
            ReaderChapterOrder.Ascending,
        )

        assertEquals(1, groups.size)
        assertNull(groups.single().volume)
        assertEquals(incomplete, groups.single().chapters)
    }
}
