package org.kaloscope.tv.feature.reader.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import org.kaloscope.tv.core.designsystem.readerBackgroundColor
import org.kaloscope.tv.core.model.TextReaderFont
import org.kaloscope.tv.core.model.TextReaderTheme

data class TextReaderPalette(
    val background: Color,
    val text: Color,
    val muted: Color,
    val panel: Color,
)

object TextReaderPalettes {
    fun forTheme(theme: TextReaderTheme): TextReaderPalette =
        when (theme) {
            TextReaderTheme.White -> palette(
                background = theme.readerBackgroundColor(),
                text = 0xFF333333,
                muted = 0xFF999999,
                panel = 0xFFFFFFFF,
            )

            TextReaderTheme.Cream -> palette(
                background = theme.readerBackgroundColor(),
                text = 0xFF5C4B3A,
                muted = 0xFF9A8978,
                panel = 0xFFFFFFFF,
            )

            TextReaderTheme.Sepia -> palette(
                background = theme.readerBackgroundColor(),
                text = 0xFF5B4636,
                muted = 0xFFA08B76,
                panel = 0xFFFFFFFF,
            )

            TextReaderTheme.LightGray -> palette(
                background = theme.readerBackgroundColor(),
                text = 0xFF444444,
                muted = 0xFF888888,
                panel = 0xFFFFFFFF,
            )

            TextReaderTheme.Green -> palette(
                background = theme.readerBackgroundColor(),
                text = 0xFF3A4A3A,
                muted = 0xFF6B7B6B,
                panel = 0xFFFFFFFF,
            )

            TextReaderTheme.Dark -> palette(
                background = theme.readerBackgroundColor(),
                text = 0xFFCCCCCC,
                muted = 0xFF666666,
                panel = 0xFF222222,
            )

            TextReaderTheme.Slate -> palette(
                background = theme.readerBackgroundColor(),
                text = 0xFFB0BEC5,
                muted = 0xFF546E7A,
                panel = 0xFF1E242C,
            )

            TextReaderTheme.Black -> palette(
                background = theme.readerBackgroundColor(),
                text = 0xFFAAAAAA,
                muted = 0xFF444444,
                panel = 0xFF1A1A1A,
            )
        }

    private fun palette(
        background: Color,
        text: Long,
        muted: Long,
        panel: Long,
    ) = TextReaderPalette(
        background = background,
        text = Color(text),
        muted = Color(muted),
        panel = Color(panel),
    )
}

internal fun TextReaderFont.toFontFamily(): FontFamily =
    when (this) {
        TextReaderFont.System -> FontFamily.Default
        TextReaderFont.Sans -> FontFamily.SansSerif
        TextReaderFont.Serif -> FontFamily.Serif
        TextReaderFont.Kai -> FontFamily.Cursive
        TextReaderFont.Monospace -> FontFamily.Monospace
    }
