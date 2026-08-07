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
                paragraphSpacingEm = 1f,
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
                paragraphSpacingEm = 0f,
                horizontalPaddingDp = 0,
            ),
            ReaderSettingsPolicy.sanitize(
                TextReaderSettings(
                    fontSizeSp = 19,
                    lineHeight = 1.31f,
                    paragraphSpacingEm = -1f,
                    horizontalPaddingDp = -10,
                ),
            ),
        )
        assertEquals(
            TextReaderSettings(
                fontSizeSp = 44,
                lineHeight = 3f,
                paragraphSpacingEm = 2f,
                horizontalPaddingDp = 96,
            ),
            ReaderSettingsPolicy.sanitize(
                TextReaderSettings(
                    fontSizeSp = 45,
                    lineHeight = 3.1f,
                    paragraphSpacingEm = 2.4f,
                    horizontalPaddingDp = 102,
                ),
            ),
        )
        assertEquals(
            TextReaderSettings(
                fontSizeSp = 32,
                lineHeight = 1.8f,
                paragraphSpacingEm = 0.5f,
                horizontalPaddingDp = 48,
            ),
            ReaderSettingsPolicy.sanitize(
                TextReaderSettings(
                    fontSizeSp = 31,
                    lineHeight = 1.73f,
                    paragraphSpacingEm = 0.74f,
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
                    paragraphSpacingEm = Float.POSITIVE_INFINITY,
                ),
            ),
        )
    }
}
