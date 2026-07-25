package org.kaloscope.tv.feature.search

import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.data.search.SearchRepository

sealed interface SearchUiState {
    data object Loading : SearchUiState

    data object EmptyIndexers : SearchUiState

    data class Error(val error: AppError) : SearchUiState

    data class Content(
        val profiles: List<IndexerSourceProfile>,
        val selectedIndexerId: Long,
        val query: String = "",
        val submittedKeyword: String = "",
        val appliedFilters: Map<String, SearchFilterValue> = emptyMap(),
        val filterDrawerOpen: Boolean = false,
        val results: SearchResultsState = SearchResultsState.AwaitingQuery,
        val focusedResultId: String? = null,
        val gridViewport: GridViewportSnapshot = GridViewportSnapshot.Top,
        val resolvingResultId: String? = null,
        val playbackError: AppError? = null,
        val pendingPlaybackRequestId: String? = null,
    ) : SearchUiState {
        val indexers
            get() = profiles.map(IndexerSourceProfile::indexer)

        val selectedProfile: IndexerSourceProfile
            get() = profiles.first { it.indexer.id == selectedIndexerId }
    }
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
        when (val result = repository.getAvailableProfiles(session)) {
            is AppResult.Failure -> mutableState.value = SearchUiState.Error(result.error)
            is AppResult.Success -> {
                if (result.value.isEmpty()) {
                    mutableState.value = SearchUiState.EmptyIndexers
                    return
                }
                val firstProfile = result.value.first()
                mutableState.value = SearchUiState.Content(
                    profiles = result.value,
                    selectedIndexerId = firstProfile.indexer.id,
                )
                if (!firstProfile.keywordRequired) {
                    search(session)
                }
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
        val profile = content.profiles.firstOrNull { it.indexer.id == indexerId } ?: return
        if (content.selectedIndexerId == indexerId) {
            return
        }
        mutableState.value = content.copy(
            selectedIndexerId = indexerId,
            query = "",
            submittedKeyword = "",
            appliedFilters = emptyMap(),
            filterDrawerOpen = false,
            results = SearchResultsState.AwaitingQuery,
            focusedResultId = null,
            gridViewport = GridViewportSnapshot.Top,
            playbackError = null,
        )
        if (!profile.keywordRequired) {
            search(session)
        }
    }

    suspend fun search(session: Session) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        val profile = content.selectedProfile
        val keyword = content.query.trim()
        if (profile.keywordRequired && keyword.isBlank()) {
            mutableState.value = content.copy(
                submittedKeyword = "",
                results = SearchResultsState.AwaitingQuery,
                focusedResultId = null,
                gridViewport = GridViewportSnapshot.Top,
            )
            return
        }
        mutableState.value = content.copy(
            submittedKeyword = keyword,
            results = SearchResultsState.Loading,
            focusedResultId = null,
            gridViewport = GridViewportSnapshot.Top,
            playbackError = null,
        )
        loadFirstPage(session, profile, keyword, content.appliedFilters)
    }

    suspend fun retry(session: Session) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        if (content.results is SearchResultsState.Error) {
            mutableState.value = content.copy(results = SearchResultsState.Loading)
            loadFirstPage(
                session = session,
                profile = content.selectedProfile,
                keyword = content.submittedKeyword,
                filters = content.appliedFilters,
            )
        }
    }

    suspend fun loadNext(session: Session) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        val current = content.results as? SearchResultsState.Content ?: return
        if (!current.hasNext || current.isLoadingMore) {
            return
        }
        mutableState.value = content.copy(
            results = current.copy(isLoadingMore = true, loadMoreError = null),
        )
        try {
            when (
                val result = repository.search(
                    session,
                    content.selectedProfile,
                    content.submittedKeyword,
                    content.appliedFilters,
                    current.pageNumber + 1,
                )
            ) {
                is AppResult.Failure -> updateContent {
                    copy(
                        results = current.copy(
                            isLoadingMore = false,
                            loadMoreError = result.error,
                        ),
                    )
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
        } catch (error: CancellationException) {
            updateContent {
                val latest = results as? SearchResultsState.Content
                if (
                    latest?.isLoadingMore == true &&
                    latest.pageNumber == current.pageNumber
                ) {
                    copy(results = latest.copy(isLoadingMore = false))
                } else {
                    this
                }
            }
            throw error
        }
    }

    fun openFilters() {
        updateContent { copy(filterDrawerOpen = true) }
    }

    fun dismissFilters() {
        updateContent { copy(filterDrawerOpen = false) }
    }

    suspend fun applyFilters(
        session: Session,
        values: Map<String, SearchFilterValue>,
    ) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        val allowedKeys = content.selectedProfile.filters.mapTo(mutableSetOf()) { it.key }
        mutableState.value = content.copy(
            appliedFilters = values.filterKeys(allowedKeys::contains),
            filterDrawerOpen = false,
            focusedResultId = null,
            gridViewport = GridViewportSnapshot.Top,
        )
        search(session)
    }

    suspend fun clearFilters(session: Session) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        mutableState.value = content.copy(
            appliedFilters = emptyMap(),
            filterDrawerOpen = false,
            focusedResultId = null,
            gridViewport = GridViewportSnapshot.Top,
        )
        search(session)
    }

    fun rememberFocusedResult(resultId: String) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        if (content.results.items.any { it.id == resultId }) {
            mutableState.value = content.copy(focusedResultId = resultId)
        }
    }

    fun rememberGridViewport(snapshot: GridViewportSnapshot) {
        val content = mutableState.value as? SearchUiState.Content ?: return
        if (content.gridViewport != snapshot) {
            mutableState.value = content.copy(gridViewport = snapshot)
        }
    }

    suspend fun play(
        session: Session,
        resultId: String,
        settings: TvSettings = TvSettings(),
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
                settings.transcodeResolution,
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
                    preferredDefinition = settings.transcodeResolution,
                    autoplayNext = settings.autoplayNext,
                    danmakuSettings = settings.danmaku,
                    subtitleEnabled = settings.subtitleEnabled,
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

    private suspend fun loadFirstPage(
        session: Session,
        profile: IndexerSourceProfile,
        keyword: String,
        filters: Map<String, SearchFilterValue>,
    ) {
        when (val result = repository.search(session, profile, keyword, filters, 1)) {
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
