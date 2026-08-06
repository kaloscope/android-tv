package org.kaloscope.tv.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuBlockPolicyTest {
    @Test
    fun `selected block types use the shared canonical order`() {
        val settings = DanmakuSettings(
            visibleModes = setOf(DanmakuDisplayMode.Top),
            blockColored = true,
        )

        assertEquals(
            listOf(
                DanmakuBlockType.Scroll,
                DanmakuBlockType.Bottom,
                DanmakuBlockType.Colored,
            ),
            DanmakuBlockPolicy.selected(settings),
        )
    }

    @Test
    fun `toggle changes only the requested block type`() {
        val start = DanmakuSettings()
        val scrollBlocked = DanmakuBlockPolicy.toggle(
            settings = start,
            type = DanmakuBlockType.Scroll,
        )
        val colorBlocked = DanmakuBlockPolicy.toggle(
            settings = scrollBlocked,
            type = DanmakuBlockType.Colored,
        )

        assertFalse(DanmakuDisplayMode.Scroll in colorBlocked.visibleModes)
        assertTrue(colorBlocked.blockColored)
        assertTrue(DanmakuDisplayMode.Top in colorBlocked.visibleModes)
        assertTrue(DanmakuDisplayMode.Bottom in colorBlocked.visibleModes)
    }
}
