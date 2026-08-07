package org.kaloscope.tv.feature.reader.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import org.kaloscope.tv.core.model.TextReaderFont
import org.kaloscope.tv.core.model.TextReaderTheme

data class TextReaderPalette(
    val background: Color,
    val text: Color,
    val muted: Color,
    val panel: Color,
    val overlay: Color,
)

object TextReaderPalettes {
    fun forTheme(theme: TextReaderTheme): TextReaderPalette =
        when (theme) {
            TextReaderTheme.White -> palette(
                background = 0xFFFAFAF5,
                text = 0xFF333333,
                muted = 0xFF999999,
                panel = 0xFFFFFFFF,
                overlayAlpha = 0.06f,
            )

            TextReaderTheme.Cream -> palette(
                background = 0xFFFDF6E3,
                text = 0xFF5C4B3A,
                muted = 0xFF9A8978,
                panel = 0xFFFFFFFF,
                overlayAlpha = 0.08f,
            )

            TextReaderTheme.Sepia -> palette(
                background = 0xFFF4ECD8,
                text = 0xFF5B4636,
                muted = 0xFFA08B76,
                panel = 0xFFFFFFFF,
                overlayAlpha = 0.08f,
            )

            TextReaderTheme.LightGray -> palette(
                background = 0xFFE6E6E6,
                text = 0xFF444444,
                muted = 0xFF888888,
                panel = 0xFFFFFFFF,
                overlayAlpha = 0.08f,
            )

            TextReaderTheme.Green -> palette(
                background = 0xFFDCE8D8,
                text = 0xFF3A4A3A,
                muted = 0xFF6B7B6B,
                panel = 0xFFFFFFFF,
                overlayAlpha = 0.08f,
            )

            TextReaderTheme.Dark -> palette(
                background = 0xFF2B2B2B,
                text = 0xFFCCCCCC,
                muted = 0xFF666666,
                panel = 0xFF222222,
                overlayAlpha = 0.5f,
            )

            TextReaderTheme.Slate -> palette(
                background = 0xFF1A2128,
                text = 0xFFB0BEC5,
                muted = 0xFF546E7A,
                panel = 0xFF1E242C,
                overlayAlpha = 0.5f,
            )

            TextReaderTheme.Black -> palette(
                background = 0xFF000000,
                text = 0xFFAAAAAA,
                muted = 0xFF444444,
                panel = 0xFF1A1A1A,
                overlayAlpha = 0.5f,
            )
        }

    private fun palette(
        background: Long,
        text: Long,
        muted: Long,
        panel: Long,
        overlayAlpha: Float,
    ) = TextReaderPalette(
        background = Color(background),
        text = Color(text),
        muted = Color(muted),
        panel = Color(panel),
        overlay = Color.Black.copy(alpha = overlayAlpha),
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
