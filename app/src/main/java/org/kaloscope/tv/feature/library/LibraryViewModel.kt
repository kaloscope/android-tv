package org.kaloscope.tv.feature.library

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
class LibraryViewModel @Inject constructor(
    repository: MediaRepository,
) : ViewModel() {
    private val coordinator = LibraryCoordinator(repository)
    private var loadedServerId: String? = null
    private var requestJob: Job? = null

    val uiState: StateFlow<LibraryUiState> = coordinator.state

    fun load(
        session: Session,
        force: Boolean = false,
    ) {
        if (!force && loadedServerId == session.server.id) {
            return
        }
        loadedServerId = session.server.id
        startRequest { coordinator.load(session) }
    }

    fun updateQuery(value: String) = coordinator.updateQuery(value)

    fun search(session: Session) {
        startRequest { coordinator.search(session) }
    }

    fun selectLibrary(
        session: Session,
        libraryId: Long,
    ) {
        startRequest { coordinator.selectLibrary(session, libraryId) }
    }

    fun retryContent(session: Session) {
        startRequest { coordinator.retryContent(session) }
    }

    fun loadNext(session: Session) {
        startRequest { coordinator.loadNext(session) }
    }

    fun rememberFocusedMedia(mediaId: Long) =
        coordinator.rememberFocusedMedia(mediaId)

    fun reset() {
        requestJob?.cancel()
        requestJob = null
        loadedServerId = null
        coordinator.reset()
    }

    private fun startRequest(block: suspend () -> Unit) {
        // Source changes cancel origin-scoped work before it can overwrite newer state.
        requestJob?.cancel()
        requestJob = viewModelScope.launch { block() }
    }
}
