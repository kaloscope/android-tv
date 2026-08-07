package org.kaloscope.tv.core.designsystem

import androidx.compose.ui.graphics.Color
import org.kaloscope.tv.core.model.TextReaderTheme

fun TextReaderTheme.readerBackgroundColor(): Color =
    when (this) {
        TextReaderTheme.White -> Color(0xFFFAFAF5)
        TextReaderTheme.Cream -> Color(0xFFFDF6E3)
        TextReaderTheme.Sepia -> Color(0xFFF4ECD8)
        TextReaderTheme.LightGray -> Color(0xFFE6E6E6)
        TextReaderTheme.Green -> Color(0xFFDCE8D8)
        TextReaderTheme.Dark -> Color(0xFF2B2B2B)
        TextReaderTheme.Slate -> Color(0xFF1A2128)
        TextReaderTheme.Black -> Color(0xFF000000)
    }
