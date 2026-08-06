package org.kaloscope.tv.core.model

enum class DanmakuBlockType {
    Scroll,
    Top,
    Bottom,
    Colored,
}

object DanmakuBlockPolicy {
    fun selected(settings: DanmakuSettings): List<DanmakuBlockType> =
        DanmakuBlockType.entries.filter { type -> isSelected(settings, type) }

    fun isSelected(
        settings: DanmakuSettings,
        type: DanmakuBlockType,
    ): Boolean = when (type) {
        DanmakuBlockType.Scroll -> DanmakuDisplayMode.Scroll !in settings.visibleModes
        DanmakuBlockType.Top -> DanmakuDisplayMode.Top !in settings.visibleModes
        DanmakuBlockType.Bottom -> DanmakuDisplayMode.Bottom !in settings.visibleModes
        DanmakuBlockType.Colored -> settings.blockColored
    }

    fun toggle(
        settings: DanmakuSettings,
        type: DanmakuBlockType,
    ): DanmakuSettings = when (type) {
        DanmakuBlockType.Colored ->
            settings.copy(blockColored = !settings.blockColored)

        else -> {
            val displayMode = type.displayMode
            val visibleModes = if (displayMode in settings.visibleModes) {
                settings.visibleModes - displayMode
            } else {
                settings.visibleModes + displayMode
            }
            settings.copy(visibleModes = visibleModes)
        }
    }
}

private val DanmakuBlockType.displayMode: DanmakuDisplayMode
    get() = when (this) {
        DanmakuBlockType.Scroll -> DanmakuDisplayMode.Scroll
        DanmakuBlockType.Top -> DanmakuDisplayMode.Top
        DanmakuBlockType.Bottom -> DanmakuDisplayMode.Bottom
        DanmakuBlockType.Colored -> error("Colored has no display mode")
    }
