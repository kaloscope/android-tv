package org.kaloscope.tv.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.history.HistoryRepository
import org.kaloscope.tv.feature.home.HomeCoordinator
import org.kaloscope.tv.feature.home.HomeUiState

@HiltViewModel
class MainViewModel @Inject constructor(
    historyRepository: HistoryRepository,
) : ViewModel() {
    private val homeCoordinator = HomeCoordinator(historyRepository)
    private var loadedServerId: String? = null
    private var loadJob: Job? = null

    val homeState: StateFlow<HomeUiState> = homeCoordinator.state

    fun loadHome(session: Session, force: Boolean = false) {
        if (!force && loadedServerId == session.server.id) {
            return
        }
        loadedServerId = session.server.id
        // History and credentials are origin-scoped, so stale requests must not win.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            homeCoordinator.load(session)
        }
    }

    fun reset() {
        loadJob?.cancel()
        loadJob = null
        loadedServerId = null
        homeCoordinator.reset()
    }
}
