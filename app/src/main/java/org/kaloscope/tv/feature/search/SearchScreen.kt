package org.kaloscope.tv.feature.search

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Outline
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.Session

@Composable
fun SearchScreen(
    session: Session,
    state: SearchUiState,
    onSelectIndexer: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onResultFocused: (String) -> Unit,
    onPlay: (String) -> Unit,
) {
    when (state) {
        SearchUiState.Loading -> SearchStatus(
            title = stringResource(R.string.loading_indexers),
            description = stringResource(R.string.loading_indexers_description),
        )

        SearchUiState.EmptyIndexers -> SearchStatus(
            title = stringResource(R.string.no_indexers),
            description = stringResource(R.string.no_indexers_description),
        )

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
            onPlay = onPlay,
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
    onPlay: (String) -> Unit,
) {
    val firstIndexerFocus = remember { FocusRequester() }
    LaunchedEffect(state.selectedIndexerId, state.focusedResultId) {
        if (state.focusedResultId == null) {
            firstIndexerFocus.requestFocus()
        }
    }
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
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
            when (state.source) {
                SearchSourceState.Loading -> SearchStatus(
                    title = stringResource(R.string.loading_indexer),
                    description = stringResource(R.string.loading_indexer_description),
                )

                SearchSourceState.WebAuthRequired -> SearchAuthRequired(
                    title = stringResource(R.string.indexer_auth_required),
                    description = stringResource(R.string.indexer_auth_required_description),
                    onRetry = onRetry,
                )

                is SearchSourceState.Error -> SearchError(state.source.error, onRetry)
                is SearchSourceState.Ready -> {
                    SearchInput(
                        value = state.query,
                        onValueChange = onQueryChange,
                        onSearch = onSearch,
                    )
                    Spacer(Modifier.height(22.dp))
                    SearchResults(
                        session = session,
                        state = state,
                        onRetry = onRetry,
                        onLoadMore = onLoadMore,
                        onResultFocused = onResultFocused,
                        onPlay = onPlay,
                    )
                }
            }
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
            .width(250.dp)
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
                .background(Panel.copy(alpha = 0.88f), RoundedCornerShape(12.dp))
                .border(1.dp, Outline, RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            textStyle = TextStyle(color = OnBackground, fontSize = 16.sp),
            singleLine = true,
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            text = stringResource(R.string.search_indexer_hint),
                            color = Muted,
                            fontSize = 16.sp,
                        )
                    }
                    field()
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
private fun SearchResults(
    session: Session,
    state: SearchUiState.Content,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onResultFocused: (String) -> Unit,
    onPlay: (String) -> Unit,
) {
    when (val results = state.results) {
        SearchResultsState.AwaitingQuery -> SearchStatus(
            title = stringResource(R.string.search_indexer_prompt),
            description = stringResource(R.string.search_indexer_prompt_description),
        )

        SearchResultsState.Loading -> SearchStatus(
            title = stringResource(R.string.searching_indexer),
            description = stringResource(R.string.searching_indexer_description),
        )

        SearchResultsState.Empty -> SearchStatus(
            title = stringResource(R.string.no_search_results),
            description = stringResource(R.string.no_search_results_description),
        )

        is SearchResultsState.Error -> SearchError(results.error, onRetry)
        is SearchResultsState.Content -> Column(modifier = Modifier.fillMaxSize()) {
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
                columns = GridCells.Fixed(5),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(results.items, key = NetworkSearchResult::id) { result ->
                    NetworkResultCard(
                        session = session,
                        result = result,
                        restoreFocus = result.id == state.focusedResultId,
                        enabled = state.resolvingResultId == null,
                        resolving = result.id == state.resolvingResultId,
                        onFocused = { onResultFocused(result.id) },
                        onClick = { onPlay(result.id) },
                    )
                }
            }
            if (results.hasNext || results.isLoadingMore || results.loadMoreError != null) {
                results.loadMoreError?.let {
                    Text(searchErrorText(it), color = Danger, fontSize = 14.sp)
                }
                Button(
                    onClick = onLoadMore,
                    enabled = !results.isLoadingMore,
                    colors = ButtonDefaults.colors(focusedContainerColor = Primary),
                ) {
                    Text(
                        if (results.isLoadingMore) {
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
private fun NetworkResultCard(
    session: Session,
    result: NetworkSearchResult,
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
                    .aspectRatio(2f / 3f)
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
private fun SearchAuthRequired(
    title: String,
    description: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(28.dp),
    ) {
        Text(title, color = OnBackground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(description, color = Muted, fontSize = 16.sp)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag("indexer-auth-retry"),
            colors = ButtonDefaults.colors(focusedContainerColor = Primary),
        ) {
            Text(stringResource(R.string.retry))
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
