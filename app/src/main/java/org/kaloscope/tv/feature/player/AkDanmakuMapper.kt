package org.kaloscope.tv.feature.player

import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.DanmakuDataFilter
import com.kuaishou.akdanmaku.ecs.component.filter.TextColorFilter
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize

internal fun List<DanmakuComment>.toAkDanmakuData(): List<DanmakuItemData> =
    mapIndexed { index, comment ->
        DanmakuItemData(
            danmakuId = index.toLong(),
            position = comment.startMillis,
            content = comment.text,
            mode = comment.mode.toAkDanmakuMode(),
            textSize = BASE_TEXT_SIZE,
            textColor = comment.color.toArgbColor(),
        )
    }

internal fun DanmakuSettings.toAkDanmakuConfig(): DanmakuConfig {
    val typeFilter = TypeFilter().apply {
        if (DanmakuDisplayMode.Scroll !in visibleModes) {
            addFilterItem(DanmakuItemData.DANMAKU_MODE_ROLLING)
        }
        if (DanmakuDisplayMode.Top !in visibleModes) {
            addFilterItem(DanmakuItemData.DANMAKU_MODE_CENTER_TOP)
        }
        if (DanmakuDisplayMode.Bottom !in visibleModes) {
            addFilterItem(DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM)
        }
    }
    val dataFilters = buildList<DanmakuDataFilter> {
        add(typeFilter)
        if (blockColored) {
            add(
                TextColorFilter().apply {
                    filterColor = mutableSetOf(WHITE_TEXT_RGB)
                },
            )
        }
    }
    return DanmakuConfig(
        durationMs = FIXED_DURATION_MILLIS,
        rollingDurationMs = speed.durationMillis,
        textSizeScale = textSize.scale,
        screenPart = (displayAreaPercent / 100f).coerceIn(0f, 1f),
        alpha = (opacityPercent / 100f).coerceIn(0f, 1f),
        visibility = enabled,
        allowOverlap = false,
        dataFilter = dataFilters,
    )
}

private fun String.toAkDanmakuMode(): Int =
    when (lowercase()) {
        "top" -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
        "bottom" -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
        else -> DanmakuItemData.DANMAKU_MODE_ROLLING
    }

private fun String?.toArgbColor(): Int {
    val rgb = this
        ?.takeIf { it.length == 7 && it.startsWith("#") }
        ?.substring(1)
        ?.toLongOrNull(16)
        ?: return DEFAULT_TEXT_COLOR
    return (OPAQUE_ALPHA or rgb).toInt()
}

private val DanmakuTextSize.scale: Float
    get() = when (this) {
        DanmakuTextSize.Small -> 0.9f
        DanmakuTextSize.Medium -> 1.15f
        DanmakuTextSize.Large -> 1.4f
        DanmakuTextSize.ExtraLarge -> 1.7f
    }

private val DanmakuSpeed.durationMillis: Long
    get() = when (this) {
        DanmakuSpeed.Slow -> 10_000L
        DanmakuSpeed.Standard -> 8_000L
        DanmakuSpeed.Fast -> 6_000L
    }

private const val BASE_TEXT_SIZE = 25
private const val FIXED_DURATION_MILLIS = 4_000L
private const val OPAQUE_ALPHA = 0xFF000000L
private const val DEFAULT_TEXT_COLOR = -0x1
private const val WHITE_TEXT_RGB = 0xFFFFFF
