package org.kaloscope.tv.feature.search

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.data.search.SearchRepository

sealed interface SearchUiState {
    data object Loading : SearchUiState

    data object EmptyIndexers : SearchUiState

    data class Error(val error: AppError) : SearchUiState

    data class Content(
        val indexers: List<NetworkIndexer>,
        val selectedIndexerId: Long,
        val source: SearchSourceState = SearchSourceState.Loading,
        val query: String = "",
        val submittedKeyword: String = "",
        val results: SearchResultsState = SearchResultsState.AwaitingQuery,
        val focusedResultId: String? = null,
        val resolvingResultId: String? = null,
        val playbackError: AppError? = null,
        val pendingPlaybackRequestId: String? = null,
    ) : SearchUiState
}

sealed interface SearchSourceState {
    data object Loading : SearchSourceState

    data object WebAuthRequired : SearchSourceState

    data class Error(val error: AppError) : SearchSourceState

    data class Ready(val profile: IndexerSourceProfile) : SearchSourceState
}

sealed interface SearchResultsState {
    val items: List<NetworkSearchResult>
    val hasNext: Boolean

    data object AwaitingQuery : SearchResultsState {
        override val items = emptyList<NetworkSearchResult>()
        override val hasNext = false
    }

    data object Loading : SearchResultsState {
        override val items = emptyList<NetworkSearchResult>()
        override val hasNext = false
    }

    data object Empty : SearchResultsState {
        override val items = emptyList<NetworkSearchResult>()
        override val hasNext = false
    }

    data class Error(val error: AppError) : SearchResultsState {
        override val items = emptyList<NetworkSearchResult>()
        override val hasNext = false
    }

    data class Content(
        override val items: List<NetworkSearchResult>,
        val total: Int?,
        val pageNumber: Int,
        override val hasNext: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreError: AppError? = null,
    ) : SearchResultsState
}

class SearchCoordinator(
    private val repository: SearchRepository,
    private val requestStore: PlaybackRequestStore,
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() },
) {
    private val mutableState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)

    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    fun reset() {
        mutableState.value = SearchUiState.Loading
    }

    suspend fun load(session: Session) {
        mutableState.value = SearchUiState.Loading
        when (val result = repository.getIndexers(session)) {
            is AppResult.Failure -> mutableState.value = SearchUiState.Error(result.error)
            is AppResult.Success -> {
                if (result.value.isEmpty()) {
                    mutableState.value = SearchUiState.EmptyIndexers
                    return
                }
                mutableState.value = SearchUiState.Content(
                    indexers = result.value,
                    selectedIndexerId = result.value.first().id,
                )
                loadProfile(session, result.value.first())
            }
        }
    }

    fun updateQuery(value: String) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        mutableState.value = content.copy(query = value)
    }

    suspend fun selectIndexer(
        session: Session,
        indexerId: Long,
    ) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        val indexer = content.indexers.firstOrNull { it.id == indexerId } ?: return
        if (content.selectedIndexerId == indexerId) {
            return
        }
        mutableState.value = content.copy(
            selectedIndexerId = indexerId,
            source = SearchSourceState.Loading,
            query = "",
            submittedKeyword = "",
            results = SearchResultsState.AwaitingQuery,
            focusedResultId = null,
            playbackError = null,
        )
        loadProfile(session, indexer)
    }

    suspend fun search(session: Session) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        val ready = content.source as? SearchSourceState.Ready ?: return
        val keyword = content.query.trim()
        if (ready.profile.keywordRequired && keyword.isBlank()) {
            mutableState.value = content.copy(
                submittedKeyword = "",
                results = SearchResultsState.AwaitingQuery,
            )
            return
        }
        mutableState.value = content.copy(
            submittedKeyword = keyword,
            results = SearchResultsState.Loading,
            focusedResultId = null,
            playbackError = null,
        )
        loadFirstPage(session, ready.profile, keyword)
    }

    suspend fun retry(session: Session) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        when (content.source) {
            is SearchSourceState.Error,
            SearchSourceState.WebAuthRequired,
            -> {
                val indexer = content.indexers.first { it.id == content.selectedIndexerId }
                mutableState.value = content.copy(source = SearchSourceState.Loading)
                loadProfile(session, indexer)
            }

            is SearchSourceState.Ready -> {
                mutableState.value = content.copy(results = SearchResultsState.Loading)
                loadFirstPage(session, content.source.profile, content.submittedKeyword)
            }

            else -> Unit
        }
    }

    suspend fun loadNext(session: Session) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        val ready = content.source as? SearchSourceState.Ready ?: return
        val current = content.results as? SearchResultsState.Content ?: return
        if (!current.hasNext || current.isLoadingMore) {
            return
        }
        mutableState.value = content.copy(
            results = current.copy(isLoadingMore = true, loadMoreError = null),
        )
        when (
            val result = repository.search(
                session,
                ready.profile,
                content.submittedKeyword,
                current.pageNumber + 1,
            )
        ) {
            is AppResult.Failure -> updateContent {
                copy(results = current.copy(isLoadingMore = false, loadMoreError = result.error))
            }

            is AppResult.Success -> updateContent {
                copy(
                    results = SearchResultsState.Content(
                        items = (current.items + result.value.items)
                            .distinctBy(NetworkSearchResult::id),
                        total = result.value.total,
                        pageNumber = result.value.pageNumber,
                        hasNext = result.value.hasNext,
                    ),
                )
            }
        }
    }

    fun rememberFocusedResult(resultId: String) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        if (content.results.items.any { it.id == resultId }) {
            mutableState.value = content.copy(focusedResultId = resultId)
        }
    }

    suspend fun play(
        session: Session,
        resultId: String,
    ) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        val result = content.results.items.firstOrNull { it.id == resultId } ?: return
        if (content.resolvingResultId != null) {
            return
        }
        mutableState.value = content.copy(
            resolvingResultId = resultId,
            playbackError = null,
        )
        when (
            val playback = repository.resolvePlayback(
                session,
                content.selectedIndexerId,
                result,
                TranscodeResolution.P1080,
            )
        ) {
            is AppResult.Failure -> updateContent {
                copy(resolvingResultId = null, playbackError = playback.error)
            }

            is AppResult.Success -> {
                val request = PlaybackRequest.NetworkVideo(
                    requestId = requestIdFactory(),
                    serverId = session.server.id,
                    title = playback.value.title,
                    source = playback.value,
                )
                requestStore.put(request)
                updateContent {
                    copy(
                        resolvingResultId = null,
                        playbackError = null,
                        pendingPlaybackRequestId = request.requestId,
                    )
                }
            }
        }
    }

    fun consumePlaybackRequest(requestId: String) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        if (content.pendingPlaybackRequestId == requestId) {
            mutableState.value = content.copy(pendingPlaybackRequestId = null)
        }
    }

    private suspend fun loadProfile(
        session: Session,
        indexer: NetworkIndexer,
    ) {
        when (val result = repository.getProfile(session, indexer)) {
            is AppResult.Failure -> updateContent {
                copy(source = SearchSourceState.Error(result.error))
            }

            is AppResult.Success -> {
                if (result.value.webAuthRequired) {
                    updateContent { copy(source = SearchSourceState.WebAuthRequired) }
                } else {
                    updateContent { copy(source = SearchSourceState.Ready(result.value)) }
                    if (!result.value.keywordRequired) {
                        search(session)
                    }
                }
            }
        }
    }

    private suspend fun loadFirstPage(
        session: Session,
        profile: IndexerSourceProfile,
        keyword: String,
    ) {
        when (val result = repository.search(session, profile, keyword, 1)) {
            is AppResult.Failure -> updateContent {
                copy(results = SearchResultsState.Error(result.error))
            }

            is AppResult.Success -> updateContent {
                copy(
                    results = if (result.value.items.isEmpty()) {
                        SearchResultsState.Empty
                    } else {
                        SearchResultsState.Content(
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

    private inline fun updateContent(
        transform: SearchUiState.Content.() -> SearchUiState.Content,
    ) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        mutableState.value = content.transform()
    }
}
