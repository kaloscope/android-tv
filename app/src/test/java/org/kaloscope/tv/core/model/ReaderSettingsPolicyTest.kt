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
}
