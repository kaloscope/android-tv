package org.kaloscope.tv.feature.library

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.shouldPrefetchGridItem
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.designsystem.RatingBadge
import org.kaloscope.tv.core.designsystem.KaloscopeGridSkeleton
import org.kaloscope.tv.core.designsystem.TvSearchField
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.RatingDisplayPolicy
import org.kaloscope.tv.core.model.Session

@Composable
fun LibraryScreen(
    session: Session,
    state: LibraryUiState,
    restoreMediaId: Long?,
    onSelectLibrary: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onMediaFocused: (Long) -> Unit,
    onOpenMedia: (Long) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit = {},
) {
    when (state) {
        LibraryUiState.Loading -> KaloscopeGridSkeleton("library-loading-skeleton")

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
    onSelectLibrary: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onMediaFocused: (Long) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit,
    onOpenMedia: (Long) -> Unit,
) {
    val firstLibraryFocus = remember { FocusRequester() }
    val restoreTargetId = restoreMediaId ?: state.focusedMediaId

    // Entering the root starts at the first source; returning from detail restores its card.
    LaunchedEffect(state.selectedLibraryId, restoreTargetId) {
        if (restoreTargetId == null) {
            firstLibraryFocus.requestFocus()
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        LibrarySidebar(
            libraries = state.libraries,
            selectedLibraryId = state.selectedLibraryId,
            firstLibraryFocus = firstLibraryFocus,
            onSelectLibrary = onSelectLibrary,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag("library-content"),
        ) {
            LibrarySearch(
                value = state.query,
                onValueChange = onQueryChange,
                onSearch = onSearch,
            )
            Spacer(Modifier.height(22.dp))
            LibraryItems(
                session = session,
                state = state.items,
                restoreMediaId = restoreTargetId,
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
    firstLibraryFocus: FocusRequester,
    onSelectLibrary: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(Panel.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = libraries,
            key = MediaLibrary::id,
        ) { library ->
            KaloscopeFocusSurface(
                onClick = { onSelectLibrary(library.id) },
                selected = library.id == selectedLibraryId,
                shape = RoundedCornerShape(12.dp),
                focusedContainerColor = PanelElevated,
                focusScale = 1.02f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .then(
                        if (library == libraries.first()) {
                            Modifier.focusRequester(firstLibraryFocus)
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
                        text = library.name.take(1),
                        color = Primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = library.name,
                        color = OnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySearch(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
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
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("library-search-input"),
        )
        Button(
            onClick = onSearch,
            colors = ButtonDefaults.colors(focusedContainerColor = Primary),
        ) {
            Text(stringResource(R.string.search_action))
        }
    }
}

@Composable
private fun LibraryItems(
    session: Session,
    state: LibraryItemsState,
    restoreMediaId: Long?,
    gridViewport: GridViewportSnapshot,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onMediaFocused: (Long) -> Unit,
    onGridViewportChanged: (GridViewportSnapshot) -> Unit,
    onOpenMedia: (Long) -> Unit,
) {
    when (state) {
        LibraryItemsState.Loading -> KaloscopeGridSkeleton("library-items-loading-skeleton")

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
                    columns = GridCells.Adaptive(minSize = 172.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library-results-grid"),
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
                        items = state.items,
                        key = { _, media -> media.id },
                    ) { mediaIndex, media ->
                        MediaCard(
                            session = session,
                            media = media,
                            restoreFocus = media.id == resolvedRestoreMediaId,
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
                                    text = libraryErrorText(state.loadMoreError),
                                    color = Danger,
                                    fontSize = 14.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = onLoadMore,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("library-load-more-retry"),
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
        containerColor = Panel.copy(alpha = 0.65f),
        focusedContainerColor = PanelElevated,
        focusScale = 1.04f,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("media-card-${media.id}")
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    onFocused()
                }
            }
            .semantics(mergeDescendants = true) {},
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box {
                ServerImage(
                    session = session,
                    rawValue = media.posterPath,
                    fallbackText = media.title,
                    contentDescription = null,
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
            Spacer(Modifier.height(9.dp))
            Text(
                text = media.title,
                color = OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            media.year?.let { year ->
                Text(
                    text = year.toString(),
                    color = Muted,
                    fontSize = 13.sp,
                )
            }
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
            text = libraryErrorText(error),
            color = Danger,
            fontSize = 16.sp,
        )
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
private fun libraryErrorText(error: AppError): String =
    when (error) {
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        AppError.Forbidden -> stringResource(R.string.error_forbidden)
        AppError.NotFound -> stringResource(R.string.error_not_found)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.Offline -> stringResource(R.string.error_offline)
        is AppError.Api -> stringResource(R.string.error_api, error.code.orEmpty())
        is AppError.InvalidData -> stringResource(R.string.error_invalid_data)
    }
