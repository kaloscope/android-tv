package org.kaloscope.tv.feature.player

import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.DanmakuDataFilter
import com.kuaishou.akdanmaku.ecs.component.filter.DuplicateMergedFilter
import com.kuaishou.akdanmaku.ecs.component.filter.TextColorFilter
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize

internal fun List<DanmakuComment>.toAkDanmakuData(): List<DanmakuItemData> =
    duplicateGroups()
        .flatMap(DanmakuDuplicateGroup::candidates)
        .sortedWith(
            compareBy<DanmakuCandidate> { it.comment.startMillis }
                .thenBy(DanmakuCandidate::sourceIndex)
                .thenBy(DanmakuCandidate::mergedType),
        )
        .mapIndexed { index, candidate ->
            candidate.toAkDanmakuData(index.toLong())
        }

private fun List<DanmakuComment>.duplicateGroups(): List<DanmakuDuplicateGroup> {
    val latestGroups = mutableMapOf<String, DanmakuDuplicateGroup>()
    val groups = mutableListOf<DanmakuDuplicateGroup>()
    withIndex()
        .sortedWith(
            compareBy<IndexedValue<DanmakuComment>> { it.value.startMillis }
                .thenBy(IndexedValue<DanmakuComment>::index),
        )
        .forEach { indexedComment ->
            val comment = indexedComment.value
            val existing = latestGroups[comment.text]
            if (
                existing != null &&
                comment.startMillis - existing.startMillis <= DUPLICATE_MERGE_WINDOW_MILLIS
            ) {
                existing.comments += indexedComment
            } else {
                val group = DanmakuDuplicateGroup(
                    startMillis = comment.startMillis,
                    comments = mutableListOf(indexedComment),
                )
                latestGroups[comment.text] = group
                groups += group
            }
        }
    return groups
}

private fun DanmakuDuplicateGroup.candidates(): List<DanmakuCandidate> {
    if (comments.size == 1) {
        val only = comments.single()
        return listOf(
            DanmakuCandidate(
                sourceIndex = only.index,
                comment = only.value,
                content = only.value.text,
                mergedType = DanmakuItemData.MERGED_TYPE_NORMAL,
            ),
        )
    }
    val originals = comments.map { indexedComment ->
        DanmakuCandidate(
            sourceIndex = indexedComment.index,
            comment = indexedComment.value,
            content = indexedComment.value.text,
            mergedType = DanmakuItemData.MERGED_TYPE_ORIGINAL,
        )
    }
    val first = comments.first()
    return originals + DanmakuCandidate(
        sourceIndex = first.index,
        comment = first.value,
        content = "${first.value.text} X${comments.size}",
        mergedType = DanmakuItemData.MERGED_TYPE_MERGED,
    )
}

private fun DanmakuCandidate.toAkDanmakuData(danmakuId: Long): DanmakuItemData =
    DanmakuItemData(
        danmakuId = danmakuId,
        position = comment.startMillis,
        content = content,
        mode = comment.mode.toAkDanmakuMode(),
        textSize = BASE_TEXT_SIZE,
        textColor = comment.color.toArgbColor(),
        mergedType = mergedType,
    )

private data class DanmakuDuplicateGroup(
    val startMillis: Long,
    val comments: MutableList<IndexedValue<DanmakuComment>>,
)

private data class DanmakuCandidate(
    val sourceIndex: Int,
    val comment: DanmakuComment,
    val content: String,
    val mergedType: Int,
)

internal fun DanmakuSettings.toAkDanmakuConfig(): DanmakuConfig =
    toAkDanmakuConfig(
        typeFilter = TypeFilter(),
        colorFilter = TextColorFilter(),
        duplicateFilter = DuplicateMergedFilter(),
        filterGeneration = 0,
    )

internal class AkDanmakuRuntimeConfigState {
    private val typeFilter = TypeFilter()
    private val colorFilter = TextColorFilter()
    private val duplicateFilter = DuplicateMergedFilter()
    private var filterGeneration = 0

    fun update(settings: DanmakuSettings): DanmakuConfig {
        // DanmakuSystem increments an incoming generation once while installing it.
        // Emitting only even generations prevents the next update from colliding
        // with the odd generation retained by the dependency.
        filterGeneration += FILTER_GENERATION_STEP
        return settings.toAkDanmakuConfig(
            typeFilter = typeFilter,
            colorFilter = colorFilter,
            duplicateFilter = duplicateFilter,
            filterGeneration = filterGeneration,
        )
    }
}

private fun DanmakuSettings.toAkDanmakuConfig(
    typeFilter: TypeFilter,
    colorFilter: TextColorFilter,
    duplicateFilter: DuplicateMergedFilter,
    filterGeneration: Int,
): DanmakuConfig {
    typeFilter.clear()
    if (DanmakuDisplayMode.Scroll !in visibleModes) {
        typeFilter.addFilterItem(DanmakuItemData.DANMAKU_MODE_ROLLING)
    }
    if (DanmakuDisplayMode.Top !in visibleModes) {
        typeFilter.addFilterItem(DanmakuItemData.DANMAKU_MODE_CENTER_TOP)
    }
    if (DanmakuDisplayMode.Bottom !in visibleModes) {
        typeFilter.addFilterItem(DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM)
    }
    colorFilter.filterColor = mutableSetOf(WHITE_TEXT_RGB)
    duplicateFilter.enable = mergeDuplicates
    val dataFilters = buildList<DanmakuDataFilter> {
        add(typeFilter)
        if (blockColored) {
            add(colorFilter)
        }
        add(duplicateFilter)
    }
    return DanmakuConfig(
        durationMs = FIXED_DURATION_MILLIS,
        rollingDurationMs = speed.durationMillis,
        textSizeScale = textSize.scale,
        screenPart = (displayAreaPercent / 100f).coerceIn(0f, 1f),
        alpha = (opacityPercent / 100f).coerceIn(0f, 1f),
        visibility = enabled,
        allowOverlap = false,
        filterGeneration = filterGeneration,
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
private const val FILTER_GENERATION_STEP = 2
private const val DUPLICATE_MERGE_WINDOW_MILLIS = 10_000L
