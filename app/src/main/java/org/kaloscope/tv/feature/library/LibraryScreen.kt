package org.kaloscope.tv.feature.library

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
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
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.designsystem.KaloscopeNavigationIcon
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.ContentCardFocused
import org.kaloscope.tv.core.designsystem.RatingBadge
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.designsystem.TvSearchField
import org.kaloscope.tv.core.designsystem.appErrorText
import org.kaloscope.tv.core.designsystem.focusSafeBottomPadding
import org.kaloscope.tv.core.designsystem.shouldPrefetchGridItem
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.RatingDisplayPolicy
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
internal fun LibraryScreen(
    session: Session,
    state: LibraryUiState,
    restoreMediaId: Long?,
    requestInitialFocus: Boolean = true,
    libraryEntryFocusRequester: FocusRequester? = null,
    topNavigationFocusRequester: FocusRequester? = null,
    onSelectLibrary: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onMediaFocused: (Long) -> Unit,
    onOpenMedia: (Long) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit = {},
    onBackdropChanged: (LibraryBackdropPresentation) -> Unit = {},
) {
    val currentOnBackdropChanged by rememberUpdatedState(onBackdropChanged)
    val backdrop = (state as? LibraryUiState.Content)?.let { content ->
        val items = (content.items as? LibraryItemsState.Content)?.items.orEmpty()
        resolveLibraryBackdropPresentation(
            items = items,
            restoreMediaId = restoreMediaId,
            focusedMediaId = content.focusedMediaId,
        )
    }
    LaunchedEffect(backdrop) {
        backdrop?.let(currentOnBackdropChanged)
    }

    when (state) {
        LibraryUiState.Loading -> KaloscopeLoadingLayout("library-loading")

        LibraryUiState.EmptyLibraries -> LibraryStatus(
            title = stringResource(R.string.no_libraries),
            description = stringResource(R.string.no_libraries_description),
        )

        is LibraryUiState.Error -> LibraryError(
            error = state.error,
            onRetry = onRetry,
        )

        is LibraryUiState.Content -> LibraryContent(
            session = session,
            state = state,
            restoreMediaId = restoreMediaId,
            requestInitialFocus = requestInitialFocus,
            libraryEntryFocusRequester = libraryEntryFocusRequester,
            topNavigationFocusRequester = topNavigationFocusRequester,
            onSelectLibrary = onSelectLibrary,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onRetry = onRetry,
            onLoadMore = onLoadMore,
            onMediaFocused = onMediaFocused,
            onGridViewportChanged = onGridViewportChanged,
            onOpenMedia = onOpenMedia,
        )
    }
}

@Composable
private fun LibraryContent(
    session: Session,
    state: LibraryUiState.Content,
    restoreMediaId: Long?,
    requestInitialFocus: Boolean,
    libraryEntryFocusRequester: FocusRequester?,
    topNavigationFocusRequester: FocusRequester?,
    onSelectLibrary: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onMediaFocused: (Long) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit,
    onOpenMedia: (Long) -> Unit,
) {
    val internalLibraryEntryFocus = remember { FocusRequester() }
    val libraryEntryFocus =
        libraryEntryFocusRequester ?: internalLibraryEntryFocus
    val hasMultipleLibraries = state.libraries.size > 1
    val firstLibraryFocus = remember { FocusRequester() }
    val internalSelectedLibraryFocus = remember { FocusRequester() }
    val selectedLibraryIndex = state.libraries
        .indexOfFirst { it.id == state.selectedLibraryId }
        .takeIf { it >= 0 }
        ?: state.libraries.indices.firstOrNull()
        ?: -1
    val selectedLibraryFocus = if (selectedLibraryIndex == 0) {
        firstLibraryFocus
    } else {
        internalSelectedLibraryFocus
    }
    val internalSearchInputFocus = remember { FocusRequester() }
    val searchInputFocus = if (hasMultipleLibraries) {
        internalSearchInputFocus
    } else {
        libraryEntryFocus
    }
    val restoreTargetId = restoreMediaId ?: state.focusedMediaId

    // Source changes refresh content in-place and must not replay root-entry focus.
    // Returning from detail restores its card before applying root entry focus.
    LaunchedEffect(Unit) {
        if (requestInitialFocus && restoreTargetId == null) {
            if (hasMultipleLibraries) {
                firstLibraryFocus.requestFocus()
            } else {
                searchInputFocus.requestFocus()
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(BrowseLayoutTokens.PaneSpacing),
    ) {
        LibrarySidebar(
            libraries = state.libraries,
            selectedLibraryId = state.selectedLibraryId,
            selectedLibraryIndex = selectedLibraryIndex,
            sidebarFocus = libraryEntryFocus.takeIf { hasMultipleLibraries },
            firstLibraryFocus = firstLibraryFocus,
            selectedLibraryFocus = selectedLibraryFocus,
            menuItemsAreFocusable = hasMultipleLibraries,
            topNavigationFocusRequester = topNavigationFocusRequester,
            onSelectLibrary = onSelectLibrary,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag("library-content"),
        ) {
            LibrarySearch(
                value = state.query,
                inputFocusRequester = searchInputFocus,
                topNavigationFocusRequester = topNavigationFocusRequester,
                onValueChange = onQueryChange,
                onSearch = onSearch,
            )
            Spacer(Modifier.height(BrowseLayoutTokens.HeaderContentSpacing))
            LibraryItems(
                session = session,
                state = state.items,
                restoreMediaId = restoreTargetId,
                requestInitialFocus = requestInitialFocus,
                gridViewport = state.gridViewport,
                onRetry = onRetry,
                onLoadMore = onLoadMore,
                onMediaFocused = onMediaFocused,
                onGridViewportChanged = onGridViewportChanged,
                onOpenMedia = onOpenMedia,
            )
        }
    }
}

@Composable
private fun LibrarySidebar(
    libraries: List<MediaLibrary>,
    selectedLibraryId: Long,
    selectedLibraryIndex: Int,
    sidebarFocus: FocusRequester?,
    firstLibraryFocus: FocusRequester,
    selectedLibraryFocus: FocusRequester,
    menuItemsAreFocusable: Boolean,
    topNavigationFocusRequester: FocusRequester?,
    onSelectLibrary: (Long) -> Unit,
) {
    val firstLibraryId = libraries.firstOrNull()?.id
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
                        requestedFocusDirection == FocusDirection.Down &&
                        selectedLibraryIndex >= 0 &&
                        menuItemsAreFocusable
                    ) {
                        cancelFocusChange()
                        focusEntryJob?.cancel()
                        focusEntryJob = focusScope.launch {
                            val targetIsVisible = listState.layoutInfo
                                .visibleItemsInfo
                                .any { it.index == selectedLibraryIndex }
                            if (!targetIsVisible) {
                                listState.scrollToItem(selectedLibraryIndex)
                            }
                            withFrameNanos { }
                            selectedLibraryFocus.requestFocus()
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
        items(
            items = libraries,
            key = MediaLibrary::id,
        ) { library ->
            val isFirstLibrary = library.id == firstLibraryId
            val isSelectedLibrary = library.id == selectedLibraryId
            KaloscopeButton(
                onClick = { onSelectLibrary(library.id) },
                selected = library.id == selectedLibraryId,
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
                    .testTag("library-sidebar-item-${library.id}")
                    .focusProperties { canFocus = menuItemsAreFocusable }
                    .then(
                        when {
                            isFirstLibrary -> Modifier.focusRequester(firstLibraryFocus)
                            isSelectedLibrary -> Modifier.focusRequester(selectedLibraryFocus)
                            else -> Modifier
                        },
                    )
                    .then(
                        if (isFirstLibrary) {
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
                    KaloscopeNavigationIcon(
                        iconRes = library.type.iconResource(),
                        modifier = Modifier.testTag(library.type.iconTestTag()),
                    )
                    Spacer(Modifier.width(BrowseLayoutTokens.SidebarIconTextSpacing))
                    Text(
                        text = library.name,
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

@DrawableRes
private fun MediaLibraryType.iconResource(): Int =
    when (this) {
        MediaLibraryType.Movie -> R.drawable.ic_library_movie
        MediaLibraryType.TvShow -> R.drawable.ic_library_tv_show
        MediaLibraryType.Unknown -> R.drawable.ic_library_unknown
    }

private fun MediaLibraryType.iconTestTag(): String =
    when (this) {
        MediaLibraryType.Movie -> "library-type-icon-movie"
        MediaLibraryType.TvShow -> "library-type-icon-tv-show"
        MediaLibraryType.Unknown -> "library-type-icon-unknown"
    }

@Composable
private fun LibrarySearch(
    value: String,
    inputFocusRequester: FocusRequester,
    topNavigationFocusRequester: FocusRequester?,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val searchActionFocus = remember { FocusRequester() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvSearchField(
            value = value,
            hint = stringResource(R.string.search_library_hint),
            onValueChange = onValueChange,
            onSearch = onSearch,
            focusRequester = inputFocusRequester,
            onMoveUp = topNavigationFocusRequester?.let { requester ->
                { requester.requestFocus() }
            },
            onMoveRight = searchActionFocus::requestFocus,
            modifier = Modifier
                .weight(1f)
                .height(BrowseLayoutTokens.SearchControlHeight)
                .focusProperties { right = searchActionFocus }
                .testTag("library-search-input"),
        )
        KaloscopeIconButton(
            onClick = onSearch,
            modifier = Modifier
                .size(BrowseLayoutTokens.SearchControlHeight)
                .focusRequester(searchActionFocus)
                .focusProperties {
                    topNavigationFocusRequester?.let { up = it }
                }
                .testTag("library-search-action-button"),
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_search),
                contentDescription = stringResource(R.string.search_action),
                modifier = Modifier
                    .size(24.dp)
                    .testTag("library-search-action-icon"),
            )
        }
    }
}

@Composable
private fun LibraryItems(
    session: Session,
    state: LibraryItemsState,
    restoreMediaId: Long?,
    requestInitialFocus: Boolean,
    gridViewport: GridViewportSnapshot,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onMediaFocused: (Long) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit,
    onOpenMedia: (Long) -> Unit,
) {
    when (state) {
        LibraryItemsState.Loading -> KaloscopeLoadingLayout("library-items-loading")

        LibraryItemsState.Empty -> LibraryStatus(
            title = stringResource(R.string.no_media),
            description = stringResource(R.string.no_media_description),
        )

        is LibraryItemsState.Error -> LibraryError(
            error = state.error,
            onRetry = onRetry,
        )

        is LibraryItemsState.Content -> {
            val restoreIndex = gridViewport.firstVisibleItemIndex
                .coerceIn(0, state.items.lastIndex.coerceAtLeast(0))
            val resolvedRestoreMediaId = restoreMediaId?.let { focusedId ->
                focusedId.takeIf { id -> state.items.any { it.id == id } }
                    ?: state.items.getOrNull(restoreIndex)?.id
            }
            val gridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = restoreIndex,
                initialFirstVisibleItemScrollOffset =
                    gridViewport.firstVisibleItemScrollOffset,
            )
            var lastPrefetchedPage by remember { mutableIntStateOf(-1) }
            LaunchedEffect(gridState, state.items.size) {
                snapshotFlow {
                    GridViewportSnapshot(
                        firstVisibleItemIndex = gridState.firstVisibleItemIndex
                            .coerceAtMost(state.items.lastIndex.coerceAtLeast(0)),
                        firstVisibleItemScrollOffset =
                            gridState.firstVisibleItemScrollOffset.coerceAtLeast(0),
                    )
                }.distinctUntilChanged().collect(onGridViewportChanged)
            }
            Column(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(
                        minSize = BrowseLayoutTokens.PortraitGridMinWidth,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library-results-grid"),
                    contentPadding = PaddingValues(
                        start = BrowseLayoutTokens.GridHorizontalContentPadding,
                        top = BrowseLayoutTokens.GridTopContentPadding,
                        end = BrowseLayoutTokens.GridHorizontalContentPadding,
                        bottom = BrowseLayoutTokens.GridBottomContentPadding,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(
                        BrowseLayoutTokens.GridHorizontalSpacing,
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        BrowseLayoutTokens.GridVerticalSpacing,
                    ),
                ) {
                    itemsIndexed(
                        items = state.items,
                        key = { _, media -> media.id },
                    ) { mediaIndex, media ->
                        MediaCard(
                            session = session,
                            media = media,
                            restoreFocus =
                                requestInitialFocus && media.id == resolvedRestoreMediaId,
                            onFocused = {
                                onMediaFocused(media.id)
                                if (
                                    lastPrefetchedPage != state.pageNumber &&
                                    shouldPrefetchGridItem(
                                        focusedItemIndex = mediaIndex,
                                        itemCount = state.items.size,
                                        columnCount = gridState.layoutInfo.maxSpan,
                                        hasNext = state.hasNext,
                                        isLoadingMore = state.isLoadingMore,
                                        hasLoadMoreError = state.loadMoreError != null,
                                    )
                                ) {
                                    lastPrefetchedPage = state.pageNumber
                                    onLoadMore()
                                }
                            },
                            onClick = { onOpenMedia(media.id) },
                        )
                    }
                    if (state.hasNext && state.isLoadingMore) {
                        item(
                            key = "library-load-more-loading",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Text(
                                text = stringResource(R.string.loading_more),
                                color = Muted,
                                fontSize = 14.sp,
                                modifier = Modifier.testTag("library-load-more-loading"),
                            )
                        }
                    }
                    if (
                        state.hasNext &&
                        !state.isLoadingMore &&
                        state.loadMoreError != null
                    ) {
                        item(
                            key = "library-load-more-retry",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Column {
                                Text(
                                    text = appErrorText(state.loadMoreError),
                                    color = Danger,
                                    fontSize = 14.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                KaloscopeButton(
                                    onClick = onLoadMore,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("library-load-more-retry"),
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
private fun MediaCard(
    session: Session,
    media: MediaSummary,
    restoreFocus: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val focusRequester = remember(media.id) { FocusRequester() }
    LaunchedEffect(restoreFocus) {
        if (restoreFocus) {
            // Navigation has attached the returning card by the next frame.
            withFrameNanos { }
            focusRequester.requestFocus()
        }
    }
    KaloscopeFocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        focusedContainerColor = ContentCardFocused,
        focusScale = 1.03f,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = BrowseLayoutTokens.GridCardFocusBottomPadding)
            .testTag("media-card-${media.id}")
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    onFocused()
                }
            }
            .focusSafeBottomPadding(BrowseLayoutTokens.GridCardFocusBottomPadding)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
        ) {
            Box {
                ServerImage(
                    session = session,
                    rawValue = media.posterPath,
                    contentDescription = null,
                    policy = ServerImagePolicy.Store,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(11.dp)),
                )
                RatingDisplayPolicy.format(media.rating)?.let { rating ->
                    RatingBadge(
                        rating = rating,
                        testTag = "media-rating-${media.id}",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(7.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = media.title,
                color = OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("media-title-${media.id}"),
            )
            Text(
                text = media.year?.toString().orEmpty(),
                color = Muted,
                fontSize = 12.sp,
                minLines = 1,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("media-year-${media.id}"),
            )
        }
    }
}

@Composable
private fun LibraryStatus(
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
            Text(
                text = title,
                color = OnBackground,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                color = Muted,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun LibraryError(
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
            text = stringResource(R.string.library_load_failed),
            color = OnBackground,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = appErrorText(error),
            color = Danger,
            fontSize = 16.sp,
        )
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
