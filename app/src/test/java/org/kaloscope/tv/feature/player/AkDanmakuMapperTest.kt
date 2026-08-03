package org.kaloscope.tv.feature.player

import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TextColorFilter
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize

class AkDanmakuMapperTest {
    @Test
    fun `comments map modes colors milliseconds and dataset unique ids`() {
        val mapped = listOf(
            comment("duplicate", "Scroll", "scroll", "#12ABEF", 1_250),
            comment("duplicate", "Top", "top", null, 2_500),
            comment(null, "Bottom", "bottom", "broken", 3_750),
            comment("unknown", "Unknown", "special", "#000000", 5_000),
        ).toAkDanmakuData()

        assertEquals(listOf(0L, 1L, 2L, 3L), mapped.map { it.danmakuId })
        assertEquals(
            listOf(1_250L, 2_500L, 3_750L, 5_000L),
            mapped.map { it.position },
        )
        assertEquals(
            listOf(
                DanmakuItemData.DANMAKU_MODE_ROLLING,
                DanmakuItemData.DANMAKU_MODE_CENTER_TOP,
                DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM,
                DanmakuItemData.DANMAKU_MODE_ROLLING,
            ),
            mapped.map { it.mode },
        )
        assertEquals(0xFF12ABEF.toInt(), mapped[0].textColor)
        assertEquals(0xFFFFFFFF.toInt(), mapped[1].textColor)
        assertEquals(0xFFFFFFFF.toInt(), mapped[2].textColor)
        assertEquals(0xFF000000.toInt(), mapped[3].textColor)
        assertTrue(mapped.all { it.textSize == 25 })
    }

    @Test
    fun `settings map to non-overlapping Ak config and type filter`() {
        val config = DanmakuSettings(
            enabled = false,
            textSize = DanmakuTextSize.Large,
            speed = DanmakuSpeed.Fast,
            opacityPercent = 50,
            displayAreaPercent = 25,
            visibleModes = setOf(DanmakuDisplayMode.Scroll),
        ).toAkDanmakuConfig()

        assertFalse(config.visibility)
        assertEquals(1.4f, config.textSizeScale, 0f)
        assertEquals(6_000L, config.rollingDurationMs)
        assertEquals(4_000L, config.durationMs)
        assertEquals(0.5f, config.alpha, 0f)
        assertEquals(0.25f, config.screenPart, 0f)
        assertFalse(config.allowOverlap)

        val typeFilter = config.dataFilter.single() as TypeFilter
        assertFalse(
            DanmakuItemData.DANMAKU_MODE_ROLLING in typeFilter.filterSet,
        )
        assertTrue(
            DanmakuItemData.DANMAKU_MODE_CENTER_TOP in typeFilter.filterSet,
        )
        assertTrue(
            DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM in typeFilter.filterSet,
        )
    }

    @Test
    fun `colored blocking keeps only white RGB while type filtering stays independent`() {
        val config = DanmakuSettings(
            visibleModes = setOf(DanmakuDisplayMode.Scroll),
            blockColored = true,
        ).toAkDanmakuConfig()

        val colorFilter = config.dataFilter.filterIsInstance<TextColorFilter>().single()
        assertEquals(setOf(0xFFFFFF), colorFilter.filterColor)
        assertEquals(1, config.dataFilter.filterIsInstance<TypeFilter>().size)
    }
}

private fun comment(
    id: String?,
    text: String,
    mode: String,
    color: String?,
    startMillis: Long,
) = DanmakuComment(
    id = id,
    text = text,
    mode = mode,
    color = color,
    startMillis = startMillis,
)
