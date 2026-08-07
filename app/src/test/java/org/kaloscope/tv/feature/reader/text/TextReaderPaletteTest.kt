package org.kaloscope.tv.feature.reader.text

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.model.TextReaderTheme

class TextReaderPaletteTest {
    @Test
    fun `all themes expose the approved WebUI colors`() {
        val expected = mapOf(
            TextReaderTheme.White to listOf(0xFFFAFAF5, 0xFF333333, 0xFF999999, 0xFFFFFFFF),
            TextReaderTheme.Cream to listOf(0xFFFDF6E3, 0xFF5C4B3A, 0xFF9A8978, 0xFFFFFFFF),
            TextReaderTheme.Sepia to listOf(0xFFF4ECD8, 0xFF5B4636, 0xFFA08B76, 0xFFFFFFFF),
            TextReaderTheme.LightGray to listOf(0xFFE6E6E6, 0xFF444444, 0xFF888888, 0xFFFFFFFF),
            TextReaderTheme.Green to listOf(0xFFDCE8D8, 0xFF3A4A3A, 0xFF6B7B6B, 0xFFFFFFFF),
            TextReaderTheme.Dark to listOf(0xFF2B2B2B, 0xFFCCCCCC, 0xFF666666, 0xFF222222),
            TextReaderTheme.Slate to listOf(0xFF1A2128, 0xFFB0BEC5, 0xFF546E7A, 0xFF1E242C),
            TextReaderTheme.Black to listOf(0xFF000000, 0xFFAAAAAA, 0xFF444444, 0xFF1A1A1A),
        )

        expected.forEach { (theme, colors) ->
            val palette = TextReaderPalettes.forTheme(theme)
            assertEquals(Color(colors[0]), palette.background)
            assertEquals(Color(colors[1]), palette.text)
            assertEquals(Color(colors[2]), palette.muted)
            assertEquals(Color(colors[3]), palette.panel)
        }
        assertEquals(
            0.06f,
            TextReaderPalettes.forTheme(TextReaderTheme.White).overlay.alpha,
            0.005f,
        )
        assertEquals(
            0.5f,
            TextReaderPalettes.forTheme(TextReaderTheme.Black).overlay.alpha,
            0.005f,
        )
    }
}
