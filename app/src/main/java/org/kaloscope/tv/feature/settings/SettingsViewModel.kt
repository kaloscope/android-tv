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
    private var loadJob: Job? = null
    private var connectionJob: Job? = null

    val uiState: StateFlow<SettingsUiState> = coordinator.state

    init {
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) {
            return
        }
        loadJob = viewModelScope.launch { coordinator.load() }
    }

    fun selectSection(section: SettingsSection) = coordinator.selectSection(section)

    fun setPlaybackMode(value: PlaybackMode) =
        enqueueSettingsUpdate { coordinator.setPlaybackMode(value) }

    fun setTranscodeQuality(value: TranscodeQuality) =
        enqueueSettingsUpdate { coordinator.setTranscodeQuality(value) }

    fun setAutoplayNext(value: Boolean) =
        enqueueSettingsUpdate { coordinator.setAutoplayNext(value) }

    fun setAccentColor(value: AccentColor) =
        enqueueSettingsUpdate { coordinator.setAccentColor(value) }

    fun setDanmakuSettings(value: DanmakuSettings) =
        enqueueSettingsUpdate { coordinator.setDanmakuSettings(value) }

    fun setPlayerDanmakuPreferences(value: DanmakuSettings) =
        enqueueSettingsUpdate { coordinator.setPlayerDanmakuPreferences(value) }

    fun setSubtitleSettings(value: SubtitleSettings) =
        enqueueSettingsUpdate { coordinator.setSubtitleSettings(value) }

    fun setPlayerSubtitlePreferences(value: SubtitleSettings) =
        enqueueSettingsUpdate { coordinator.setPlayerSubtitlePreferences(value) }

    fun setStartPage(value: StartPage) =
        enqueueSettingsUpdate { coordinator.setStartPage(value) }

    fun setReaderChapterOrder(value: ReaderChapterOrder) =
        enqueueSettingsUpdate { coordinator.setReaderChapterOrder(value) }

    fun setImageReaderSettings(value: ImageReaderSettings) =
        enqueueSettingsUpdate { coordinator.setImageReaderSettings(value) }

    fun setTextReaderSettings(value: TextReaderSettings) =
        enqueueSettingsUpdate { coordinator.setTextReaderSettings(value) }

    fun testConnection(session: Session) {
        if (connectionJob?.isActive == true) {
            return
        }
        connectionJob = viewModelScope.launch {
            coordinator.testConnection(session)
        }
    }

    private fun enqueueSettingsUpdate(block: suspend () -> Unit) {
        // The coordinator serializes persistence; every input must still update its latest snapshot.
        viewModelScope.launch { block() }
    }
}
