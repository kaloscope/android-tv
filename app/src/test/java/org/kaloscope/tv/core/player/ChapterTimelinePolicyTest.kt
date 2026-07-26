package org.kaloscope.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.model.MediaChapter

class ChapterTimelinePolicyTest {
    @Test
    fun `markers exclude zero and out of range starts and remain sorted`() {
        val markers = ChapterTimelinePolicy.markers(
            chapters = listOf(
                chapter("late", 80_000, 100_000),
                chapter("opening", 0, 20_000),
                chapter("middle", 20_000, 50_000),
                chapter("past", 120_000, 140_000),
            ),
            durationMillis = 100_000,
        )

        assertEquals(listOf(0.2f, 0.8f), markers)
    }

    @Test
    fun `current title uses exact range then last started chapter inside gaps`() {
        val chapters = listOf(
            chapter("opening", 0, 10_000),
            chapter("middle", 20_000, 30_000),
        )

        assertEquals("opening", ChapterTimelinePolicy.currentTitle(chapters, 5_000))
        assertEquals("opening", ChapterTimelinePolicy.currentTitle(chapters, 15_000))
        assertEquals("middle", ChapterTimelinePolicy.currentTitle(chapters, 25_000))
    }

    @Test
    fun `empty chapters have no markers or title`() {
        assertEquals(emptyList<Float>(), ChapterTimelinePolicy.markers(emptyList(), 60_000))
        assertNull(ChapterTimelinePolicy.currentTitle(emptyList(), 10_000))
    }

    private fun chapter(
        title: String,
        start: Long,
        end: Long,
    ) = MediaChapter(title, title, start, end)
}
