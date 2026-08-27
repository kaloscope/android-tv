package org.kaloscope.tv.core.model

import kotlin.math.roundToInt

enum class ReaderChapterOrder {
    Ascending,
    Descending,
}

enum class ImageReadMode {
    Scroll,
    Paged,
}

enum class ImageZoomMode {
    Auto,
    FitWidth,
    FitHeight,
}

enum class ImagePageDirection {
    Right,
    Left,
    Down,
}

data class ImageReaderSettings(
    val readMode: ImageReadMode = ImageReadMode.Scroll,
    val zoomMode: ImageZoomMode = ImageZoomMode.Auto,
    val pageDirection: ImagePageDirection = ImagePageDirection.Right,
)

enum class TextReaderTheme {
    White,
    Cream,
    Sepia,
    LightGray,
    Green,
    Dark,
    Slate,
    Black,
}

enum class TextReaderFont {
    System,
    Sans,
    Serif,
    Kai,
    Monospace,
}

data class TextReaderSettings(
    val theme: TextReaderTheme = TextReaderTheme.White,
    val font: TextReaderFont = TextReaderFont.System,
    val fontSizeSp: Int = ReaderSettingsPolicy.DEFAULT_FONT_SIZE_SP,
    val lineHeight: Float = ReaderSettingsPolicy.DEFAULT_LINE_HEIGHT,
    val paragraphSpacingDp: Int = ReaderSettingsPolicy.DEFAULT_PARAGRAPH_SPACING_DP,
    val horizontalPaddingDp: Int = ReaderSettingsPolicy.DEFAULT_HORIZONTAL_PADDING_DP,
)

object ReaderSettingsPolicy {
    const val MIN_FONT_SIZE_SP = 20
    const val MAX_FONT_SIZE_SP = 44
    const val FONT_SIZE_STEP_SP = 2
    const val DEFAULT_FONT_SIZE_SP = 28

    const val MIN_LINE_HEIGHT = 1.4f
    const val MAX_LINE_HEIGHT = 3f
    const val LINE_HEIGHT_STEP = 0.2f
    const val DEFAULT_LINE_HEIGHT = 1.8f

    const val MIN_PARAGRAPH_SPACING_DP = 0
    const val MAX_PARAGRAPH_SPACING_DP = 88
    const val PARAGRAPH_SPACING_STEP_DP = 4
    const val DEFAULT_PARAGRAPH_SPACING_DP = 28

    const val MIN_HORIZONTAL_PADDING_DP = 0
    const val MAX_HORIZONTAL_PADDING_DP = 96
    const val HORIZONTAL_PADDING_STEP_DP = 12
    const val DEFAULT_HORIZONTAL_PADDING_DP = 48

    fun sanitize(settings: TextReaderSettings): TextReaderSettings =
        settings.copy(
            fontSizeSp = snapInt(
                value = settings.fontSizeSp,
                minimum = MIN_FONT_SIZE_SP,
                maximum = MAX_FONT_SIZE_SP,
                step = FONT_SIZE_STEP_SP,
            ),
            lineHeight = snapTenths(
                value = settings.lineHeight,
                default = DEFAULT_LINE_HEIGHT,
                minimumTenths = 14,
                maximumTenths = 30,
                stepTenths = 2,
            ),
            // Preserve exact legacy values even when they are outside the new 4 dp grid.
            paragraphSpacingDp = settings.paragraphSpacingDp.coerceIn(
                MIN_PARAGRAPH_SPACING_DP,
                MAX_PARAGRAPH_SPACING_DP,
            ),
            horizontalPaddingDp = snapInt(
                value = settings.horizontalPaddingDp,
                minimum = MIN_HORIZONTAL_PADDING_DP,
                maximum = MAX_HORIZONTAL_PADDING_DP,
                step = HORIZONTAL_PADDING_STEP_DP,
            ),
        )

    private fun snapInt(
        value: Int,
        minimum: Int,
        maximum: Int,
        step: Int,
    ): Int {
        val clamped = value.coerceIn(minimum, maximum)
        val stepIndex = ((clamped - minimum).toFloat() / step).roundToInt()
        return (minimum + stepIndex * step).coerceIn(minimum, maximum)
    }

    private fun snapTenths(
        value: Float,
        default: Float,
        minimumTenths: Int,
        maximumTenths: Int,
        stepTenths: Int,
    ): Float {
        if (!value.isFinite()) return default
        val valueTenths = (value * 10f).roundToInt()
        return snapInt(
            value = valueTenths,
            minimum = minimumTenths,
            maximum = maximumTenths,
            step = stepTenths,
        ) / 10f
    }
}
