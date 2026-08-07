package org.kaloscope.tv.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.model.TextReaderTheme

class TextReaderThemeColorsTest {
    @Test
    fun `reader themes resolve to their approved background colors`() {
        val expected = mapOf(
            TextReaderTheme.White to Color(0xFFFAFAF5),
            TextReaderTheme.Cream to Color(0xFFFDF6E3),
            TextReaderTheme.Sepia to Color(0xFFF4ECD8),
            TextReaderTheme.LightGray to Color(0xFFE6E6E6),
            TextReaderTheme.Green to Color(0xFFDCE8D8),
            TextReaderTheme.Dark to Color(0xFF2B2B2B),
            TextReaderTheme.Slate to Color(0xFF1A2128),
            TextReaderTheme.Black to Color(0xFF000000),
        )

        assertEquals(expected, TextReaderTheme.entries.associateWith { it.readerBackgroundColor() })
    }
}
