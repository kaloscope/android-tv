package org.kaloscope.tv.feature.detail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.media.MediaRepository

sealed interface MediaDetailUiState {
    data object Loading : MediaDetailUiState

    data class Content(
        val parent: MediaDetail,
        val selectedChild: MediaDetail? = null,
        val loadingChildId: Long? = null,
        val childError: AppError? = null,
    ) : MediaDetailUiState

    data class Error(
        val error: AppError,
    ) : MediaDetailUiState
}

class MediaDetailCoordinator(
    private val repository: MediaRepository,
) {
    private val mutableState =
        MutableStateFlow<MediaDetailUiState>(MediaDetailUiState.Loading)

    val state: StateFlow<MediaDetailUiState> = mutableState.asStateFlow()

    fun reset() {
        mutableState.value = MediaDetailUiState.Loading
    }

    suspend fun load(
        session: Session,
        mediaId: Long,
    ) {
        mutableState.value = MediaDetailUiState.Loading
        mutableState.value = when (val result = repository.getMediaDetail(session, mediaId)) {
            is AppResult.Success -> MediaDetailUiState.Content(result.value)
            is AppResult.Failure -> MediaDetailUiState.Error(result.error)
        }
    }

    suspend fun selectChild(
        session: Session,
        childId: Long,
    ) {
        val content = mutableState.value as? MediaDetailUiState.Content ?: return
        if (content.parent.children.none { it.id == childId }) {
            return
        }
        mutableState.value = content.copy(
            loadingChildId = childId,
            childError = null,
        )
        mutableState.value = when (val result = repository.getMediaDetail(session, childId)) {
            is AppResult.Success -> content.copy(selectedChild = result.value)
            is AppResult.Failure -> content.copy(childError = result.error)
        }
    }
}
