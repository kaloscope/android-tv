package org.kaloscope.tv.core.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleTrack

class SubtitleSelectionPolicyTest {
    @Test
    fun `language preference matches language or label without case sensitivity`() {
        val selected = SubtitleSelectionPolicy.preferredTrackId(
            tracks = tracks(),
            settings = SubtitleSettings(languagePreference = "CHS|zh-cn"),
        )

        assertEquals("zh", selected)
    }

    @Test
    fun `invalid or unmatched preference falls back to first track`() {
        assertEquals(
            "first",
            SubtitleSelectionPolicy.preferredTrackId(
                tracks = tracks(),
                settings = SubtitleSettings(languagePreference = "["),
            ),
        )
        assertEquals(
            "first",
            SubtitleSelectionPolicy.preferredTrackId(
                tracks = tracks(),
                settings = SubtitleSettings(languagePreference = "French"),
            ),
        )
    }

    @Test
    fun `disabled subtitles do not select a default track`() {
        assertNull(
            SubtitleSelectionPolicy.preferredTrackId(
                tracks = tracks(),
                settings = SubtitleSettings(enabled = false),
            ),
        )
    }

    @Test
    fun `only selected subtitle receives default flag`() {
        val flags = listOf("first", "zh", "en").map {
            SubtitleSelectionPolicy.selectionFlags(it, "zh")
        }

        assertEquals(listOf(0, C.SELECTION_FLAG_DEFAULT, 0), flags)
    }

    private fun tracks() = listOf(
        SubtitleTrack("first", "繁体中文", "/first.vtt", "zh-TW"),
        SubtitleTrack("zh", "CHS 简体", "/zh.vtt", "zh-CN"),
        SubtitleTrack("en", "English", "/en.vtt", "en"),
    )
}
