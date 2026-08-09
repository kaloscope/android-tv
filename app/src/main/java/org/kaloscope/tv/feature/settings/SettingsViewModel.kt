package org.kaloscope.tv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeQuality
import org.kaloscope.tv.data.server.ServerRepository
import org.kaloscope.tv.data.settings.SettingsRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    serverRepository: ServerRepository,
) : ViewModel() {
    private val coordinator = SettingsCoordinator(settingsRepository, serverRepository)
    private var settingsJob: Job? = null
    private var connectionJob: Job? = null

    val uiState: StateFlow<SettingsUiState> = coordinator.state

    init {
        load()
    }

    fun load() = launchSettingsOperation { coordinator.load() }

    fun selectSection(section: SettingsSection) = coordinator.selectSection(section)

    fun setPlaybackMode(value: PlaybackMode) =
        launchSettingsOperation { coordinator.setPlaybackMode(value) }

    fun setTranscodeQuality(value: TranscodeQuality) =
        launchSettingsOperation { coordinator.setTranscodeQuality(value) }

    fun setAutoplayNext(value: Boolean) =
        launchSettingsOperation { coordinator.setAutoplayNext(value) }

    fun setAccentColor(value: AccentColor) =
        launchSettingsOperation { coordinator.setAccentColor(value) }

    fun setDanmakuSettings(value: DanmakuSettings) =
        launchSettingsOperation { coordinator.setDanmakuSettings(value) }

    fun setSubtitleSettings(value: SubtitleSettings) =
        launchSettingsOperation { coordinator.setSubtitleSettings(value) }

    fun setStartPage(value: StartPage) =
        launchSettingsOperation { coordinator.setStartPage(value) }

    fun setReaderChapterOrder(value: ReaderChapterOrder) =
        launchSettingsOperation { coordinator.setReaderChapterOrder(value) }

    fun setImageReaderSettings(value: ImageReaderSettings) =
        launchSettingsOperation { coordinator.setImageReaderSettings(value) }

    fun setTextReaderSettings(value: TextReaderSettings) =
        launchSettingsOperation { coordinator.setTextReaderSettings(value) }

    fun testConnection(session: Session) {
        if (connectionJob?.isActive == true) {
            return
        }
        connectionJob = viewModelScope.launch {
            coordinator.testConnection(session)
        }
    }

    private fun launchSettingsOperation(block: suspend () -> Unit) {
        // Serialize writes so a repeated remote key cannot strand an interrupted saving state.
        if (settingsJob?.isActive == true) {
            return
        }
        settingsJob = viewModelScope.launch { block() }
    }
}
