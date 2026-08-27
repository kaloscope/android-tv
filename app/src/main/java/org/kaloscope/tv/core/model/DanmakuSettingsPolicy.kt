package org.kaloscope.tv.core.model

object DanmakuSettingsPolicy {
    val PERCENTAGE_VALUES: List<Int> = (20..100 step 5).toList()
    const val DEFAULT_OPACITY_PERCENT = 80

    fun adjustOpacity(
        settings: DanmakuSettings,
        offset: Int,
    ): DanmakuSettings = settings.copy(
        opacityPercent = adjustPercentage(
            value = settings.opacityPercent,
            fallback = DEFAULT_OPACITY_PERCENT,
            offset = offset,
        ),
    )

    fun adjustDisplayArea(
        settings: DanmakuSettings,
        offset: Int,
    ): DanmakuSettings = settings.copy(
        displayAreaPercent = adjustPercentage(
            value = settings.displayAreaPercent,
            fallback = DEFAULT_DISPLAY_AREA_PERCENT,
            offset = offset,
        ),
    )

    fun validPercentage(value: Int?, fallback: Int): Int {
        require(fallback in PERCENTAGE_VALUES)
        return value?.takeIf(PERCENTAGE_VALUES::contains) ?: fallback
    }

    private fun adjustPercentage(
        value: Int,
        fallback: Int,
        offset: Int,
    ): Int {
        val current = validPercentage(value, fallback)
        val currentIndex = PERCENTAGE_VALUES.indexOf(current)
        val targetIndex = (currentIndex + offset).coerceIn(PERCENTAGE_VALUES.indices)
        return PERCENTAGE_VALUES[targetIndex]
    }

    private const val DEFAULT_DISPLAY_AREA_PERCENT = 75
}
