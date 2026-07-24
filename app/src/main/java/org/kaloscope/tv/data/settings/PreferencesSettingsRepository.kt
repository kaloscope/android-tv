package org.kaloscope.tv.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
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
                preferences[DANMAKU_ENABLED] = settings.danmakuEnabled
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
            danmakuEnabled = this[DANMAKU_ENABLED] ?: true,
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

    private companion object {
        val START_PAGE = stringPreferencesKey("start_page")
        val PLAYBACK_MODE = stringPreferencesKey("playback_mode")
        val TRANSCODE_RESOLUTION = stringPreferencesKey("transcode_resolution")
        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
        val DANMAKU_ENABLED = booleanPreferencesKey("danmaku_enabled")
        val SUBTITLE_ENABLED = booleanPreferencesKey("subtitle_enabled")
    }
}
