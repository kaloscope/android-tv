package org.kaloscope.tv.feature.player

import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings

internal enum class PlayerDanmakuBlockOption {
    Scroll,
    Top,
    Bottom,
    Colored,
}

internal object PlayerDanmakuBlockPolicy {
    fun selected(settings: DanmakuSettings): List<PlayerDanmakuBlockOption> =
        PlayerDanmakuBlockOption.entries.filter { option ->
            when (option) {
                PlayerDanmakuBlockOption.Scroll ->
                    DanmakuDisplayMode.Scroll !in settings.visibleModes
                PlayerDanmakuBlockOption.Top ->
                    DanmakuDisplayMode.Top !in settings.visibleModes
                PlayerDanmakuBlockOption.Bottom ->
                    DanmakuDisplayMode.Bottom !in settings.visibleModes
                PlayerDanmakuBlockOption.Colored -> settings.blockColored
            }
        }

    fun toggle(
        settings: DanmakuSettings,
        option: PlayerDanmakuBlockOption,
    ): DanmakuSettings = when (option) {
        PlayerDanmakuBlockOption.Colored ->
            settings.copy(blockColored = !settings.blockColored)
        else -> {
            val mode = option.displayMode
            val visibleModes = if (mode in settings.visibleModes) {
                settings.visibleModes - mode
            } else {
                settings.visibleModes + mode
            }
            settings.copy(visibleModes = visibleModes)
        }
    }
}

private val PlayerDanmakuBlockOption.displayMode: DanmakuDisplayMode
    get() = when (this) {
        PlayerDanmakuBlockOption.Scroll -> DanmakuDisplayMode.Scroll
        PlayerDanmakuBlockOption.Top -> DanmakuDisplayMode.Top
        PlayerDanmakuBlockOption.Bottom -> DanmakuDisplayMode.Bottom
        PlayerDanmakuBlockOption.Colored -> error("Colored has no display mode")
    }
