package org.kaloscope.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuComment

class DanmakuSchedulerTest {
    @Test
    fun `scroll comment progress follows the player position`() {
        val timeline = DanmakuTimeline.create(
            comments = listOf(comment("first", 1_000)),
            config = DanmakuTimeline.Config(scrollDurationMillis = 8_000),
        )

        assertEquals(0f, timeline.framesAt(1_000).single().progress)
        assertEquals(0.5f, timeline.framesAt(5_000).single().progress)
        assertTrue(timeline.framesAt(9_000).isEmpty())
    }

    @Test
    fun `modes use independent lanes and fixed comments expire`() {
        val timeline = DanmakuTimeline.create(
            comments = listOf(
                comment("scroll", 1_000, "scroll"),
                comment("top", 1_000, "top"),
                comment("bottom", 1_000, "bottom"),
            ),
            config = DanmakuTimeline.Config(fixedDurationMillis = 4_000),
        )

        val frames = timeline.framesAt(2_000)

        assertEquals(
            listOf(DanmakuMode.Scroll, DanmakuMode.Top, DanmakuMode.Bottom),
            frames.map { it.mode },
        )
        assertTrue(timeline.framesAt(5_000).none { it.mode != DanmakuMode.Scroll })
    }

    @Test
    fun `simultaneous comments receive separate lanes and overflow is dropped`() {
        val timeline = DanmakuTimeline.create(
            comments = listOf(
                comment("first", 1_000),
                comment("second", 1_000),
                comment("overflow", 1_000),
            ),
            config = DanmakuTimeline.Config(scrollLaneCount = 2),
        )

        val frames = timeline.framesAt(2_000)

        assertEquals(listOf("first", "second"), frames.map { it.comment.text })
        assertEquals(listOf(0, 1), frames.map { it.lane })
    }

    @Test
    fun `seeking rebuilds the active frame from absolute time`() {
        val timeline = DanmakuTimeline.create(
            listOf(
                comment("early", 1_000),
                comment("late", 20_000),
            ),
        )

        assertEquals(listOf("late"), timeline.framesAt(21_000).map { it.comment.text })
        assertEquals(listOf("early"), timeline.framesAt(2_000).map { it.comment.text })
    }
}

private fun comment(
    text: String,
    startMillis: Long,
    mode: String = "scroll",
) = DanmakuComment(
    id = text,
    text = text,
    mode = mode,
    color = null,
    startMillis = startMillis,
)
