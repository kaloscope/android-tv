package org.kaloscope.tv.feature.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.data.history.HistoryRepository

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val items: List<WatchHistoryItem>,
        val refreshError: AppError? = null,
    ) : HomeUiState

    data object Empty : HomeUiState

    data class Error(
        val error: AppError,
    ) : HomeUiState
}

class HomeCoordinator(
    private val repository: HistoryRepository,
) {
    private val mutableState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)

    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    fun reset() {
        mutableState.value = HomeUiState.Loading
    }

    suspend fun load(session: Session) {
        val retainedContent = mutableState.value as? HomeUiState.Content
        if (retainedContent == null) {
            mutableState.value = HomeUiState.Loading
        } else {
            mutableState.value = retainedContent.copy(refreshError = null)
        }
        mutableState.value = when (val result = repository.getRecentVideos(session)) {
            is AppResult.Success -> {
                if (result.value.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Content(result.value)
                }
            }

            is AppResult.Failure -> retainedContent?.copy(refreshError = result.error)
                ?: HomeUiState.Error(result.error)
        }
    }
}
