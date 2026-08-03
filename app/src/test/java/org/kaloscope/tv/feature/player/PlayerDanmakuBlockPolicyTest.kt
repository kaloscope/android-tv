package org.kaloscope.tv.feature.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings

class PlayerDanmakuBlockPolicyTest {
    @Test
    fun `selected options are blocked modes in fixed display order`() {
        val settings = DanmakuSettings(
            visibleModes = setOf(DanmakuDisplayMode.Top),
            blockColored = true,
        )

        assertEquals(
            listOf(
                PlayerDanmakuBlockOption.Scroll,
                PlayerDanmakuBlockOption.Bottom,
                PlayerDanmakuBlockOption.Colored,
            ),
            PlayerDanmakuBlockPolicy.selected(settings),
        )
    }

    @Test
    fun `toggle updates position visibility and color independently`() {
        val start = DanmakuSettings()
        val scrollBlocked = PlayerDanmakuBlockPolicy.toggle(
            start,
            PlayerDanmakuBlockOption.Scroll,
        )
        val colorBlocked = PlayerDanmakuBlockPolicy.toggle(
            scrollBlocked,
            PlayerDanmakuBlockOption.Colored,
        )

        assertFalse(DanmakuDisplayMode.Scroll in colorBlocked.visibleModes)
        assertTrue(colorBlocked.blockColored)
    }
}
