package org.kaloscope.tv.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleSettingsPolicy
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeResolution

@Singleton
class PreferencesSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override suspend fun getSettings(): AppResult<TvSettings> =
        localCall("settings_read") {
            dataStore.data.first().toSettings()
        }

    override suspend fun saveSettings(settings: TvSettings): AppResult<TvSettings> =
        localCall("settings_write") {
            dataStore.edit { preferences ->
                preferences[ACCENT_COLOR] = settings.accentColor.storedValue
                preferences[START_PAGE] = settings.startPage.storedValue
                preferences[PLAYBACK_MODE] = settings.playbackMode.storedValue
                preferences[TRANSCODE_RESOLUTION] = settings.transcodeResolution.queryValue
                preferences[AUTOPLAY_NEXT] = settings.autoplayNext
                preferences[DANMAKU_ENABLED] = settings.danmaku.enabled
                preferences[DANMAKU_TEXT_SIZE] = settings.danmaku.textSize.storedValue
                preferences[DANMAKU_SPEED] = settings.danmaku.speed.storedValue
                preferences[DANMAKU_OPACITY] = settings.danmaku.opacityPercent
                preferences[DANMAKU_DISPLAY_AREA] = settings.danmaku.displayAreaPercent
                preferences[DANMAKU_SCROLL_VISIBLE] =
                    DanmakuDisplayMode.Scroll in settings.danmaku.visibleModes
                preferences[DANMAKU_TOP_VISIBLE] =
                    DanmakuDisplayMode.Top in settings.danmaku.visibleModes
                preferences[DANMAKU_BOTTOM_VISIBLE] =
                    DanmakuDisplayMode.Bottom in settings.danmaku.visibleModes
                preferences[DANMAKU_COLORED_BLOCKED] = settings.danmaku.blockColored
                val subtitle = SubtitleSettingsPolicy.sanitize(settings.subtitle)
                preferences[SUBTITLE_ENABLED] = subtitle.enabled
                preferences[SUBTITLE_LANGUAGE_PREFERENCE] = subtitle.languagePreference
                preferences[SUBTITLE_DISPLAY_MODE] = subtitle.displayMode.storedValue
                preferences[SUBTITLE_TIME_OFFSET_TENTHS] =
                    (subtitle.timeOffsetSeconds * 10).toInt()
                preferences[SUBTITLE_FONT_SCALE] = subtitle.fontScalePercent
                preferences[SUBTITLE_VERTICAL_POSITION] = subtitle.verticalPositionPercent
            }
            settings
        }

    private fun Preferences.toSettings(): TvSettings =
        TvSettings(
            accentColor = enumValue(
                stored = this[ACCENT_COLOR],
                fallback = AccentColor.Blue,
            ),
            startPage = StartPage.entries.firstOrNull {
                it.storedValue == this[START_PAGE]
            } ?: StartPage.Home,
            playbackMode = PlaybackMode.entries.firstOrNull {
                it.storedValue == this[PLAYBACK_MODE]
            } ?: PlaybackMode.Auto,
            transcodeResolution = TranscodeResolution.entries.firstOrNull {
                it.queryValue == this[TRANSCODE_RESOLUTION]
            } ?: TranscodeResolution.P1080,
            autoplayNext = this[AUTOPLAY_NEXT] ?: true,
            danmaku = DanmakuSettings(
                enabled = this[DANMAKU_ENABLED] ?: true,
                textSize = enumValue(
                    stored = this[DANMAKU_TEXT_SIZE],
                    fallback = DanmakuTextSize.Medium,
                ),
                speed = enumValue(
                    stored = this[DANMAKU_SPEED],
                    fallback = DanmakuSpeed.Standard,
                ),
                opacityPercent = this[DANMAKU_OPACITY].validPercent(100),
                displayAreaPercent = this[DANMAKU_DISPLAY_AREA].validPercent(75),
                visibleModes = buildSet {
                    if (this@toSettings[DANMAKU_SCROLL_VISIBLE] ?: true) {
                        add(DanmakuDisplayMode.Scroll)
                    }
                    if (this@toSettings[DANMAKU_TOP_VISIBLE] ?: true) {
                        add(DanmakuDisplayMode.Top)
                    }
                    if (this@toSettings[DANMAKU_BOTTOM_VISIBLE] ?: true) {
                        add(DanmakuDisplayMode.Bottom)
                    }
                },
                blockColored = this[DANMAKU_COLORED_BLOCKED] ?: false,
            ),
            subtitle = SubtitleSettingsPolicy.sanitize(
                SubtitleSettings(
                    enabled = this[SUBTITLE_ENABLED] ?: true,
                    languagePreference = this[SUBTITLE_LANGUAGE_PREFERENCE].orEmpty(),
                    displayMode = enumValue(
                        stored = this[SUBTITLE_DISPLAY_MODE],
                        fallback = SubtitleDisplayMode.Stroke,
                    ),
                    timeOffsetSeconds = (this[SUBTITLE_TIME_OFFSET_TENTHS] ?: 0) / 10f,
                    fontScalePercent = this[SUBTITLE_FONT_SCALE] ?: 100,
                    verticalPositionPercent = this[SUBTITLE_VERTICAL_POSITION] ?: 2,
                ),
            ),
        )

    private suspend fun <T> localCall(
        context: String,
        block: suspend () -> T,
    ): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            AppResult.Failure(AppError.InvalidData(context))
        }

    private val Enum<*>.storedValue: String
        get() = name.lowercase()

    private inline fun <reified T : Enum<T>> enumValue(
        stored: String?,
        fallback: T,
    ): T = enumValues<T>().firstOrNull { it.name.equals(stored, ignoreCase = true) }
        ?: fallback

    private fun Int?.validPercent(fallback: Int): Int =
        this?.takeIf(ALLOWED_PERCENTAGES::contains) ?: fallback

    private companion object {
        val ALLOWED_PERCENTAGES = setOf(25, 50, 75, 100)
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val START_PAGE = stringPreferencesKey("start_page")
        val PLAYBACK_MODE = stringPreferencesKey("playback_mode")
        val TRANSCODE_RESOLUTION = stringPreferencesKey("transcode_resolution")
        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
        val DANMAKU_ENABLED = booleanPreferencesKey("danmaku_enabled")
        val DANMAKU_TEXT_SIZE = stringPreferencesKey("danmaku_text_size")
        val DANMAKU_SPEED = stringPreferencesKey("danmaku_speed")
        val DANMAKU_OPACITY = intPreferencesKey("danmaku_opacity")
        val DANMAKU_DISPLAY_AREA = intPreferencesKey("danmaku_display_area")
        val DANMAKU_SCROLL_VISIBLE = booleanPreferencesKey("danmaku_scroll_visible")
        val DANMAKU_TOP_VISIBLE = booleanPreferencesKey("danmaku_top_visible")
        val DANMAKU_BOTTOM_VISIBLE = booleanPreferencesKey("danmaku_bottom_visible")
        val DANMAKU_COLORED_BLOCKED = booleanPreferencesKey("danmaku_colored_blocked")
        val SUBTITLE_ENABLED = booleanPreferencesKey("subtitle_enabled")
        val SUBTITLE_LANGUAGE_PREFERENCE =
            stringPreferencesKey("subtitle_language_preference")
        val SUBTITLE_DISPLAY_MODE = stringPreferencesKey("subtitle_display_mode")
        val SUBTITLE_TIME_OFFSET_TENTHS =
            intPreferencesKey("subtitle_time_offset_tenths")
        val SUBTITLE_FONT_SCALE = intPreferencesKey("subtitle_font_scale")
        val SUBTITLE_VERTICAL_POSITION =
            intPreferencesKey("subtitle_vertical_position")
    }
}
