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
        val focusedChildDetail: MediaDetail? = null,
        val childDetailError: AppError? = null,
        val childViewport: GridViewportSnapshot = GridViewportSnapshot.Top,
    ) : MediaDetailUiState

    data class Error(
        val error: AppError,
    ) : MediaDetailUiState
}

class MediaDetailCoordinator(
    private val repository: MediaRepository,
) {
    private val childDetailCache = mutableMapOf<Long, MediaDetail>()
    private val mutableState =
        MutableStateFlow<MediaDetailUiState>(MediaDetailUiState.Loading)

    val state: StateFlow<MediaDetailUiState> = mutableState.asStateFlow()

    fun reset() {
        childDetailCache.clear()
        mutableState.value = MediaDetailUiState.Loading
    }

    suspend fun load(
        session: Session,
        mediaId: Long,
    ) {
        childDetailCache.clear()
        mutableState.value = MediaDetailUiState.Loading
        mutableState.value = when (val result = repository.getMediaDetail(session, mediaId)) {
            is AppResult.Success -> MediaDetailUiState.Content(
                parent = result.value,
                focusedChildId = result.value.children.firstOrNull()?.id,
            )
            is AppResult.Failure -> MediaDetailUiState.Error(result.error)
        }
    }

    fun rememberFocusedChild(childId: Long): Boolean {
        var detailLoadRequired = false
        updateContent { content ->
            if (content.parent.children.none { it.id == childId }) {
                content
            } else {
                val cachedDetail = childDetailCache[childId]
                detailLoadRequired = cachedDetail == null
                content.copy(
                    focusedChildId = childId,
                    focusedChildDetail = cachedDetail,
                    childDetailError = null,
                )
            }
        }
        return detailLoadRequired
    }

    suspend fun loadFocusedChild(
        session: Session,
        childId: Long,
    ) {
        val content = mutableState.value as? MediaDetailUiState.Content ?: return
        if (
            content.focusedChildId != childId ||
            content.parent.children.none { it.id == childId }
        ) {
            return
        }
        val parentId = content.parent.id
        childDetailCache[childId]?.let { cachedDetail ->
            publishChildResult(parentId, childId) { current ->
                current.copy(
                    focusedChildDetail = cachedDetail,
                    childDetailError = null,
                )
            }
            return
        }

        when (val result = repository.getMediaDetail(session, childId)) {
            is AppResult.Success -> {
                if (result.value.id != childId) {
                    publishChildFailure(
                        parentId = parentId,
                        childId = childId,
                        error = AppError.InvalidData("media child detail"),
                    )
                    return
                }
                val current = mutableState.value as? MediaDetailUiState.Content ?: return
                if (
                    current.parent.id != parentId ||
                    current.parent.children.none { it.id == childId }
                ) {
                    return
                }
                childDetailCache[childId] = result.value
                if (current.focusedChildId == childId) {
                    mutableState.value = current.copy(
                        focusedChildDetail = result.value,
                        childDetailError = null,
                    )
                }
            }

            is AppResult.Failure -> publishChildFailure(
                parentId = parentId,
                childId = childId,
                error = result.error,
            )
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

    private inline fun publishChildResult(
        parentId: Long,
        childId: Long,
        transform: (MediaDetailUiState.Content) -> MediaDetailUiState.Content,
    ) {
        val current = mutableState.value as? MediaDetailUiState.Content ?: return
        if (current.parent.id == parentId && current.focusedChildId == childId) {
            mutableState.value = transform(current)
        }
    }

    private fun publishChildFailure(
        parentId: Long,
        childId: Long,
        error: AppError,
    ) {
        publishChildResult(parentId, childId) { current ->
            current.copy(
                focusedChildDetail = null,
                childDetailError = error,
            )
        }
    }
}
