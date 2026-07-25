package org.kaloscope.tv.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.data.search.SearchRepository

@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: SearchRepository,
    requestStore: PlaybackRequestStore,
) : ViewModel() {
    private val coordinator = SearchCoordinator(repository, requestStore)
    private var loadedServerId: String? = null
    private var requestJob: Job? = null

    val uiState: StateFlow<SearchUiState> = coordinator.state

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

    fun selectIndexer(
        session: Session,
        indexerId: Long,
    ) = startRequest { coordinator.selectIndexer(session, indexerId) }

    fun search(session: Session) = startRequest { coordinator.search(session) }

    fun retry(session: Session) = startRequest { coordinator.retry(session) }

    fun loadNext(session: Session) = startRequest { coordinator.loadNext(session) }

    fun openFilters() = coordinator.openFilters()

    fun dismissFilters() = coordinator.dismissFilters()

    fun applyFilters(
        session: Session,
        values: Map<String, SearchFilterValue>,
    ) = startRequest { coordinator.applyFilters(session, values) }

    fun clearFilters(session: Session) =
        startRequest { coordinator.clearFilters(session) }

    fun rememberFocusedResult(resultId: String) =
        coordinator.rememberFocusedResult(resultId)

    fun play(
        session: Session,
        resultId: String,
        settings: TvSettings = TvSettings(),
    ) = startRequest { coordinator.play(session, resultId, settings) }

    fun consumePlaybackRequest(requestId: String) =
        coordinator.consumePlaybackRequest(requestId)

    fun reset() {
        requestJob?.cancel()
        requestJob = null
        loadedServerId = null
        coordinator.reset()
    }

    private fun startRequest(block: suspend () -> Unit) {
        // Source changes cancel origin-scoped work before stale results can replace new state.
        requestJob?.cancel()
        requestJob = viewModelScope.launch { block() }
    }
}
