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
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize
import org.kaloscope.tv.core.model.StartPage
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
                preferences[SUBTITLE_ENABLED] = settings.subtitleEnabled
            }
            settings
        }

    private fun Preferences.toSettings(): TvSettings =
        TvSettings(
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
            ),
            subtitleEnabled = this[SUBTITLE_ENABLED] ?: true,
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

    private val StartPage.storedValue: String
        get() = name.lowercase()

    private val PlaybackMode.storedValue: String
        get() = name.lowercase()

    private val DanmakuTextSize.storedValue: String
        get() = name.lowercase()

    private val DanmakuSpeed.storedValue: String
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
        val SUBTITLE_ENABLED = booleanPreferencesKey("subtitle_enabled")
    }
}
