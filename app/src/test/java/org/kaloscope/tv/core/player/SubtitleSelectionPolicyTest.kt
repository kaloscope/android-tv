package org.kaloscope.tv.core.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `restoration keeps a valid remembered track`() {
        assertEquals(
            "en",
            SubtitleSelectionPolicy.restoredTrackId(
                tracks = tracks(),
                rememberedTrackId = "en",
                settings = SubtitleSettings(enabled = false, languagePreference = "zh"),
            ),
        )
    }

    @Test
    fun `restoration falls back to preference then first track`() {
        assertEquals(
            "zh",
            SubtitleSelectionPolicy.restoredTrackId(
                tracks = tracks(),
                rememberedTrackId = "missing",
                settings = SubtitleSettings(enabled = false, languagePreference = "zh-cn"),
            ),
        )
        assertEquals(
            "first",
            SubtitleSelectionPolicy.restoredTrackId(
                tracks = tracks(),
                rememberedTrackId = null,
                settings = SubtitleSettings(enabled = false, languagePreference = "French"),
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

    @Test
    fun `matches merged external subtitle ids`() {
        assertTrue(SubtitleSelectionPolicy.matchesMedia3TrackId("1:en", "en", 0))
        assertTrue(SubtitleSelectionPolicy.matchesMedia3TrackId("3:zh", "zh", 2))
        assertTrue(SubtitleSelectionPolicy.matchesMedia3TrackId("2:subtitle:en", "subtitle:en", 1))
        assertTrue(SubtitleSelectionPolicy.matchesMedia3TrackId("2:1:en", "1:en", 1))
    }

    @Test
    fun `does not confuse subtitle ids or merged source indices`() {
        assertFalse(SubtitleSelectionPolicy.matchesMedia3TrackId("en", "en", 0))
        assertFalse(SubtitleSelectionPolicy.matchesMedia3TrackId("1:en", "1:en", 1))
        assertFalse(SubtitleSelectionPolicy.matchesMedia3TrackId("0:en", "en", 0))
        assertFalse(SubtitleSelectionPolicy.matchesMedia3TrackId("2:en", "en", 0))
        assertFalse(SubtitleSelectionPolicy.matchesMedia3TrackId("1:subtitle:en", "en", 0))
        assertFalse(SubtitleSelectionPolicy.matchesMedia3TrackId("1:zh", "en", 0))
        assertFalse(SubtitleSelectionPolicy.matchesMedia3TrackId(null, "en", 0))
        assertFalse(SubtitleSelectionPolicy.matchesMedia3TrackId("1:en", "en", -1))
    }

    private fun tracks() = listOf(
        SubtitleTrack("first", "繁体中文", "/first.vtt", "zh-TW"),
        SubtitleTrack("zh", "CHS 简体", "/zh.vtt", "zh-CN"),
        SubtitleTrack("en", "English", "/en.vtt", "en"),
    )
}
