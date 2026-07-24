package org.kaloscope.tv.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.media.MediaRepository

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    repository: MediaRepository,
) : ViewModel() {
    private val coordinator = MediaDetailCoordinator(repository)
    private var currentMediaId: Long? = null
    private var requestJob: Job? = null

    val uiState: StateFlow<MediaDetailUiState> = coordinator.state

    fun load(
        session: Session,
        mediaId: Long,
        force: Boolean = false,
    ) {
        if (!force && currentMediaId == mediaId) {
            return
        }
        currentMediaId = mediaId
        startRequest { coordinator.load(session, mediaId) }
    }

    fun selectChild(
        session: Session,
        childId: Long,
    ) {
        startRequest { coordinator.selectChild(session, childId) }
    }

    fun retry(session: Session) {
        val mediaId = currentMediaId ?: return
        startRequest { coordinator.load(session, mediaId) }
    }

    fun reset() {
        requestJob?.cancel()
        requestJob = null
        currentMediaId = null
        coordinator.reset()
    }

    private fun startRequest(block: suspend () -> Unit) {
        requestJob?.cancel()
        requestJob = viewModelScope.launch { block() }
    }
}
