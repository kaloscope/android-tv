package org.kaloscope.tv.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.BrowseLayoutTokens
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeBusyIndicator
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.ContentCardFocused
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
    indexerEntryFocusRequester: FocusRequester? = null,
    topNavigationFocusRequester: FocusRequester? = null,
    onRefreshIndexers: () -> Unit,
    onSelectIndexer: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onResultFocused: (String) -> Unit,
    onOpenResult: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onDismissFilters: () -> Unit,
    onApplyFilters: (Map<String, SearchFilterValue>) -> Unit,
    onClearFilters: () -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit = {},
    onManageServers: () -> Unit = {},
) {
    when (state) {
        SearchUiState.Loading -> KaloscopeLoadingLayout("search-loading")

        SearchUiState.EmptyIndexers -> SearchEmptyIndexers(
            requestInitialFocus = requestInitialFocus,
            entryFocusRequester = indexerEntryFocusRequester,
            topNavigationFocusRequester = topNavigationFocusRequester,
            onRefresh = onRefreshIndexers,
            onManageServers = onManageServers,
        )

        is SearchUiState.Error -> SearchError(state.error, onRetry)
        is SearchUiState.Content -> SearchContent(
            session = session,
            state = state,
            requestInitialFocus = requestInitialFocus,
            indexerEntryFocusRequester = indexerEntryFocusRequester,
            topNavigationFocusRequester = topNavigationFocusRequester,
            onSelectIndexer = onSelectIndexer,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onRetry = onRetry,
            onLoadMore = onLoadMore,
            onResultFocused = onResultFocused,
            onGridViewportChanged = onGridViewportChanged,
            onOpenResult = onOpenResult,
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
    indexerEntryFocusRequester: FocusRequester?,
    topNavigationFocusRequester: FocusRequester?,
    onSelectIndexer: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onResultFocused: (String) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit,
    onOpenResult: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onDismissFilters: () -> Unit,
    onApplyFilters: (Map<String, SearchFilterValue>) -> Unit,
    onClearFilters: () -> Unit,
) {
    val internalIndexerEntryFocus = remember { FocusRequester() }
    val indexerEntryFocus =
        indexerEntryFocusRequester ?: internalIndexerEntryFocus
    val hasMultipleIndexers = state.indexers.size > 1
    val firstIndexerFocus = remember { FocusRequester() }
    val internalSelectedIndexerFocus = remember { FocusRequester() }
    val selectedIndexerIndex = state.indexers
        .indexOfFirst { it.id == state.selectedIndexerId }
        .takeIf { it >= 0 }
        ?: state.indexers.indices.firstOrNull()
        ?: -1
    val selectedIndexerFocus = if (selectedIndexerIndex == 0) {
        firstIndexerFocus
    } else {
        internalSelectedIndexerFocus
    }
    val internalSearchInputFocus = remember { FocusRequester() }
    val searchInputFocus = if (hasMultipleIndexers) {
        internalSearchInputFocus
    } else {
        indexerEntryFocus
    }
    val filterButtonFocus = remember { FocusRequester() }
    val resultEntryFocus = remember { FocusRequester() }
    val hasFocusableResults = (state.results as? SearchResultsState.Content)
        ?.items
        ?.isNotEmpty() == true
    var restoreFilterFocus by remember { mutableStateOf(false) }
    // Source changes refresh content in-place and must not replay root-entry focus.
    LaunchedEffect(Unit) {
        if (requestInitialFocus && state.focusedResultId == null) {
            if (hasMultipleIndexers) {
                firstIndexerFocus.requestFocus()
            } else {
                searchInputFocus.requestFocus()
            }
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
        horizontalArrangement = Arrangement.spacedBy(BrowseLayoutTokens.PaneSpacing),
    ) {
        IndexerSidebar(
            session = session,
            indexers = state.indexers,
            selectedIndexerId = state.selectedIndexerId,
            selectedIndexerIndex = selectedIndexerIndex,
            sidebarFocus = indexerEntryFocus.takeIf { hasMultipleIndexers },
            firstIndexerFocus = firstIndexerFocus,
            selectedIndexerFocus = selectedIndexerFocus,
            menuItemsAreFocusable = hasMultipleIndexers,
            resultEntryFocusRequester = resultEntryFocus.takeIf { hasFocusableResults },
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
                inputFocusRequester = searchInputFocus,
                filterFocusRequester = filterButtonFocus,
                topNavigationFocusRequester = topNavigationFocusRequester,
                onValueChange = onQueryChange,
                onSearch = onSearch,
                onOpenFilters = {
                    restoreFilterFocus = true
                    onOpenFilters()
                },
            )
            Spacer(Modifier.height(BrowseLayoutTokens.HeaderContentSpacing))
            SearchResults(
                session = session,
                state = state,
                coverRatio = state.selectedProfile.coverRatio,
                requestInitialFocus = requestInitialFocus,
                resultEntryFocusRequester = resultEntryFocus,
                resultExitFocusRequester = indexerEntryFocus.takeIf {
                    hasMultipleIndexers
                },
                onRetry = {
                    if (hasMultipleIndexers) {
                        selectedIndexerFocus.requestFocus()
                    } else {
                        searchInputFocus.requestFocus()
                    }
                    onRetry()
                },
                onLoadMore = onLoadMore,
                onResultFocused = onResultFocused,
                onGridViewportChanged = onGridViewportChanged,
                onOpenResult = onOpenResult,
            )
        }
    }
    fun runFilterCloseAction(action: () -> Unit) {
        // Hand focus back before the modal node disappears; transient Home focus changes routes.
        filterButtonFocus.requestFocus()
        action()
    }

    if (state.filterDrawerOpen) {
        SearchFilterDrawer(
            definitions = state.selectedProfile.filters,
            appliedValues = state.appliedFilters,
            onApply = { values ->
                runFilterCloseAction { onApplyFilters(values) }
            },
            onClear = {
                runFilterCloseAction(onClearFilters)
            },
            onDismiss = {
                runFilterCloseAction(onDismissFilters)
            },
        )
    }
}

@Composable
private fun SearchEmptyIndexers(
    requestInitialFocus: Boolean,
    entryFocusRequester: FocusRequester?,
    topNavigationFocusRequester: FocusRequester?,
    onRefresh: () -> Unit,
    onManageServers: () -> Unit,
) {
    val internalEntryFocus = remember { FocusRequester() }
    val refreshFocus = entryFocusRequester ?: internalEntryFocus
    LaunchedEffect(Unit) {
        if (requestInitialFocus) {
            refreshFocus.requestFocus()
        }
    }
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KaloscopeButton(
                onClick = onRefresh,
                modifier = Modifier
                    .focusRequester(refreshFocus)
                    .focusProperties {
                        topNavigationFocusRequester?.let { up = it }
                    }
                    .testTag("refresh-indexers"),
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
            ) {
                Text(stringResource(R.string.refresh_indexers))
            }
            KaloscopeButton(
                onClick = onManageServers,
                modifier = Modifier
                    .focusProperties {
                        topNavigationFocusRequester?.let { up = it }
                    }
                    .testTag("search-manage-servers"),
                variant = KaloscopeControlVariant.Ghost,
                size = KaloscopeControlSize.Compact,
            ) {
                Text(stringResource(R.string.switch_server))
            }
        }
    }
}

@Composable
private fun IndexerSidebar(
    session: Session,
    indexers: List<NetworkIndexer>,
    selectedIndexerId: Long,
    selectedIndexerIndex: Int,
    sidebarFocus: FocusRequester?,
    firstIndexerFocus: FocusRequester,
    selectedIndexerFocus: FocusRequester,
    menuItemsAreFocusable: Boolean,
    resultEntryFocusRequester: FocusRequester?,
    topNavigationFocusRequester: FocusRequester?,
    onSelectIndexer: (Long) -> Unit,
) {
    val firstIndexerId = indexers.firstOrNull()?.id
    val listState = rememberLazyListState()
    val focusScope = rememberCoroutineScope()
    var focusEntryJob by remember { mutableStateOf<Job?>(null) }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .then(
                sidebarFocus?.let { Modifier.focusRequester(it) } ?: Modifier,
            )
            .focusProperties {
                onEnter = {
                    if (
                        (
                            requestedFocusDirection == FocusDirection.Down ||
                                requestedFocusDirection == FocusDirection.Left
                        ) &&
                        selectedIndexerIndex >= 0 &&
                        menuItemsAreFocusable
                    ) {
                        cancelFocusChange()
                        focusEntryJob?.cancel()
                        focusEntryJob = focusScope.launch {
                            val targetIsVisible = listState.layoutInfo
                                .visibleItemsInfo
                                .any { it.index == selectedIndexerIndex }
                            if (!targetIsVisible) {
                                listState.scrollToItem(selectedIndexerIndex)
                            }
                            withFrameNanos { }
                            selectedIndexerFocus.requestFocus()
                        }
                    }
                }
            }
            .focusGroup()
            .width(BrowseLayoutTokens.SidebarWidth)
            .fillMaxHeight()
            .background(Panel.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
            .padding(BrowseLayoutTokens.SidebarContentPadding),
        verticalArrangement = Arrangement.spacedBy(BrowseLayoutTokens.SidebarItemSpacing),
    ) {
        items(indexers, key = NetworkIndexer::id) { indexer ->
            val isFirstIndexer = indexer.id == firstIndexerId
            val isSelectedIndexer = indexer.id == selectedIndexerId
            KaloscopeButton(
                onClick = { onSelectIndexer(indexer.id) },
                selected = indexer.id == selectedIndexerId,
                variant = KaloscopeControlVariant.Sidebar,
                size = KaloscopeControlSize.Row,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(
                    horizontal = BrowseLayoutTokens.SidebarItemHorizontalPadding,
                    vertical = 0.dp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BrowseLayoutTokens.SidebarItemHeight)
                    .testTag("indexer-${indexer.id}")
                    .focusProperties {
                        canFocus = menuItemsAreFocusable
                        resultEntryFocusRequester?.let { right = it }
                    }
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
                    ServerImage(
                        session = session,
                        rawValue = indexer.iconPath,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        policy = ServerImagePolicy.Auto,
                    )
                    Spacer(Modifier.width(BrowseLayoutTokens.SidebarIconTextSpacing))
                    Text(
                        text = indexer.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
    inputFocusRequester: FocusRequester,
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
            focusRequester = inputFocusRequester,
            onMoveUp = topNavigationFocusRequester?.let { requester ->
                { requester.requestFocus() }
            },
            onMoveRight = firstActionFocus::requestFocus,
            modifier = Modifier
                .weight(1f)
                .height(BrowseLayoutTokens.SearchControlHeight)
                .focusProperties { right = firstActionFocus }
                .testTag("network-search-input"),
        )
        if (filtersAvailable) {
            KaloscopeIconButton(
                onClick = onOpenFilters,
                selected = filtersActive,
                modifier = Modifier
                    .size(BrowseLayoutTokens.SearchControlHeight)
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
                .size(BrowseLayoutTokens.SearchControlHeight)
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
    resultEntryFocusRequester: FocusRequester,
    resultExitFocusRequester: FocusRequester?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onResultFocused: (String) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit,
    onOpenResult: (String) -> Unit,
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
            val firstVisibleResultIndex by remember(gridState, results.items.size) {
                derivedStateOf {
                    if (results.items.isEmpty()) {
                        -1
                    } else {
                        val layoutInfo = gridState.layoutInfo
                        layoutInfo.visibleItemsInfo
                            .asSequence()
                            .filter { item ->
                                val itemTop = item.offset.y
                                val itemBottom = itemTop + item.size.height
                                itemBottom > layoutInfo.viewportStartOffset &&
                                    itemTop < layoutInfo.viewportEndOffset
                            }
                            .map { item -> item.index }
                            .filter { it in results.items.indices }
                            .minOrNull()
                            ?: gridState.firstVisibleItemIndex.coerceIn(
                                0,
                                results.items.lastIndex,
                            )
                    }
                }
            }
            val leftmostResultIndices by remember(gridState, results.items.size) {
                derivedStateOf {
                    gridState.layoutInfo.visibleItemsInfo
                        .asSequence()
                        .filter { item ->
                            item.index in results.items.indices && item.column == 0
                        }
                        .map { item -> item.index }
                        .toSet()
                }
            }
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
                state.resolutionError?.let { error ->
                    Text(
                        text = stringResource(
                            R.string.resolve_resource_failed,
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
                        minSize = if (coverRatio >= 1f) {
                            BrowseLayoutTokens.LandscapeGridMinWidth
                        } else {
                            BrowseLayoutTokens.PortraitGridMinWidth
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search-results-grid"),
                    contentPadding = PaddingValues(
                        start = BrowseLayoutTokens.GridHorizontalContentPadding,
                        top = BrowseLayoutTokens.GridTopContentPadding,
                        end = BrowseLayoutTokens.GridHorizontalContentPadding,
                        bottom = BrowseLayoutTokens.GridBottomContentPadding,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(
                        BrowseLayoutTokens.NetworkGridHorizontalSpacing,
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        BrowseLayoutTokens.GridVerticalSpacing,
                    ),
                ) {
                    itemsIndexed(
                        items = results.items,
                        key = { _, result -> result.id },
                    ) { resultIndex, result ->
                        NetworkResultCard(
                            session = session,
                            result = result,
                            coverRatio = coverRatio,
                            restoreFocus = result.id == restoreResultId &&
                                (requestInitialFocus || state.resolutionError != null),
                            entryFocusRequester = resultEntryFocusRequester.takeIf {
                                resultIndex == firstVisibleResultIndex
                            },
                            leftFocusRequester = resultExitFocusRequester.takeIf {
                                resultIndex in leftmostResultIndices
                            },
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
                            onClick = { onOpenResult(result.id) },
                        )
                    }
                    if (results.hasNext && results.isLoadingMore) {
                        item(
                            key = "search-load-more-loading",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Row(
                                modifier = Modifier.testTag("search-load-more-loading"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                KaloscopeBusyIndicator(
                                    modifier = Modifier.testTag(
                                        "search-load-more-loading-indicator",
                                    ),
                                    color = Muted,
                                )
                                Text(
                                    text = stringResource(R.string.loading_more),
                                    color = Muted,
                                    fontSize = 14.sp,
                                )
                            }
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
    entryFocusRequester: FocusRequester?,
    leftFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val restoreFocusRequester = remember(result.id) { FocusRequester() }
    val focusRequester = entryFocusRequester ?: restoreFocusRequester
    LaunchedEffect(restoreFocus, focusRequester) {
        if (restoreFocus) {
            withFrameNanos { }
            focusRequester.requestFocus()
        }
    }
    KaloscopeFocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        containerColor = Panel.copy(alpha = 0.65f),
        focusedContainerColor = ContentCardFocused,
        focusScale = BrowseLayoutTokens.GridCardFocusScale,
        focusScaleEdgeClearance = BrowseLayoutTokens.GridCardFocusEdgeClearance,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("network-result-${result.id}")
            .focusRequester(focusRequester)
            .focusProperties {
                leftFocusRequester?.let { left = it }
            }
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
                // Keep titles and metadata anchored to opposite edges across every card.
                // Their minimum two-line heights trim the gap without shifting either anchor.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(41.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Text(
                        text = result.title,
                        color = OnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SearchResultFooter(result)
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
            .height(32.dp)
            .testTag("search-result-footer-${result.id}"),
        contentAlignment = Alignment.CenterStart,
    ) {
        val sizeMaxWidth = maxWidth / 2
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Bottom,
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
                    lineHeight = 14.sp,
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
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("search-retry-refresh-icon"),
            )
            Spacer(Modifier.width(7.dp))
            Text(stringResource(R.string.retry))
        }
    }
}
