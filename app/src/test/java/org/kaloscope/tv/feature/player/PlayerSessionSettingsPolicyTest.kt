package org.kaloscope.tv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleTrack

class PlayerSessionSettingsPolicyTest {
    @Test
    fun `subtitle toggle remembers and restores the active track`() {
        val initial = PlayerSessionSettingsPolicy.initial(
            tracks = tracks(),
            subtitleSettings = SubtitleSettings(languagePreference = "zh-cn"),
            danmakuSettings = DanmakuSettings(),
        )

        val disabled = PlayerSessionSettingsPolicy.toggleSubtitles(initial, tracks())
        val restored = PlayerSessionSettingsPolicy.toggleSubtitles(disabled, tracks())

        assertNull(disabled.selectedSubtitleTrackId)
        assertEquals("zh", disabled.rememberedSubtitleTrackId)
        assertFalse(disabled.subtitleSettings.enabled)
        assertEquals("zh", restored.selectedSubtitleTrackId)
        assertTrue(restored.subtitleSettings.enabled)
    }

    @Test
    fun `selecting another track enables and remembers it`() {
        val initial = PlayerSessionSettingsPolicy.initial(
            tracks = tracks(),
            subtitleSettings = SubtitleSettings(enabled = false),
            danmakuSettings = DanmakuSettings(),
        )

        val selected = PlayerSessionSettingsPolicy.selectSubtitleTrack(
            state = initial,
            tracks = tracks(),
            trackId = "en",
        )

        assertEquals("en", selected.selectedSubtitleTrackId)
        assertEquals("en", selected.rememberedSubtitleTrackId)
        assertTrue(selected.subtitleSettings.enabled)
    }

    @Test
    fun `track refresh drops invalid memory and resolves configured fallback`() {
        val stale = PlayerSessionSettingsState(
            subtitleSettings = SubtitleSettings(languagePreference = "en"),
            selectedSubtitleTrackId = "removed",
            rememberedSubtitleTrackId = "removed",
            danmakuSettings = DanmakuSettings(),
        )

        val refreshed = PlayerSessionSettingsPolicy.refreshTracks(stale, tracks())

        assertEquals("en", refreshed.selectedSubtitleTrackId)
        assertEquals("en", refreshed.rememberedSubtitleTrackId)
    }

    @Test
    fun `empty refresh clears renderer selection but preserves remembered track`() {
        val initial = PlayerSessionSettingsPolicy.initial(
            tracks = tracks(),
            subtitleSettings = SubtitleSettings(languagePreference = "zh-cn"),
            danmakuSettings = DanmakuSettings(),
        )

        val empty = PlayerSessionSettingsPolicy.refreshTracks(initial, emptyList())
        val restored = PlayerSessionSettingsPolicy.refreshTracks(empty, tracks())

        assertNull(empty.selectedSubtitleTrackId)
        assertEquals("zh", empty.rememberedSubtitleTrackId)
        assertEquals("zh", restored.selectedSubtitleTrackId)
    }

    @Test
    fun `danmaku toggle preserves every non-master setting`() {
        val initial = PlayerSessionSettingsPolicy.initial(
            tracks = tracks(),
            subtitleSettings = SubtitleSettings(),
            danmakuSettings = DanmakuSettings(
                enabled = true,
                opacityPercent = 50,
                blockColored = true,
            ),
        )

        val toggled = PlayerSessionSettingsPolicy.toggleDanmakus(initial)

        assertFalse(toggled.danmakuSettings.enabled)
        assertEquals(50, toggled.danmakuSettings.opacityPercent)
        assertTrue(toggled.danmakuSettings.blockColored)
    }

    private fun tracks() = listOf(
        SubtitleTrack("first", "繁体中文", "/first.vtt", "zh-TW"),
        SubtitleTrack("zh", "CHS 简体", "/zh.vtt", "zh-CN"),
        SubtitleTrack("en", "English", "/en.vtt", "en"),
    )
}
