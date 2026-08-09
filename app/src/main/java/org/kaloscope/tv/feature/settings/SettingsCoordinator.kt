package org.kaloscope.tv.feature.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderSettingsPolicy
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleSettingsPolicy
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeQuality
import org.kaloscope.tv.data.server.ServerRepository
import org.kaloscope.tv.data.settings.SettingsRepository

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Error(val error: AppError) : SettingsUiState

    data class Content(
        val settings: TvSettings,
        val section: SettingsSection = SettingsSection.Playback,
        val isSaving: Boolean = false,
        val saveError: AppError? = null,
        val connection: SettingsConnection = SettingsConnection.Idle,
    ) : SettingsUiState
}

enum class SettingsSection {
    Playback,
    Danmaku,
    Subtitle,
    Reading,
    Behavior,
    ServerAccount,
}

sealed interface SettingsConnection {
    data object Idle : SettingsConnection

    data object Testing : SettingsConnection

    data class Success(val version: String) : SettingsConnection

    data class Failure(val error: AppError) : SettingsConnection
}

class SettingsCoordinator(
    private val settingsRepository: SettingsRepository,
    private val serverRepository: ServerRepository,
) {
    private val mutableState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    suspend fun load() {
        mutableState.value = SettingsUiState.Loading
        mutableState.value = when (val result = settingsRepository.getSettings()) {
            is AppResult.Success -> SettingsUiState.Content(result.value)
            is AppResult.Failure -> SettingsUiState.Error(result.error)
        }
    }

    fun selectSection(section: SettingsSection) {
        val content = mutableState.value as? SettingsUiState.Content ?: return
        mutableState.value = content.copy(section = section)
    }

    suspend fun setPlaybackMode(value: PlaybackMode) =
        update { copy(playbackMode = value) }

    suspend fun setTranscodeQuality(value: TranscodeQuality) =
        update { copy(transcodeQuality = value) }

    suspend fun setAutoplayNext(value: Boolean) =
        update { copy(autoplayNext = value) }

    suspend fun setAccentColor(value: AccentColor) =
        update { copy(accentColor = value) }

    suspend fun setDanmakuSettings(value: DanmakuSettings) =
        update { copy(danmaku = value) }

    suspend fun setSubtitleSettings(value: SubtitleSettings) =
        update { copy(subtitle = SubtitleSettingsPolicy.sanitize(value)) }

    suspend fun setStartPage(value: StartPage) =
        update { copy(startPage = value) }

    suspend fun setReaderChapterOrder(value: ReaderChapterOrder) =
        update { copy(readerChapterOrder = value) }

    suspend fun setImageReaderSettings(value: ImageReaderSettings) =
        update { copy(imageReader = value) }

    suspend fun setTextReaderSettings(value: TextReaderSettings) =
        update { copy(textReader = ReaderSettingsPolicy.sanitize(value)) }

    suspend fun testConnection(session: Session) {
        val content = mutableState.value as? SettingsUiState.Content ?: return
        if (content.connection == SettingsConnection.Testing) {
            return
        }
        mutableState.value = content.copy(connection = SettingsConnection.Testing)
        when (val result = serverRepository.testConnection(session.server.origin)) {
            is AppResult.Success -> updateContent {
                copy(connection = SettingsConnection.Success(result.value))
            }

            is AppResult.Failure -> updateContent {
                copy(connection = SettingsConnection.Failure(result.error))
            }
        }
    }

    private suspend fun update(transform: TvSettings.() -> TvSettings) {
        val content = mutableState.value as? SettingsUiState.Content ?: return
        if (content.isSaving) {
            return
        }
        val updated = content.settings.transform()
        mutableState.value = content.copy(isSaving = true, saveError = null)
        when (val result = settingsRepository.saveSettings(updated)) {
            is AppResult.Success -> updateContent {
                copy(settings = result.value, isSaving = false)
            }

            is AppResult.Failure -> updateContent {
                copy(isSaving = false, saveError = result.error)
            }
        }
    }

    private inline fun updateContent(
        transform: SettingsUiState.Content.() -> SettingsUiState.Content,
    ) {
        val content = mutableState.value as? SettingsUiState.Content ?: return
        mutableState.value = content.transform()
    }
}
