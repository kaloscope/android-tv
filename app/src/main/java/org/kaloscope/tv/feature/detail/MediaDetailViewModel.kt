package org.kaloscope.tv.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.media.MediaRepository

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    repository: MediaRepository,
) : ViewModel() {
    private val coordinator = MediaDetailCoordinator(repository)
    private var currentMediaId: Long? = null
    private var currentSession: Session? = null
    private var requestJob: Job? = null
    private var childDetailJob: Job? = null

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
        currentSession = session
        startParentRequest(session, mediaId)
    }

    fun rememberFocusedChild(childId: Long) {
        val detailLoadRequired = coordinator.rememberFocusedChild(childId)
        childDetailJob?.cancel()
        childDetailJob = null
        if (detailLoadRequired) {
            currentSession?.let { session -> scheduleChildDetailLoad(session, childId) }
        }
    }

    fun rememberChildViewport(snapshot: GridViewportSnapshot) =
        coordinator.rememberChildViewport(snapshot)

    fun retry(session: Session) {
        val mediaId = currentMediaId ?: return
        currentSession = session
        startParentRequest(session, mediaId)
    }

    fun reset() {
        requestJob?.cancel()
        childDetailJob?.cancel()
        requestJob = null
        childDetailJob = null
        currentMediaId = null
        currentSession = null
        coordinator.reset()
    }

    private fun startParentRequest(
        session: Session,
        mediaId: Long,
    ) {
        requestJob?.cancel()
        childDetailJob?.cancel()
        childDetailJob = null
        requestJob = viewModelScope.launch {
            coordinator.load(session, mediaId)
            val childId = (coordinator.state.value as? MediaDetailUiState.Content)
                ?.focusedChildId
                ?: return@launch
            scheduleChildDetailLoad(session, childId)
        }
    }

    private fun scheduleChildDetailLoad(
        session: Session,
        childId: Long,
    ) {
        childDetailJob?.cancel()
        childDetailJob = viewModelScope.launch {
            delay(ChildDetailFocusDebounceMillis)
            coordinator.loadFocusedChild(session, childId)
        }
    }
}

internal const val ChildDetailFocusDebounceMillis = 180L
