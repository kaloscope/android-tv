package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.kaloscope.tv.R
import org.kaloscope.tv.core.model.ImagePageDirection
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageZoomMode
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.TextReaderFont
import org.kaloscope.tv.core.model.TextReaderTheme

@Composable
fun readerChapterOrderLabel(value: ReaderChapterOrder): String =
    stringResource(
        when (value) {
            ReaderChapterOrder.Ascending -> R.string.reader_order_ascending
            ReaderChapterOrder.Descending -> R.string.reader_order_descending
        },
    )

@Composable
fun imageReadModeLabel(value: ImageReadMode): String =
    stringResource(
        when (value) {
            ImageReadMode.Scroll -> R.string.reader_mode_scroll
            ImageReadMode.Paged -> R.string.reader_mode_paged
        },
    )

@Composable
fun imageZoomModeLabel(value: ImageZoomMode): String =
    stringResource(
        when (value) {
            ImageZoomMode.Auto -> R.string.reader_zoom_auto
            ImageZoomMode.FitWidth -> R.string.reader_zoom_fit_width
            ImageZoomMode.FitHeight -> R.string.reader_zoom_fit_height
        },
    )

@Composable
fun imagePageDirectionLabel(value: ImagePageDirection): String =
    stringResource(
        when (value) {
            ImagePageDirection.Right -> R.string.reader_direction_right
            ImagePageDirection.Left -> R.string.reader_direction_left
            ImagePageDirection.Down -> R.string.reader_direction_down
        },
    )

@Composable
fun textReaderThemeLabel(value: TextReaderTheme): String =
    stringResource(
        when (value) {
            TextReaderTheme.White -> R.string.reader_theme_white
            TextReaderTheme.Cream -> R.string.reader_theme_cream
            TextReaderTheme.Sepia -> R.string.reader_theme_sepia
            TextReaderTheme.LightGray -> R.string.reader_theme_light_gray
            TextReaderTheme.Green -> R.string.reader_theme_green
            TextReaderTheme.Dark -> R.string.reader_theme_dark
            TextReaderTheme.Slate -> R.string.reader_theme_slate
            TextReaderTheme.Black -> R.string.reader_theme_black
        },
    )

@Composable
fun textReaderFontLabel(value: TextReaderFont): String =
    stringResource(
        when (value) {
            TextReaderFont.System -> R.string.reader_font_system
            TextReaderFont.Sans -> R.string.reader_font_sans
            TextReaderFont.Serif -> R.string.reader_font_serif
            TextReaderFont.Kai -> R.string.reader_font_kai
            TextReaderFont.Monospace -> R.string.reader_font_monospace
        },
    )
