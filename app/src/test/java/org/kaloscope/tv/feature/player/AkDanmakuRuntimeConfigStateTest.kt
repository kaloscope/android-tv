package org.kaloscope.tv.feature.player

import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TextColorFilter
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings

class AkDanmakuRuntimeConfigStateTest {
    @Test
    fun `same-size positional update mutates installed filter and advances generation`() {
        val state = AkDanmakuRuntimeConfigState()
        val initialConfig = state.update(
            DanmakuSettings(
                visibleModes = setOf(DanmakuDisplayMode.Scroll),
            ),
        )
        val installedFilters = initialConfig.dataFilter.toList()
        val initialGeneration = initialConfig.filterGeneration

        val updatedConfig = state.update(
            DanmakuSettings(
                visibleModes = setOf(DanmakuDisplayMode.Top),
            ),
        )

        assertEquals(installedFilters.size, updatedConfig.dataFilter.size)
        val retainedTypeFilter = installedFilters.single() as TypeFilter
        assertTrue(
            DanmakuItemData.DANMAKU_MODE_ROLLING in retainedTypeFilter.filterSet,
        )
        assertTrue(
            DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM in retainedTypeFilter.filterSet,
        )
        assertTrue(
            DanmakuItemData.DANMAKU_MODE_CENTER_TOP !in retainedTypeFilter.filterSet,
        )
        assertTrue(updatedConfig.filterGeneration > initialGeneration)
    }

    @Test
    fun `next generation does not collide with dependency increment`() {
        val state = AkDanmakuRuntimeConfigState()
        var installedGeneration = 0

        repeat(3) { index ->
            val emittedConfig = state.update(
                DanmakuSettings(
                    visibleModes = if (index % 2 == 0) {
                        setOf(DanmakuDisplayMode.Scroll)
                    } else {
                        setOf(DanmakuDisplayMode.Top)
                    },
                ),
            )
            assertNotEquals(installedGeneration, emittedConfig.filterGeneration)

            // DanmakuSystem increments the incoming generation once when installing it.
            emittedConfig.filterGeneration += 1
            installedGeneration = emittedConfig.filterGeneration
        }
    }

    @Test
    fun `colored filter stays independent across same-size positional updates`() {
        val state = AkDanmakuRuntimeConfigState()
        val initialConfig = state.update(
            DanmakuSettings(
                visibleModes = setOf(DanmakuDisplayMode.Scroll),
                blockColored = true,
            ),
        )
        val installedTypeFilter = initialConfig.dataFilter
            .filterIsInstance<TypeFilter>()
            .single()
        val installedColorFilter = initialConfig.dataFilter
            .filterIsInstance<TextColorFilter>()
            .single()

        val updatedConfig = state.update(
            DanmakuSettings(
                visibleModes = setOf(DanmakuDisplayMode.Bottom),
                blockColored = true,
            ),
        )

        assertSame(
            installedTypeFilter,
            updatedConfig.dataFilter.filterIsInstance<TypeFilter>().single(),
        )
        assertSame(
            installedColorFilter,
            updatedConfig.dataFilter.filterIsInstance<TextColorFilter>().single(),
        )
        assertEquals(setOf(0xFFFFFF), installedColorFilter.filterColor)
        assertEquals(
            setOf(
                DanmakuItemData.DANMAKU_MODE_ROLLING,
                DanmakuItemData.DANMAKU_MODE_CENTER_TOP,
            ),
            installedTypeFilter.filterSet,
        )
    }
}
