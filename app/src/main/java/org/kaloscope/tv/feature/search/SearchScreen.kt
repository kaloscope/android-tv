package org.kaloscope.tv.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.designsystem.RatingBadge
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.designsystem.TvSearchField
import org.kaloscope.tv.core.designsystem.appErrorText
import org.kaloscope.tv.core.designsystem.shouldPrefetchGridItem
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.RatingDisplayPolicy
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
fun SearchScreen(
    session: Session,
    state: SearchUiState,
    requestInitialFocus: Boolean = true,
    firstIndexerFocusRequester: FocusRequester? = null,
    topNavigationFocusRequester: FocusRequester? = null,
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
        SearchUiState.Loading -> KaloscopeLoadingLayout("search-loading")

        SearchUiState.EmptyIndexers -> SearchEmptyIndexers(onRefreshIndexers)

        is SearchUiState.Error -> SearchError(state.error, onRetry)
        is SearchUiState.Content -> SearchContent(
            session = session,
            state = state,
            requestInitialFocus = requestInitialFocus,
            firstIndexerFocusRequester = firstIndexerFocusRequester,
            topNavigationFocusRequester = topNavigationFocusRequester,
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
    requestInitialFocus: Boolean,
    firstIndexerFocusRequester: FocusRequester?,
    topNavigationFocusRequester: FocusRequester?,
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
    val internalFirstIndexerFocus = remember { FocusRequester() }
    val firstIndexerFocus =
        firstIndexerFocusRequester ?: internalFirstIndexerFocus
    val internalSelectedIndexerFocus = remember { FocusRequester() }
    val selectedIndexerFocus = if (
        state.selectedIndexerId == state.indexers.firstOrNull()?.id
    ) {
        firstIndexerFocus
    } else {
        internalSelectedIndexerFocus
    }
    val filterButtonFocus = remember { FocusRequester() }
    var restoreFilterFocus by remember { mutableStateOf(false) }
    LaunchedEffect(
        state.selectedIndexerId,
        state.focusedResultId,
        requestInitialFocus,
    ) {
        if (requestInitialFocus && state.focusedResultId == null) {
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
            session = session,
            indexers = state.indexers,
            selectedIndexerId = state.selectedIndexerId,
            firstIndexerFocus = firstIndexerFocus,
            selectedIndexerFocus = selectedIndexerFocus,
            topNavigationFocusRequester = topNavigationFocusRequester,
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
                topNavigationFocusRequester = topNavigationFocusRequester,
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
                requestInitialFocus = requestInitialFocus,
                onRetry = {
                    selectedIndexerFocus.requestFocus()
                    onRetry()
                },
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
        KaloscopeButton(
            onClick = onRefresh,
            modifier = Modifier.testTag("refresh-indexers"),
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
        ) {
            Text(stringResource(R.string.refresh_indexers))
        }
    }
}

@Composable
private fun IndexerSidebar(
    session: Session,
    indexers: List<NetworkIndexer>,
    selectedIndexerId: Long,
    firstIndexerFocus: FocusRequester,
    selectedIndexerFocus: FocusRequester,
    topNavigationFocusRequester: FocusRequester?,
    onSelectIndexer: (Long) -> Unit,
) {
    val firstIndexerId = indexers.firstOrNull()?.id
    LazyColumn(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(Panel.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(indexers, key = NetworkIndexer::id) { indexer ->
            val isFirstIndexer = indexer.id == firstIndexerId
            val isSelectedIndexer = indexer.id == selectedIndexerId
            KaloscopeButton(
                onClick = { onSelectIndexer(indexer.id) },
                selected = indexer.id == selectedIndexerId,
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Row,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("indexer-${indexer.id}")
                    .then(
                        when {
                            isFirstIndexer -> Modifier.focusRequester(firstIndexerFocus)
                            isSelectedIndexer -> Modifier.focusRequester(selectedIndexerFocus)
                            else -> Modifier
                        },
                    )
                    .then(
                        if (isFirstIndexer) {
                            topNavigationFocusRequester?.let { requester ->
                                Modifier.focusProperties { up = requester }
                            } ?: Modifier
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (indexer.iconPath.isNullOrBlank()) {
                        Text(
                            text = indexer.name.take(1),
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        ServerImage(
                            session = session,
                            rawValue = indexer.iconPath,
                            fallbackText = indexer.name,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            policy = ServerImagePolicy.Auto,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = indexer.name,
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
    topNavigationFocusRequester: FocusRequester?,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenFilters: () -> Unit,
) {
    val searchActionFocus = remember { FocusRequester() }
    val firstActionFocus = if (filtersAvailable) {
        filterFocusRequester
    } else {
        searchActionFocus
    }
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
            onMoveUp = topNavigationFocusRequester?.let { requester ->
                { requester.requestFocus() }
            },
            onMoveRight = firstActionFocus::requestFocus,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .focusProperties { right = firstActionFocus }
                .testTag("network-search-input"),
        )
        if (filtersAvailable) {
            KaloscopeIconButton(
                onClick = onOpenFilters,
                selected = filtersActive,
                modifier = Modifier
                    .size(52.dp)
                    .focusRequester(filterFocusRequester)
                    .focusProperties {
                        topNavigationFocusRequester?.let { up = it }
                        right = searchActionFocus
                    }
                    .testTag("search-filter-button"),
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_filter),
                    contentDescription = stringResource(
                        if (filtersActive) {
                            R.string.search_filters_active
                        } else {
                            R.string.search_filters
                        },
                    ),
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("search-filter-icon"),
                )
            }
        }
        KaloscopeIconButton(
            onClick = onSearch,
            modifier = Modifier
                .size(52.dp)
                .focusRequester(searchActionFocus)
                .focusProperties {
                    topNavigationFocusRequester?.let { up = it }
                    right = FocusRequester.Cancel
                }
                .testTag("search-action-button"),
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_search),
                contentDescription = stringResource(R.string.search_action),
                modifier = Modifier
                    .size(24.dp)
                    .testTag("search-action-icon"),
            )
        }
    }
}

@Composable
private fun SearchResults(
    session: Session,
    state: SearchUiState.Content,
    coverRatio: Float,
    requestInitialFocus: Boolean,
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

        SearchResultsState.Loading -> KaloscopeLoadingLayout("search-results-loading")

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
                            appErrorText(error),
                        ),
                        color = Danger,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(
                        minSize = if (coverRatio >= 1f) 220.dp else 172.dp,
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
                            restoreFocus =
                                requestInitialFocus && result.id == restoreResultId,
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
                                    text = appErrorText(results.loadMoreError),
                                    color = Danger,
                                    fontSize = 14.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                KaloscopeButton(
                                    onClick = onLoadMore,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search-load-more-retry"),
                                    variant = KaloscopeControlVariant.Filled,
                                    size = KaloscopeControlSize.Compact,
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                ServerImage(
                    session = session,
                    rawValue = result.coverPath,
                    fallbackText = result.title,
                    contentDescription = null,
                    policy = ServerImagePolicy.Auto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(coverRatio)
                        .clip(
                            RoundedCornerShape(
                                topStart = 15.dp,
                                topEnd = 15.dp,
                            ),
                        ),
                )
                SearchResultBadge(result)
                SearchResultCoverMetadata(result)
            }
            Column(
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = 9.dp,
                    end = 10.dp,
                    bottom = 8.dp,
                ),
            ) {
                Text(
                    text = result.title,
                    color = OnBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
                SearchResultFooter(
                    result = result,
                    resolving = resolving,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.SearchResultBadge(result: NetworkSearchResult) {
    val ranking = result.ranking
    if (ranking != null) {
        SearchRankingBadge(
            ranking = ranking,
            modifier = Modifier.align(Alignment.TopStart),
            testTag = "search-result-ranking-${result.id}",
        )
    } else {
        RatingDisplayPolicy.format(result.rating)?.let { rating ->
            RatingBadge(
                rating = rating,
                testTag = "search-result-rating-${result.id}",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp),
            )
        }
    }
}

@Composable
private fun SearchRankingBadge(
    ranking: Int,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    val colors = when (ranking) {
        1 -> listOf(Color(0xFFFFD84D), Color(0xFFFF9F1C))
        2 -> listOf(Color(0xFF9EE7FF), Color(0xFF5BA8FF))
        3 -> listOf(Color(0xFFFFCFBC), Color(0xFFFF8F70))
        else -> listOf(Color(0xFF9AA6B8), Color(0xFF6F7D95))
    }
    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minWidth = 28.dp, minHeight = 24.dp)
            .background(
                brush = Brush.linearGradient(colors),
                shape = RoundedCornerShape(bottomEnd = 7.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ranking.toString(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun BoxScope.SearchResultCoverMetadata(result: NetworkSearchResult) {
    if (result.category == null && result.misc == null) {
        return
    }
    Row(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .height(34.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xCC000000)),
                ),
            )
            .padding(start = 9.dp, top = 10.dp, end = 9.dp, bottom = 5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        result.category?.let { category ->
            Text(
                text = category,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        if (result.category != null && result.misc != null) {
            Spacer(Modifier.width(8.dp))
        }
        result.misc?.let { misc ->
            if (result.category == null) {
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = misc,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SearchResultFooter(
    result: NetworkSearchResult,
    resolving: Boolean,
) {
    val uploader = result.uploader?.let {
        stringResource(R.string.search_result_uploader, it)
    }
    val source = listOfNotNull(uploader, result.uploadedAt)
        .joinToString(" · ")
        .takeIf(String::isNotEmpty)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .testTag("search-result-footer-${result.id}"),
        contentAlignment = Alignment.CenterStart,
    ) {
        val sizeMaxWidth = maxWidth / 2
        if (resolving) {
            Text(
                text = stringResource(R.string.resolving_playback),
                color = Primary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (source != null) {
                    Text(
                        text = source,
                        color = Muted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else if (result.size != null) {
                    Spacer(Modifier.weight(1f))
                }
                result.size?.let { size ->
                    if (source != null) {
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = size,
                        color = Muted.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = sizeMaxWidth),
                    )
                }
            }
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
        Text(appErrorText(error), color = Danger, fontSize = 16.sp)
        Spacer(Modifier.height(18.dp))
        KaloscopeButton(
            onClick = onRetry,
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}
