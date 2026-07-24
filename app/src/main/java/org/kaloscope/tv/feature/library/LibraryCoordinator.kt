package org.kaloscope.tv.feature.library

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.media.MediaRepository

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data object EmptyLibraries : LibraryUiState

    data class Error(
        val error: AppError,
    ) : LibraryUiState

    data class Content(
        val libraries: List<MediaLibrary>,
        val selectedLibraryId: Long,
        val query: String = "",
        val submittedKeyword: String = "",
        val items: LibraryItemsState = LibraryItemsState.Loading,
        val focusedMediaId: Long? = null,
    ) : LibraryUiState {
        val selectedLibrary: MediaLibrary
            get() = checkNotNull(libraries.firstOrNull { it.id == selectedLibraryId })
    }
}

sealed interface LibraryItemsState {
    val items: List<MediaSummary>
    val hasNext: Boolean

    data object Loading : LibraryItemsState {
        override val items: List<MediaSummary> = emptyList()
        override val hasNext: Boolean = false
    }

    data object Empty : LibraryItemsState {
        override val items: List<MediaSummary> = emptyList()
        override val hasNext: Boolean = false
    }

    data class Error(
        val error: AppError,
    ) : LibraryItemsState {
        override val items: List<MediaSummary> = emptyList()
        override val hasNext: Boolean = false
    }

    data class Content(
        override val items: List<MediaSummary>,
        val total: Int,
        val pageNumber: Int,
        override val hasNext: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreError: AppError? = null,
    ) : LibraryItemsState
}

class LibraryCoordinator(
    private val repository: MediaRepository,
) {
    private val mutableState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)

    val state: StateFlow<LibraryUiState> = mutableState.asStateFlow()

    fun reset() {
        mutableState.value = LibraryUiState.Loading
    }

    suspend fun load(session: Session) {
        mutableState.value = LibraryUiState.Loading
        when (val result = repository.getLibraries(session)) {
            is AppResult.Failure -> mutableState.value = LibraryUiState.Error(result.error)
            is AppResult.Success -> {
                val libraries = result.value
                if (libraries.isEmpty()) {
                    mutableState.value = LibraryUiState.EmptyLibraries
                    return
                }
                mutableState.value = LibraryUiState.Content(
                    libraries = libraries,
                    selectedLibraryId = libraries.first().id,
                )
                loadFirstPage(session)
            }
        }
    }

    fun updateQuery(value: String) {
        val content = mutableState.value as? LibraryUiState.Content ?: return
        mutableState.value = content.copy(query = value)
    }

    suspend fun search(session: Session) {
        val content = mutableState.value as? LibraryUiState.Content ?: return
        mutableState.value = content.copy(
            submittedKeyword = content.query.trim(),
            items = LibraryItemsState.Loading,
            focusedMediaId = null,
        )
        loadFirstPage(session)
    }

    suspend fun selectLibrary(
        session: Session,
        libraryId: Long,
    ) {
        val content = mutableState.value as? LibraryUiState.Content ?: return
        if (libraryId == content.selectedLibraryId ||
            content.libraries.none { it.id == libraryId }
        ) {
            return
        }
        mutableState.value = content.copy(
            selectedLibraryId = libraryId,
            query = "",
            submittedKeyword = "",
            items = LibraryItemsState.Loading,
            focusedMediaId = null,
        )
        loadFirstPage(session)
    }

    suspend fun retryContent(session: Session) {
        val content = mutableState.value as? LibraryUiState.Content ?: return
        mutableState.value = content.copy(items = LibraryItemsState.Loading)
        loadFirstPage(session)
    }

    suspend fun loadNext(session: Session) {
        val content = mutableState.value as? LibraryUiState.Content ?: return
        val currentItems = content.items as? LibraryItemsState.Content ?: return
        if (!currentItems.hasNext || currentItems.isLoadingMore) {
            return
        }
        mutableState.value = content.copy(
            items = currentItems.copy(
                isLoadingMore = true,
                loadMoreError = null,
            ),
        )
        val nextPage = currentItems.pageNumber + 1
        when (
            val result = repository.getMediaPage(
                session = session,
                libraryId = content.selectedLibraryId,
                pageNumber = nextPage,
                keyword = content.submittedKeyword.nonBlankOrNull(),
            )
        ) {
            is AppResult.Failure -> {
                val latest = mutableState.value as? LibraryUiState.Content ?: return
                mutableState.value = latest.copy(
                    items = currentItems.copy(
                        isLoadingMore = false,
                        loadMoreError = result.error,
                    ),
                )
            }

            is AppResult.Success -> {
                val latest = mutableState.value as? LibraryUiState.Content ?: return
                val combined = (currentItems.items + result.value.items)
                    .distinctBy(MediaSummary::id)
                mutableState.value = latest.copy(
                    items = LibraryItemsState.Content(
                        items = combined,
                        total = result.value.total,
                        pageNumber = result.value.pageNumber,
                        hasNext = result.value.hasNext,
                    ),
                )
            }
        }
    }

    fun rememberFocusedMedia(mediaId: Long) {
        val content = mutableState.value as? LibraryUiState.Content ?: return
        if (content.items.items.none { it.id == mediaId }) {
            return
        }
        mutableState.value = content.copy(focusedMediaId = mediaId)
    }

    private suspend fun loadFirstPage(session: Session) {
        val content = mutableState.value as? LibraryUiState.Content ?: return
        when (
            val result = repository.getMediaPage(
                session = session,
                libraryId = content.selectedLibraryId,
                keyword = content.submittedKeyword.nonBlankOrNull(),
            )
        ) {
            is AppResult.Failure -> {
                val latest = mutableState.value as? LibraryUiState.Content ?: return
                mutableState.value = latest.copy(
                    items = LibraryItemsState.Error(result.error),
                )
            }

            is AppResult.Success -> {
                val latest = mutableState.value as? LibraryUiState.Content ?: return
                mutableState.value = latest.copy(
                    items = if (result.value.items.isEmpty()) {
                        LibraryItemsState.Empty
                    } else {
                        LibraryItemsState.Content(
                            items = result.value.items,
                            total = result.value.total,
                            pageNumber = result.value.pageNumber,
                            hasNext = result.value.hasNext,
                        )
                    },
                )
            }
        }
    }
}

private fun String.nonBlankOrNull(): String? = takeIf(String::isNotBlank)
