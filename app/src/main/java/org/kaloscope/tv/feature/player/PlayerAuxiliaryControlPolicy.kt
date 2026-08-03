package org.kaloscope.tv.feature.player

internal enum class PlayerAuxiliaryControl {
    Subtitle,
    Danmaku,
    Speed,
    Quality,
    Settings,
}

internal object PlayerAuxiliaryControlPolicy {
    fun visibleControls(
        subtitles: PlayerActionUiState,
        danmakus: PlayerActionUiState,
        quality: PlayerActionUiState,
        settings: PlayerActionUiState,
    ): List<PlayerAuxiliaryControl> = buildList {
        if (subtitles.enabled) add(PlayerAuxiliaryControl.Subtitle)
        if (danmakus.enabled) add(PlayerAuxiliaryControl.Danmaku)
        add(PlayerAuxiliaryControl.Speed)
        if (quality.enabled) add(PlayerAuxiliaryControl.Quality)
        if (settings.enabled) add(PlayerAuxiliaryControl.Settings)
    }
}
