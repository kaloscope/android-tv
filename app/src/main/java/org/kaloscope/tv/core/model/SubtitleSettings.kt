package org.kaloscope.tv.core.model

enum class SubtitleDisplayMode {
    Stroke,
    Background,
}

data class SubtitleSettings(
    val enabled: Boolean = true,
    val languagePreference: String = "",
    val displayMode: SubtitleDisplayMode = SubtitleDisplayMode.Stroke,
    val timeOffsetSeconds: Float = 0f,
    val fontScalePercent: Int = 100,
    val verticalPositionPercent: Int = 2,
)
