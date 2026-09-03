package org.kaloscope.tv.data.settings

import androidx.compose.ui.unit.Density
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.designsystem.toDpDimensions
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize
import org.kaloscope.tv.core.model.ImagePageDirection
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ImageZoomMode
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.TextReaderFont
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.TextReaderTheme
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeQuality

class PreferencesSettingsRepositoryTest {
    @Test
    fun `missing preferences return product defaults`() = runTest {
        val repository = repository(this)

        val result = repository.getSettings()

        assertEquals(TvSettings(), (result as AppResult.Success).value)
        assertEquals(AccentColor.Blue, result.value.accentColor)
        assertEquals(
            DanmakuSettings(
                enabled = true,
                textSize = DanmakuTextSize.Medium,
                speed = DanmakuSpeed.Standard,
                opacityPercent = 75,
                displayAreaPercent = 50,
                visibleModes = DanmakuDisplayMode.entries.toSet(),
                mergeDuplicates = true,
            ),
            result.value.danmaku,
        )
        assertEquals(false, result.value.danmaku.blockColored)
    }

    @Test
    fun `every accent color survives repository recreation`() = runTest {
        for (accentColor in AccentColor.entries) {
            val store = dataStore(this, temporaryFile())
            val expected = TvSettings(accentColor = accentColor)

            PreferencesSettingsRepository(store).saveSettings(expected)
            val restored = PreferencesSettingsRepository(store).getSettings()

            assertEquals(expected, (restored as AppResult.Success).value)
        }
    }

    @Test
    fun `accent color parsing is case insensitive`() = runTest {
        val store = dataStore(this)
        store.edit { preferences ->
            preferences[stringPreferencesKey("accent_color")] = "PuRpLe"
        }

        val result = PreferencesSettingsRepository(store).getSettings()

        assertEquals(
            AccentColor.Purple,
            (result as AppResult.Success).value.accentColor,
        )
    }

    @Test
    fun `invalid accent color falls back to blue`() = runTest {
        val store = dataStore(this)
        store.edit { preferences ->
            preferences[stringPreferencesKey("accent_color")] = "ultraviolet"
        }

        val result = PreferencesSettingsRepository(store).getSettings()

        assertEquals(
            AccentColor.Blue,
            (result as AppResult.Success).value.accentColor,
        )
    }

    @Test
    fun `saved settings survive repository recreation`() = runTest {
        val dataStore = dataStore(this)
        val first = PreferencesSettingsRepository(dataStore)
        val expected = TvSettings(
            startPage = StartPage.Library,
            playbackMode = PlaybackMode.Transcode,
            transcodeQuality = TranscodeQuality.High,
            autoplayNext = false,
            danmaku = DanmakuSettings(enabled = false),
            subtitle = SubtitleSettings(
                enabled = false,
                languagePreference = "chs|zh-CN",
                displayMode = SubtitleDisplayMode.Background,
                fontScalePercent = 125,
                verticalPositionPercent = 8,
            ),
            readerChapterOrder = ReaderChapterOrder.Descending,
            imageReader = ImageReaderSettings(
                readMode = ImageReadMode.Paged,
                zoomMode = ImageZoomMode.FitHeight,
                pageDirection = ImagePageDirection.Down,
            ),
            textReader = TextReaderSettings(
                theme = TextReaderTheme.Slate,
                font = TextReaderFont.Monospace,
                fontSizeSp = 40,
                lineHeight = 2.6f,
                paragraphSpacingDp = 60,
                horizontalPaddingDp = 84,
            ),
        )

        assertEquals(AppResult.Success(expected), first.saveSettings(expected))
        val restored = PreferencesSettingsRepository(dataStore).getSettings()

        assertEquals(expected, (restored as AppResult.Success).value)
    }

    @Test
    fun `subtitle time offset is not restored as a global setting`() = runTest {
        val store = dataStore(this)
        PreferencesSettingsRepository(store).saveSettings(
            TvSettings(
                subtitle = SubtitleSettings(timeOffsetSeconds = -0.5f),
            ),
        )

        val restored = PreferencesSettingsRepository(store).getSettings()

        assertEquals(
            0f,
            (restored as AppResult.Success).value.subtitle.timeOffsetSeconds,
            0f,
        )
    }

    @Test
    fun `legacy subtitle time offset is ignored`() = runTest {
        val store = dataStore(this)
        store.edit { preferences ->
            preferences[intPreferencesKey("subtitle_time_offset_tenths")] = -5
        }

        val restored = PreferencesSettingsRepository(store).getSettings()

        assertEquals(
            0f,
            (restored as AppResult.Success).value.subtitle.timeOffsetSeconds,
            0f,
        )
    }

    @Test
    fun `legacy paragraph spacing stays fixed when font size changes`() = runTest {
        val store = dataStore(this)
        store.edit { preferences ->
            preferences[intPreferencesKey("text_reader_font_size_sp")] = 32
            preferences[intPreferencesKey("text_reader_paragraph_spacing_halves")] = 2
        }

        val result = PreferencesSettingsRepository(store).getSettings()
        val settings = (result as AppResult.Success).value.textReader
        val density = Density(density = 1f, fontScale = 1f)

        assertEquals(32f, settings.toDpDimensions(density).paragraphSpacing.value, 0f)
        assertEquals(
            32f,
            settings.copy(fontSizeSp = 40).toDpDimensions(density).paragraphSpacing.value,
            0f,
        )
    }

    @Test
    fun `invalid transcode quality falls back to medium`() = runTest {
        val store = dataStore(this)
        store.edit { preferences ->
            preferences[stringPreferencesKey("transcode_quality")] = "ultra"
        }

        val result = PreferencesSettingsRepository(store).getSettings()

        assertEquals(
            TranscodeQuality.Medium,
            (result as AppResult.Success).value.transcodeQuality,
        )
    }

    @Test
    fun `invalid reader values fall back independently`() = runTest {
        val store = dataStore(this)
        store.edit { preferences ->
            preferences[stringPreferencesKey("reader_chapter_order")] = "random"
            preferences[stringPreferencesKey("image_reader_read_mode")] = "book"
            preferences[stringPreferencesKey("image_reader_zoom_mode")] = "stretch"
            preferences[stringPreferencesKey("image_reader_page_direction")] = "diagonal"
            preferences[stringPreferencesKey("text_reader_theme")] = "rainbow"
            preferences[stringPreferencesKey("text_reader_font")] = "comic"
            preferences[intPreferencesKey("text_reader_font_size_sp")] = 31
            preferences[intPreferencesKey("text_reader_line_height_tenths")] = 17
            preferences[intPreferencesKey("text_reader_paragraph_spacing_halves")] = 7
            preferences[intPreferencesKey("text_reader_horizontal_padding_dp")] = 49
        }

        val result = PreferencesSettingsRepository(store).getSettings()
        val settings = (result as AppResult.Success).value

        assertEquals(ReaderChapterOrder.Ascending, settings.readerChapterOrder)
        assertEquals(ImageReaderSettings(), settings.imageReader)
        assertEquals(TextReaderSettings(), settings.textReader)
    }

    @Test
    fun `danmaku settings survive repository recreation`() = runTest {
        val store = dataStore(this)
        val expected = DanmakuSettings(
            enabled = false,
            textSize = DanmakuTextSize.ExtraLarge,
            speed = DanmakuSpeed.Fast,
            opacityPercent = 50,
            displayAreaPercent = 25,
            visibleModes = setOf(
                DanmakuDisplayMode.Scroll,
                DanmakuDisplayMode.Top,
            ),
            blockColored = true,
            mergeDuplicates = false,
        )

        PreferencesSettingsRepository(store).saveSettings(
            TvSettings(danmaku = expected),
        )
        val restored = PreferencesSettingsRepository(store).getSettings()

        assertEquals(expected, (restored as AppResult.Success).value.danmaku)
    }

    @Test
    fun `invalid danmaku values fall back independently`() = runTest {
        val store = dataStore(this)
        store.edit { preferences ->
            preferences[stringPreferencesKey("danmaku_text_size")] = "small"
            preferences[stringPreferencesKey("danmaku_speed")] = "warp"
            preferences[intPreferencesKey("danmaku_opacity")] = 41
            preferences[intPreferencesKey("danmaku_display_area")] = 0
        }

        val result = PreferencesSettingsRepository(store).getSettings()
        val danmaku = (result as AppResult.Success).value.danmaku

        assertEquals(DanmakuTextSize.Small, danmaku.textSize)
        assertEquals(DanmakuSpeed.Standard, danmaku.speed)
        assertEquals(75, danmaku.opacityPercent)
        assertEquals(50, danmaku.displayAreaPercent)
    }

    private fun repository(
        scope: TestScope,
    ) = PreferencesSettingsRepository(dataStore(scope))

    private fun dataStore(
        scope: TestScope,
        file: File = temporaryFile(),
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope.backgroundScope,
            produceFile = { file },
        )

    private fun temporaryFile(): File =
        File.createTempFile("kaloscope-settings-", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
}
