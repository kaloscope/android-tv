package org.kaloscope.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuComment

class DanmakuSchedulerTest {
    @Test
    fun `visible window follows the current player position`() {
        val comments = listOf(
            comment("first", 1_000),
            comment("second", 8_000),
        )

        assertEquals(listOf("first"), visibleDanmakusAt(comments, 3_000).map { it.text })
        assertEquals(listOf("second"), visibleDanmakusAt(comments, 9_000).map { it.text })
    }

    @Test
    fun `seeking outside every window clears visible comments`() {
        assertTrue(visibleDanmakusAt(listOf(comment("first", 1_000)), 20_000).isEmpty())
    }
}

private fun comment(
    text: String,
    startMillis: Long,
) = DanmakuComment(
    id = text,
    text = text,
    mode = "scroll",
    color = null,
    startMillis = startMillis,
)
