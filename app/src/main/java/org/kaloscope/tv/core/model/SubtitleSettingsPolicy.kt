package org.kaloscope.tv.core.model

import kotlin.math.roundToInt

object SubtitleSettingsPolicy {
    fun sanitize(settings: SubtitleSettings): SubtitleSettings =
        settings.copy(
            languagePreference = settings.languagePreference.trim(),
            timeOffsetSeconds = settings.timeOffsetSeconds.sanitizedOffset(),
            fontScalePercent = settings.fontScalePercent
                .roundToStep(FONT_SCALE_STEP)
                .coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE),
            verticalPositionPercent = settings.verticalPositionPercent
                .coerceIn(MIN_VERTICAL_POSITION, MAX_VERTICAL_POSITION),
        )

    fun adjustFontScale(
        settings: SubtitleSettings,
        offset: Int,
    ): SubtitleSettings =
        sanitize(
            settings.copy(
                fontScalePercent = settings.fontScalePercent + offset * FONT_SCALE_STEP,
            ),
        )

    fun adjustVerticalPosition(
        settings: SubtitleSettings,
        offset: Int,
    ): SubtitleSettings =
        sanitize(
            settings.copy(
                verticalPositionPercent = settings.verticalPositionPercent + offset,
            ),
        )

    fun adjustTimeOffset(
        settings: SubtitleSettings,
        offsetTenths: Int,
    ): SubtitleSettings =
        sanitize(
            settings.copy(
                timeOffsetSeconds = settings.timeOffsetSeconds + offsetTenths / 10f,
            ),
        )

    private fun Float.sanitizedOffset(): Float {
        if (!isFinite()) {
            return 0f
        }
        val tenths = (this * 10).roundToInt()
            .coerceIn(MIN_OFFSET_TENTHS, MAX_OFFSET_TENTHS)
        return tenths / 10f
    }

    private fun Int.roundToStep(step: Int): Int =
        (this.toFloat() / step).roundToInt() * step

    private const val MIN_FONT_SCALE = 50
    private const val MAX_FONT_SCALE = 200
    private const val FONT_SCALE_STEP = 5
    private const val MIN_VERTICAL_POSITION = 0
    private const val MAX_VERTICAL_POSITION = 15
    private const val MIN_OFFSET_TENTHS = -36_000
    private const val MAX_OFFSET_TENTHS = 36_000
}
