package org.kaloscope.tv.data.media

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.data.media.remote.MediaChapterData
import org.kaloscope.tv.data.media.remote.MediaProbeData

class MediaProbeMapperTest {
    @Test
    fun `probe mapper converts seconds filters invalid chapters and sorts`() {
        val model = MediaProbeData(
            duration = 90.25,
            chapters = listOf(
                chapter("second", "第二章", 20.0, 40.0),
                chapter("negative", "无效", -1.0, 5.0),
                chapter("blank", " ", 5.5, 10.25),
                chapter("reverse", "无效", 12.0, 11.0),
                chapter("nan", "无效", Double.NaN, 12.0),
            ),
        ).toModel()

        assertEquals(90_250L, model.durationMillis)
        assertEquals(listOf("blank", "second"), model.chapters.map { it.id })
        assertEquals("章节 1", model.chapters.first().title)
        assertEquals(5_500L, model.chapters.first().startMillis)
        assertEquals(10_250L, model.chapters.first().endMillis)
    }

    @Test
    fun `invalid probe duration falls back to zero`() {
        val model = MediaProbeData(duration = Double.POSITIVE_INFINITY).toModel()

        assertEquals(0L, model.durationMillis)
    }

    private fun chapter(
        id: String,
        title: String,
        start: Double,
        end: Double,
    ) = MediaChapterData(
        id = id,
        title = title,
        start = start,
        end = end,
    )
}
