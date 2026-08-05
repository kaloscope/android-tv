package org.kaloscope.tv.feature.detail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.media.MediaRepository

sealed interface MediaDetailUiState {
    data object Loading : MediaDetailUiState

    data class Content(
        val parent: MediaDetail,
        val focusedChildId: Long? = null,
        val childViewport: GridViewportSnapshot = GridViewportSnapshot.Top,
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
            is AppResult.Success -> MediaDetailUiState.Content(
                parent = result.value,
                focusedChildId = result.value.children.firstOrNull()?.id,
            )
            is AppResult.Failure -> MediaDetailUiState.Error(result.error)
        }
    }

    fun rememberFocusedChild(childId: Long) {
        updateContent { content ->
            if (content.parent.children.none { it.id == childId }) {
                content
            } else {
                content.copy(focusedChildId = childId)
            }
        }
    }

    fun rememberChildViewport(snapshot: GridViewportSnapshot) {
        updateContent { content ->
            if (content.childViewport == snapshot) content else content.copy(childViewport = snapshot)
        }
    }

    private inline fun updateContent(
        transform: (MediaDetailUiState.Content) -> MediaDetailUiState.Content,
    ) {
        val content = mutableState.value as? MediaDetailUiState.Content ?: return
        mutableState.value = transform(content)
    }
}
