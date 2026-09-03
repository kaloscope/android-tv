package org.kaloscope.tv.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderSettingsPolicyTest {
    @Test
    fun `reader defaults match first session defaults`() {
        assertEquals(ReaderChapterOrder.Ascending, TvSettings().readerChapterOrder)
        assertEquals(
            ImageReaderSettings(
                readMode = ImageReadMode.Scroll,
                zoomMode = ImageZoomMode.Auto,
                pageDirection = ImagePageDirection.Right,
            ),
            TvSettings().imageReader,
        )
        assertEquals(
            TextReaderSettings(
                theme = TextReaderTheme.White,
                font = TextReaderFont.System,
                fontSizeSp = 28,
                lineHeight = 1.8f,
                paragraphSpacingDp = 28,
                horizontalPaddingDp = 48,
            ),
            TvSettings().textReader,
        )
    }

    @Test
    fun `text values clamp and snap to supported TV steps`() {
        assertEquals(
            TextReaderSettings(
                fontSizeSp = 20,
                lineHeight = 1.4f,
                paragraphSpacingDp = 0,
                horizontalPaddingDp = 0,
            ),
            ReaderSettingsPolicy.sanitize(
                TextReaderSettings(
                    fontSizeSp = 19,
                    lineHeight = 1.31f,
                    paragraphSpacingDp = -1,
                    horizontalPaddingDp = -10,
                ),
            ),
        )
        assertEquals(
            TextReaderSettings(
                fontSizeSp = 44,
                lineHeight = 3f,
                paragraphSpacingDp = 88,
                horizontalPaddingDp = 96,
            ),
            ReaderSettingsPolicy.sanitize(
                TextReaderSettings(
                    fontSizeSp = 45,
                    lineHeight = 3.1f,
                    paragraphSpacingDp = 100,
                    horizontalPaddingDp = 102,
                ),
            ),
        )
        assertEquals(
            TextReaderSettings(
                fontSizeSp = 32,
                lineHeight = 1.8f,
                paragraphSpacingDp = 31,
                horizontalPaddingDp = 48,
            ),
            ReaderSettingsPolicy.sanitize(
                TextReaderSettings(
                    fontSizeSp = 31,
                    lineHeight = 1.73f,
                    paragraphSpacingDp = 31,
                    horizontalPaddingDp = 50,
                ),
            ),
        )
    }

    @Test
    fun `non finite text values use defaults`() {
        assertEquals(
            TextReaderSettings(),
            ReaderSettingsPolicy.sanitize(
                TextReaderSettings(
                    lineHeight = Float.NaN,
                ),
            ),
        )
    }

    @Test
    fun `text adjustments use the supported TV steps`() {
        val settings = TextReaderSettings()

        assertEquals(
            settings.copy(fontSizeSp = 30),
            ReaderSettingsPolicy.adjustFontSize(settings, 1),
        )
        assertEquals(
            settings.copy(lineHeight = 2f),
            ReaderSettingsPolicy.adjustLineHeight(settings, 1),
        )
        assertEquals(
            settings.copy(paragraphSpacingDp = 32),
            ReaderSettingsPolicy.adjustParagraphSpacing(settings, 1),
        )
        assertEquals(
            settings.copy(horizontalPaddingDp = 60),
            ReaderSettingsPolicy.adjustHorizontalPadding(settings, 1),
        )
    }

    @Test
    fun `text adjustments stop at supported bounds`() {
        val minimum = TextReaderSettings(
            fontSizeSp = 20,
            lineHeight = 1.4f,
            paragraphSpacingDp = 0,
            horizontalPaddingDp = 0,
        )
        val maximum = TextReaderSettings(
            fontSizeSp = 44,
            lineHeight = 3f,
            paragraphSpacingDp = 88,
            horizontalPaddingDp = 96,
        )

        assertEquals(minimum, ReaderSettingsPolicy.adjustFontSize(minimum, -1))
        assertEquals(minimum, ReaderSettingsPolicy.adjustLineHeight(minimum, -1))
        assertEquals(minimum, ReaderSettingsPolicy.adjustParagraphSpacing(minimum, -1))
        assertEquals(minimum, ReaderSettingsPolicy.adjustHorizontalPadding(minimum, -1))
        assertEquals(maximum, ReaderSettingsPolicy.adjustFontSize(maximum, 1))
        assertEquals(maximum, ReaderSettingsPolicy.adjustLineHeight(maximum, 1))
        assertEquals(maximum, ReaderSettingsPolicy.adjustParagraphSpacing(maximum, 1))
        assertEquals(maximum, ReaderSettingsPolicy.adjustHorizontalPadding(maximum, 1))
    }

    @Test
    fun `a text adjustment leaves unrelated settings unchanged`() {
        val settings = TextReaderSettings(
            lineHeight = 1.5f,
            paragraphSpacingDp = 13,
            horizontalPaddingDp = 25,
        )

        assertEquals(
            settings.copy(fontSizeSp = 30),
            ReaderSettingsPolicy.adjustFontSize(settings, 1),
        )
    }
}
