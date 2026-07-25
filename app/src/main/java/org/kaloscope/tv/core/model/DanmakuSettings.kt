package org.kaloscope.tv.core.model

data class DanmakuSettings(
    val enabled: Boolean = true,
    val textSize: DanmakuTextSize = DanmakuTextSize.Medium,
    val speed: DanmakuSpeed = DanmakuSpeed.Standard,
    val opacityPercent: Int = 100,
    val displayAreaPercent: Int = 75,
    val visibleModes: Set<DanmakuDisplayMode> = DanmakuDisplayMode.entries.toSet(),
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
