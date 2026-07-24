package org.kaloscope.tv.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaSummary
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
) {
    when (state) {
        LibraryUiState.Loading -> LibraryStatus(
            title = stringResource(R.string.loading_libraries),
            description = stringResource(R.string.loading_libraries_description),
        )

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
    onOpenMedia: (Long) -> Unit,
) {
    val firstLibraryFocus = remember { FocusRequester() }

    // Entering the root starts at the first source; returning from detail restores its card.
    LaunchedEffect(state.selectedLibraryId, restoreMediaId) {
        if (restoreMediaId == null) {
            firstLibraryFocus.requestFocus()
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        LibrarySidebar(
            libraries = state.libraries,
            selectedLibraryId = state.selectedLibraryId,
            firstLibraryFocus = firstLibraryFocus,
            onSelectLibrary = onSelectLibrary,
        )
        Column(modifier = Modifier.weight(1f)) {
            LibrarySearch(
                value = state.query,
                onValueChange = onQueryChange,
                onSearch = onSearch,
            )
            Spacer(Modifier.height(22.dp))
            LibraryItems(
                session = session,
                state = state.items,
                restoreMediaId = restoreMediaId,
                onRetry = onRetry,
                onLoadMore = onLoadMore,
                onMediaFocused = onMediaFocused,
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
            .width(250.dp)
            .fillMaxHeight()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = libraries,
            key = MediaLibrary::id,
        ) { library ->
            Surface(
                selected = library.id == selectedLibraryId,
                onClick = { onSelectLibrary(library.id) },
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
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .background(Color(0xFF111725), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF343D54), RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            textStyle = TextStyle(
                color = OnBackground,
                fontSize = 16.sp,
            ),
            singleLine = true,
            decorationBox = { innerField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            text = stringResource(R.string.search_library_hint),
                            color = Muted,
                            fontSize = 16.sp,
                        )
                    }
                    innerField()
                }
            },
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
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onMediaFocused: (Long) -> Unit,
    onOpenMedia: (Long) -> Unit,
) {
    when (state) {
        LibraryItemsState.Loading -> LibraryStatus(
            title = stringResource(R.string.loading_media),
            description = stringResource(R.string.loading_media_description),
        )

        LibraryItemsState.Empty -> LibraryStatus(
            title = stringResource(R.string.no_media),
            description = stringResource(R.string.no_media_description),
        )

        is LibraryItemsState.Error -> LibraryError(
            error = state.error,
            onRetry = onRetry,
        )

        is LibraryItemsState.Content -> Column(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(
                    items = state.items,
                    key = MediaSummary::id,
                ) { media ->
                    MediaCard(
                        session = session,
                        media = media,
                        restoreFocus = media.id == restoreMediaId,
                        onFocused = { onMediaFocused(media.id) },
                        onClick = { onOpenMedia(media.id) },
                    )
                }
            }
            if (state.hasNext || state.isLoadingMore || state.loadMoreError != null) {
                Spacer(Modifier.height(12.dp))
                state.loadMoreError?.let { error ->
                    Text(
                        text = libraryErrorText(error),
                        color = Danger,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = onLoadMore,
                    enabled = !state.isLoadingMore,
                    colors = ButtonDefaults.colors(focusedContainerColor = Primary),
                ) {
                    Text(
                        if (state.isLoadingMore) {
                            stringResource(R.string.loading_more)
                        } else {
                            stringResource(R.string.load_more)
                        },
                    )
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
    Surface(
        onClick = onClick,
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
            ServerImage(
                session = session,
                rawValue = media.posterPath ?: media.backdropPath,
                fallbackText = media.title,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = media.title,
                color = OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
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
