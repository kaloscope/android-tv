package org.kaloscope.tv.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.shouldPrefetchGridItem
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.KaloscopeGridSkeleton
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.designsystem.TvSearchField
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.Session

@Composable
fun SearchScreen(
    session: Session,
    state: SearchUiState,
    onRefreshIndexers: () -> Unit,
    onSelectIndexer: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onResultFocused: (String) -> Unit,
    onPlay: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onDismissFilters: () -> Unit,
    onApplyFilters: (Map<String, SearchFilterValue>) -> Unit,
    onClearFilters: () -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit = {},
) {
    when (state) {
        SearchUiState.Loading -> KaloscopeGridSkeleton("search-loading-skeleton")

        SearchUiState.EmptyIndexers -> SearchEmptyIndexers(onRefreshIndexers)

        is SearchUiState.Error -> SearchError(state.error, onRetry)
        is SearchUiState.Content -> SearchContent(
            session = session,
            state = state,
            onSelectIndexer = onSelectIndexer,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onRetry = onRetry,
            onLoadMore = onLoadMore,
            onResultFocused = onResultFocused,
            onGridViewportChanged = onGridViewportChanged,
            onPlay = onPlay,
            onOpenFilters = onOpenFilters,
            onDismissFilters = onDismissFilters,
            onApplyFilters = onApplyFilters,
            onClearFilters = onClearFilters,
        )
    }
}

@Composable
private fun SearchContent(
    session: Session,
    state: SearchUiState.Content,
    onSelectIndexer: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onResultFocused: (String) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit,
    onPlay: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onDismissFilters: () -> Unit,
    onApplyFilters: (Map<String, SearchFilterValue>) -> Unit,
    onClearFilters: () -> Unit,
) {
    val firstIndexerFocus = remember { FocusRequester() }
    val filterButtonFocus = remember { FocusRequester() }
    var restoreFilterFocus by remember { mutableStateOf(false) }
    LaunchedEffect(state.selectedIndexerId, state.focusedResultId) {
        if (state.focusedResultId == null) {
            firstIndexerFocus.requestFocus()
        }
    }
    LaunchedEffect(state.filterDrawerOpen) {
        if (!state.filterDrawerOpen && restoreFilterFocus) {
            withFrameNanos { }
            filterButtonFocus.requestFocus()
            restoreFilterFocus = false
        }
    }
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        IndexerSidebar(
            indexers = state.indexers,
            selectedIndexerId = state.selectedIndexerId,
            firstIndexerFocus = firstIndexerFocus,
            onSelectIndexer = onSelectIndexer,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag("search-content"),
        ) {
            SearchInput(
                value = state.query,
                filtersAvailable = state.selectedProfile.filters.isNotEmpty(),
                filtersActive = state.appliedFilters.isNotEmpty(),
                filterFocusRequester = filterButtonFocus,
                onValueChange = onQueryChange,
                onSearch = onSearch,
                onOpenFilters = {
                    restoreFilterFocus = true
                    onOpenFilters()
                },
            )
            Spacer(Modifier.height(22.dp))
            SearchResults(
                session = session,
                state = state,
                coverRatio = state.selectedProfile.coverRatio,
                onRetry = onRetry,
                onLoadMore = onLoadMore,
                onResultFocused = onResultFocused,
                onGridViewportChanged = onGridViewportChanged,
                onPlay = onPlay,
            )
        }
    }
    if (state.filterDrawerOpen) {
        SearchFilterDrawer(
            definitions = state.selectedProfile.filters,
            appliedValues = state.appliedFilters,
            onApply = onApplyFilters,
            onClear = onClearFilters,
            onDismiss = onDismissFilters,
        )
    }
}

@Composable
private fun SearchEmptyIndexers(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(28.dp),
    ) {
        Text(
            text = stringResource(R.string.no_indexers),
            color = OnBackground,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_indexers_description),
            color = Muted,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.testTag("refresh-indexers"),
            colors = ButtonDefaults.colors(focusedContainerColor = Primary),
        ) {
            Text(stringResource(R.string.refresh_indexers))
        }
    }
}

@Composable
private fun IndexerSidebar(
    indexers: List<NetworkIndexer>,
    selectedIndexerId: Long,
    firstIndexerFocus: FocusRequester,
    onSelectIndexer: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(Panel.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(indexers, key = NetworkIndexer::id) { indexer ->
            KaloscopeFocusSurface(
                onClick = { onSelectIndexer(indexer.id) },
                selected = indexer.id == selectedIndexerId,
                shape = RoundedCornerShape(12.dp),
                focusedContainerColor = PanelElevated,
                focusScale = 1.02f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("indexer-${indexer.id}")
                    .then(
                        if (indexer == indexers.first()) {
                            Modifier.focusRequester(firstIndexerFocus)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = indexer.name.take(1),
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = indexer.name,
                        color = OnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchInput(
    value: String,
    filtersAvailable: Boolean,
    filtersActive: Boolean,
    filterFocusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenFilters: () -> Unit,
) {
    val searchActionFocus = remember { FocusRequester() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvSearchField(
            value = value,
            hint = stringResource(R.string.search_indexer_hint),
            onValueChange = onValueChange,
            onSearch = onSearch,
            onMoveRight = searchActionFocus::requestFocus,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .focusProperties { right = searchActionFocus }
                .testTag("network-search-input"),
        )
        Button(
            onClick = onSearch,
            modifier = Modifier
                .focusRequester(searchActionFocus)
                .focusProperties {
                    right = if (filtersAvailable) {
                        filterFocusRequester
                    } else {
                        FocusRequester.Cancel
                    }
                }
                .testTag("search-action-button"),
            colors = ButtonDefaults.colors(focusedContainerColor = Primary),
        ) {
            Text(stringResource(R.string.search_action))
        }
        if (filtersAvailable) {
            Button(
                onClick = onOpenFilters,
                modifier = Modifier
                    .focusRequester(filterFocusRequester)
                    .testTag("search-filter-button"),
                colors = ButtonDefaults.colors(
                    containerColor = if (filtersActive) Primary else PanelElevated,
                    focusedContainerColor = Primary,
                ),
            ) {
                Text(
                    stringResource(
                        if (filtersActive) {
                            R.string.search_filters_active
                        } else {
                            R.string.search_filters
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    session: Session,
    state: SearchUiState.Content,
    coverRatio: Float,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onResultFocused: (String) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit,
    onPlay: (String) -> Unit,
) {
    when (val results = state.results) {
        SearchResultsState.AwaitingQuery -> SearchStatus(
            title = stringResource(R.string.search_indexer_prompt),
            description = stringResource(R.string.search_indexer_prompt_description),
        )

        SearchResultsState.Loading -> KaloscopeGridSkeleton("search-results-loading-skeleton")

        SearchResultsState.Empty -> SearchStatus(
            title = stringResource(R.string.no_search_results),
            description = stringResource(R.string.no_search_results_description),
        )

        is SearchResultsState.Error -> SearchError(results.error, onRetry)
        is SearchResultsState.Content -> {
            val restoreIndex = state.gridViewport.firstVisibleItemIndex
                .coerceIn(0, results.items.lastIndex.coerceAtLeast(0))
            val restoreResultId = state.focusedResultId?.let { focusedId ->
                focusedId.takeIf { id -> results.items.any { it.id == id } }
                    ?: results.items.getOrNull(restoreIndex)?.id
            }
            val gridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = restoreIndex,
                initialFirstVisibleItemScrollOffset =
                    state.gridViewport.firstVisibleItemScrollOffset,
            )
            var lastPrefetchedPage by remember(
                state.selectedIndexerId,
                state.submittedKeyword,
                state.appliedFilters,
            ) {
                mutableIntStateOf(-1)
            }
            LaunchedEffect(gridState, results.items.size) {
                snapshotFlow {
                    GridViewportSnapshot(
                        firstVisibleItemIndex = gridState.firstVisibleItemIndex
                            .coerceAtMost(results.items.lastIndex.coerceAtLeast(0)),
                        firstVisibleItemScrollOffset =
                            gridState.firstVisibleItemScrollOffset.coerceAtLeast(0),
                    )
                }.distinctUntilChanged().collect(onGridViewportChanged)
            }
            Column(modifier = Modifier.fillMaxSize()) {
                state.playbackError?.let { error ->
                    Text(
                        text = stringResource(
                            R.string.resolve_playback_failed,
                            searchErrorText(error),
                        ),
                        color = Danger,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(
                        minSize = if (coverRatio >= 1f) 238.dp else 172.dp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search-results-grid"),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = 8.dp,
                        end = 8.dp,
                        bottom = 24.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    itemsIndexed(
                        items = results.items,
                        key = { _, result -> result.id },
                    ) { resultIndex, result ->
                        NetworkResultCard(
                            session = session,
                            result = result,
                            coverRatio = coverRatio,
                            restoreFocus = result.id == restoreResultId,
                            enabled = state.resolvingResultId == null,
                            resolving = result.id == state.resolvingResultId,
                            onFocused = {
                                onResultFocused(result.id)
                                if (
                                    lastPrefetchedPage != results.pageNumber &&
                                    shouldPrefetchGridItem(
                                        focusedItemIndex = resultIndex,
                                        itemCount = results.items.size,
                                        columnCount = gridState.layoutInfo.maxSpan,
                                        hasNext = results.hasNext,
                                        isLoadingMore = results.isLoadingMore,
                                        hasLoadMoreError = results.loadMoreError != null,
                                    )
                                ) {
                                    lastPrefetchedPage = results.pageNumber
                                    onLoadMore()
                                }
                            },
                            onClick = { onPlay(result.id) },
                        )
                    }
                    if (results.hasNext && results.isLoadingMore) {
                        item(
                            key = "search-load-more-loading",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Text(
                                text = stringResource(R.string.loading_more),
                                color = Muted,
                                fontSize = 14.sp,
                                modifier = Modifier.testTag("search-load-more-loading"),
                            )
                        }
                    }
                    if (
                        results.hasNext &&
                        !results.isLoadingMore &&
                        results.loadMoreError != null
                    ) {
                        item(
                            key = "search-load-more-retry",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Column {
                                Text(
                                    text = searchErrorText(results.loadMoreError),
                                    color = Danger,
                                    fontSize = 14.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = onLoadMore,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search-load-more-retry"),
                                    colors = ButtonDefaults.colors(
                                        focusedContainerColor = Primary,
                                    ),
                                ) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkResultCard(
    session: Session,
    result: NetworkSearchResult,
    coverRatio: Float,
    restoreFocus: Boolean,
    enabled: Boolean,
    resolving: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val requester = remember(result.id) { FocusRequester() }
    LaunchedEffect(restoreFocus) {
        if (restoreFocus) {
            withFrameNanos { }
            requester.requestFocus()
        }
    }
    KaloscopeFocusSurface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(15.dp),
        containerColor = Panel.copy(alpha = 0.65f),
        focusedContainerColor = PanelElevated,
        focusScale = 1.04f,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("network-result-${result.id}")
            .focusRequester(requester)
            .onFocusChanged {
                if (it.isFocused) {
                    onFocused()
                }
            }
            .semantics(mergeDescendants = true) {},
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            ServerImage(
                session = session,
                rawValue = result.coverPath,
                fallbackText = result.title,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(coverRatio)
                    .clip(RoundedCornerShape(11.dp)),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = result.title,
                color = OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = if (resolving) {
                    stringResource(R.string.resolving_playback)
                } else {
                    listOfNotNull(
                        result.category,
                        result.rating?.let { stringResource(R.string.rating, it) },
                    ).joinToString(" · ")
                },
                color = if (resolving) Primary else Muted,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SearchStatus(
    title: String,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(28.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(title, color = OnBackground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(description, color = Muted, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SearchError(
    error: AppError,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(28.dp),
    ) {
        Text(
            stringResource(R.string.search_load_failed),
            color = OnBackground,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(searchErrorText(error), color = Danger, fontSize = 16.sp)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.colors(focusedContainerColor = Primary),
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun searchErrorText(error: AppError): String =
    when (error) {
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        AppError.Forbidden -> stringResource(R.string.error_forbidden)
        AppError.NotFound -> stringResource(R.string.error_not_found)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.Offline -> stringResource(R.string.error_offline)
        is AppError.Api -> stringResource(R.string.error_api, error.code.orEmpty())
        is AppError.InvalidData -> stringResource(R.string.error_invalid_data)
    }
