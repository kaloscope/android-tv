package org.kaloscope.tv.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.PanelSelected
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.designsystem.ServerBackdrop
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem

private val HomeDivider = Color(0xFF252D40)
private val HomeCard = Color(0xFF182132)

@Composable
internal fun HomeScreen(
    session: Session,
    state: HomeUiState,
    onRefresh: () -> Unit,
    restoreMediaId: Long?,
    onOpenLibrary: () -> Unit,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.continue_watching),
                color = OnBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("home-refresh"),
                shape = IconButtonDefaults.shape(CircleShape),
                colors = IconButtonDefaults.colors(
                    containerColor = Color(0xFF202738),
                    focusedContainerColor = Primary,
                    contentColor = OnBackground,
                    focusedContentColor = Color.White,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.refresh),
                    modifier = Modifier.size(25.dp),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        when (state) {
            HomeUiState.Loading -> StatusPanel(
                title = stringResource(R.string.loading_history),
                description = stringResource(R.string.loading_history_description),
            )

            HomeUiState.Empty -> HomeEmpty(onOpenLibrary)
            is HomeUiState.Error -> ErrorPanel(state.error, onRefresh)
            is HomeUiState.Content -> HistoryContent(
                session = session,
                items = state.items,
                restoreMediaId = restoreMediaId,
                onOpenMedia = onOpenMedia,
                onPlayHistory = onPlayHistory,
            )
        }
    }
}

@Composable
private fun HistoryContent(
    session: Session,
    items: List<WatchHistoryItem>,
    restoreMediaId: Long?,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    var selectedMediaId by remember { mutableStateOf<Long?>(null) }
    val selectedItem = items.firstOrNull { it.mediaId == selectedMediaId }
        ?: items.first()
    val listState = rememberLazyListState()
    val actionFocusRequester = remember { FocusRequester() }
    val selectedCardFocusRequester = remember { FocusRequester() }

    LaunchedEffect(items, restoreMediaId) {
        val restored = restoreMediaId?.let { mediaId ->
            items.firstOrNull { it.mediaId == mediaId }
        }
        val retained = items.firstOrNull { it.mediaId == selectedMediaId }
        selectedMediaId = (restored ?: retained ?: items.first()).mediaId
    }
    LaunchedEffect(selectedItem.mediaId) {
        val index = items.indexOfFirst { it.mediaId == selectedItem.mediaId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }
    LaunchedEffect(restoreMediaId, selectedItem.mediaId) {
        if (restoreMediaId == selectedItem.mediaId) {
            withFrameNanos { }
            selectedCardFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(HomeCard)
            .testTag("home-hero"),
    ) {
        ServerBackdrop(
            session = session,
            backdropPath = selectedItem.backdropPath ?: selectedItem.posterPath,
            title = selectedItem.parentTitle ?: selectedItem.title,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xFA070B14),
                        0.58f to Color(0xD9070B14),
                        1f to Color(0x38070B14),
                    ),
                )
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x18070B14),
                        0.64f to Color(0x52070B14),
                        1f to Color(0xF5070B14),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 34.dp, vertical = 18.dp),
        ) {
            SelectedHistoryDetails(
                item = selectedItem,
                onOpenMedia = onOpenMedia,
                onPlayHistory = onPlayHistory,
                actionFocusRequester = actionFocusRequester,
                selectedCardFocusRequester = selectedCardFocusRequester,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.58f),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .testTag("history-carousel"),
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.historyId },
                ) { _, item ->
                    HistoryCarouselCard(
                        session = session,
                        item = item,
                        selected = item.mediaId == selectedItem.mediaId,
                        actionFocusRequester = actionFocusRequester,
                        selectedCardFocusRequester = selectedCardFocusRequester,
                        onFocused = { selectedMediaId = item.mediaId },
                        onPlayHistory = onPlayHistory,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedHistoryDetails(
    item: WatchHistoryItem,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
    actionFocusRequester: FocusRequester,
    selectedCardFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = item.parentTitle ?: item.title,
            color = OnBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("history-selected-title"),
        )
        historyEpisodeText(item)?.let { episodeText ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = episodeText,
                color = Muted,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HistoryMetadata(item)
        Spacer(Modifier.height(10.dp))
        ProgressBar(item.percentage)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onPlayHistory(item) },
                modifier = Modifier
                    .focusRequester(actionFocusRequester)
                    .focusProperties { down = selectedCardFocusRequester },
                colors = ButtonDefaults.colors(focusedContainerColor = Primary),
            ) {
                Text(stringResource(R.string.resume_playback))
            }
            Button(
                onClick = { onOpenMedia(item.mediaId) },
                modifier = Modifier.focusProperties { down = selectedCardFocusRequester },
                colors = ButtonDefaults.colors(focusedContainerColor = Primary),
            ) {
                Text(stringResource(R.string.view_detail))
            }
        }
    }
}

@Composable
private fun HistoryCarouselCard(
    session: Session,
    item: WatchHistoryItem,
    selected: Boolean,
    actionFocusRequester: FocusRequester,
    selectedCardFocusRequester: FocusRequester,
    onFocused: () -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val restingColor = if (selected) PanelSelected else Panel.copy(alpha = 0.82f)
    Surface(
        onClick = { onPlayHistory(item) },
        modifier = Modifier
            .width(284.dp)
            .fillMaxHeight()
            .focusProperties { up = actionFocusRequester }
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocused()
                }
            }
            .then(
                if (selected) {
                    Modifier.focusRequester(selectedCardFocusRequester)
                } else {
                    Modifier
                },
            )
            .testTag("history-card-${item.mediaId}")
            .semantics { this.selected = selected },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = restingColor,
            focusedContainerColor = PanelElevated,
            contentColor = OnBackground,
            focusedContentColor = OnBackground,
            disabledContainerColor = restingColor.copy(alpha = 0.45f),
            disabledContentColor = Muted,
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.015f,
            disabledScale = 1f,
            focusedDisabledScale = 1f,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    1.dp,
                    if (selected) Primary.copy(alpha = 0.7f) else Color.Transparent,
                ),
                shape = shape,
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape,
            ),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServerImage(
                session = session,
                rawValue = item.posterPath,
                fallbackText = item.parentTitle ?: item.title,
                contentDescription = item.parentTitle ?: item.title,
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.parentTitle ?: item.title,
                    color = OnBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                historyEpisodeText(item)?.let { episodeText ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = episodeText,
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(7.dp))
                ProgressBar(item.percentage)
            }
        }
    }
}

@Composable
private fun HomeEmpty(onOpenLibrary: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Panel, RoundedCornerShape(20.dp)),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_empty_inbox),
            contentDescription = null,
            tint = Muted,
            modifier = Modifier
                .align(Alignment.Center)
                .size(120.dp)
                .alpha(0.1f),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(34.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.no_history),
                color = OnBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onOpenLibrary,
                colors = ButtonDefaults.colors(focusedContainerColor = Primary),
            ) {
                Text(stringResource(R.string.open_library))
            }
        }
    }
}

@Composable
private fun HistoryMetadata(item: WatchHistoryItem) {
    val metadata = listOfNotNull(
        item.year?.toString(),
        item.rating?.let { stringResource(R.string.rating, it) },
    ).joinToString("  ·  ")
    if (metadata.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = metadata,
            color = Muted,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun historyEpisodeText(item: WatchHistoryItem): String? {
    if (item.parentTitle == null) {
        return null
    }
    val season = item.season
    val episode = item.episode
    return if (season != null && episode != null) {
        stringResource(R.string.history_episode_title, season, episode, item.title)
    } else {
        item.title
    }
}

@Composable
private fun ProgressBar(percentage: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(HomeDivider, RoundedCornerShape(4.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percentage.coerceIn(0, 100) / 100f)
                .fillMaxHeight()
                .background(Primary, RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun StatusPanel(
    title: String,
    description: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Panel, RoundedCornerShape(20.dp))
            .padding(34.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                text = title,
                color = OnBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = description,
                color = Muted,
                fontSize = 17.sp,
            )
        }
    }
}

@Composable
private fun ErrorPanel(
    error: AppError,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(20.dp))
            .padding(34.dp),
    ) {
        Text(
            text = stringResource(R.string.history_load_failed),
            color = OnBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = historyErrorText(error),
            color = Danger,
            fontSize = 17.sp,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.colors(focusedContainerColor = Primary),
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun historyErrorText(error: AppError): String =
    when (error) {
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        AppError.Forbidden -> stringResource(R.string.error_forbidden)
        AppError.NotFound -> stringResource(R.string.error_not_found)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.Offline -> stringResource(R.string.error_offline)
        is AppError.Api -> stringResource(R.string.error_api, error.code.orEmpty())
        is AppError.InvalidData -> stringResource(R.string.error_invalid_data)
    }
