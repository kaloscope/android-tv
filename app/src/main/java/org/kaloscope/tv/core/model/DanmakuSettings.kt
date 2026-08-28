package org.kaloscope.tv.core.model

data class DanmakuSettings(
    val enabled: Boolean = true,
    val textSize: DanmakuTextSize = DanmakuTextSize.Medium,
    val speed: DanmakuSpeed = DanmakuSpeed.Standard,
    val opacityPercent: Int = DanmakuSettingsPolicy.DEFAULT_OPACITY_PERCENT,
    val displayAreaPercent: Int = 75,
    val visibleModes: Set<DanmakuDisplayMode> = DanmakuDisplayMode.entries.toSet(),
    val blockColored: Boolean = false,
    val mergeDuplicates: Boolean = false,
)

enum class DanmakuTextSize {
    Small,
    Medium,
    Large,
    ExtraLarge,
}

enum class DanmakuSpeed {
    Slow,
    Standard,
    Fast,
}

enum class DanmakuDisplayMode {
    Scroll,
    Top,
    Bottom,
}
