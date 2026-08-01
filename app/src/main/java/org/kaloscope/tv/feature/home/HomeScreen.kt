package org.kaloscope.tv.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Outline
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.designsystem.Subtle
import org.kaloscope.tv.core.designsystem.appErrorText
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.network.ServerImagePolicy

private val HomeDivider = Color(0xFF252D40)

internal data class HomeBackdropPresentation(
    val path: String,
    val title: String,
)

@Composable
internal fun HomeScreen(
    session: Session,
    state: HomeUiState,
    onRefresh: () -> Unit,
    restoreMediaId: Long?,
    topNavigationFocusRequester: FocusRequester? = null,
    onOpenLibrary: () -> Unit,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
    onBackdropChanged: (HomeBackdropPresentation?) -> Unit = {},
) {
    val refreshFocusRequester = remember { FocusRequester() }
    val currentOnBackdropChanged by rememberUpdatedState(onBackdropChanged)
    DisposableEffect(Unit) {
        onDispose {
            currentOnBackdropChanged(null)
        }
    }
    LaunchedEffect(state) {
        if (state !is HomeUiState.Content) {
            onBackdropChanged(null)
        }
    }

    when (state) {
        HomeUiState.Loading -> KaloscopeLoadingLayout("home-loading")
        else -> Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.continue_watching),
                    color = Muted,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                KaloscopeIconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(32.dp)
                        .focusRequester(refreshFocusRequester)
                        .focusProperties {
                            topNavigationFocusRequester?.let { up = it }
                        }
                        .testTag("home-refresh"),
                    variant = KaloscopeControlVariant.Filled,
                    size = KaloscopeControlSize.Compact,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.refresh),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            when (state) {
                HomeUiState.Loading -> Unit

                HomeUiState.Empty -> HomeEmpty(
                    refreshFocusRequester = refreshFocusRequester,
                    onOpenLibrary = onOpenLibrary,
                )

                is HomeUiState.Error -> ErrorPanel(
                    error = state.error,
                    refreshFocusRequester = refreshFocusRequester,
                    onRetry = onRefresh,
                )

                is HomeUiState.Content -> HistoryContent(
                    session = session,
                    items = state.items,
                    restoreMediaId = restoreMediaId,
                    refreshFocusRequester = refreshFocusRequester,
                    onOpenMedia = onOpenMedia,
                    onPlayHistory = onPlayHistory,
                    onBackdropChanged = onBackdropChanged,
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(
    session: Session,
    items: List<WatchHistoryItem>,
    restoreMediaId: Long?,
    refreshFocusRequester: FocusRequester,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
    onBackdropChanged: (HomeBackdropPresentation?) -> Unit,
) {
    var selectedMediaId by remember { mutableStateOf<Long?>(null) }
    val selectedItem = items.firstOrNull { it.mediaId == selectedMediaId }
        ?: items.first()
    val listState = rememberLazyListState()
    val canScrollBackward by remember {
        derivedStateOf { listState.canScrollBackward }
    }
    val canScrollForward by remember {
        derivedStateOf { listState.canScrollForward }
    }
    val actionFocusRequester = remember { FocusRequester() }
    val selectedCardFocusRequester = remember { FocusRequester() }
    val carouselEdgeOffset = with(LocalDensity.current) { 48.dp.roundToPx() }

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
            listState.animateScrollToItem(
                index = index,
                scrollOffset = -carouselEdgeOffset,
            )
        }
    }
    LaunchedEffect(
        session.server.id,
        selectedItem.mediaId,
        selectedItem.backdropPath,
        selectedItem.posterPath,
        selectedItem.parentTitle,
        selectedItem.title,
    ) {
        val backdropPath = selectedItem.backdropPath
            ?.takeIf { it.isNotBlank() }
            ?: selectedItem.posterPath?.takeIf { it.isNotBlank() }
        onBackdropChanged(
            backdropPath?.let { path ->
                HomeBackdropPresentation(
                    path = path,
                    title = selectedItem.parentTitle ?: selectedItem.title,
                )
            },
        )
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
            .testTag("home-content"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
        ) {
            SelectedHistoryDetails(
                item = selectedItem,
                onOpenMedia = onOpenMedia,
                onPlayHistory = onPlayHistory,
                actionFocusRequester = actionFocusRequester,
                selectedCardFocusRequester = selectedCardFocusRequester,
                refreshFocusRequester = refreshFocusRequester,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(0.58f),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(94.dp),
            ) {
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("history-carousel"),
                ) {
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize(),
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
                    if (canScrollBackward) {
                        CarouselEdgeFade(start = true)
                    }
                    if (canScrollForward) {
                        CarouselEdgeFade(start = false)
                    }
                }
                Spacer(Modifier.width(10.dp))
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
    refreshFocusRequester: FocusRequester,
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
            lineHeight = 38.sp,
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
        ProgressBar(
            percentage = item.percentage,
            modifier = Modifier.testTag("history-progress"),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KaloscopeButton(
                onClick = { onPlayHistory(item) },
                modifier = Modifier
                    .height(42.dp)
                    .focusRequester(actionFocusRequester)
                    .focusProperties {
                        up = refreshFocusRequester
                        down = selectedCardFocusRequester
                    },
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_play),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("history-resume-icon"),
                )
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.resume_playback))
            }
            KaloscopeButton(
                onClick = { onOpenMedia(item.mediaId) },
                modifier = Modifier
                    .height(42.dp)
                    .focusProperties {
                        up = refreshFocusRequester
                        down = selectedCardFocusRequester
                    },
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
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
    val restingColor = PanelElevated
    Surface(
        onClick = { onPlayHistory(item) },
        modifier = Modifier
            .width(284.dp)
            .height(86.dp)
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
                border = BorderStroke(1.dp, Outline),
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
                policy = ServerImagePolicy.Store,
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .testTag("history-card-poster-${item.mediaId}")
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
                formatHistoryUpdatedAt(
                    value = item.updatedAt,
                    todayLabel = stringResource(R.string.history_today),
                    yesterdayLabel = stringResource(R.string.history_yesterday),
                )?.let { updatedAt ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(
                            R.string.history_card_progress,
                            updatedAt,
                            item.percentage.coerceIn(0, 100),
                        ),
                        color = Subtle,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CarouselEdgeFade(start: Boolean) {
    val fadeWidth = if (start) 48.dp else 112.dp
    val fadeBrush = if (start) {
        Brush.horizontalGradient(
            colors = listOf(Background, Color.Transparent),
        )
    } else {
        Brush.horizontalGradient(
            0f to Color.Transparent,
            0.42f to Background.copy(alpha = 0.2f),
            0.72f to Background.copy(alpha = 0.72f),
            1f to Background,
        )
    }
    Box(
        modifier = Modifier
            .align(if (start) Alignment.CenterStart else Alignment.CenterEnd)
            .width(fadeWidth)
            .fillMaxHeight()
            .background(fadeBrush)
            .testTag(
                if (start) {
                    "history-carousel-start-fade"
                } else {
                    "history-carousel-end-fade"
                },
            ),
    )
}

@Composable
private fun HomeEmpty(
    refreshFocusRequester: FocusRequester,
    onOpenLibrary: () -> Unit,
) {
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
            KaloscopeButton(
                onClick = onOpenLibrary,
                modifier = Modifier.focusProperties {
                    up = refreshFocusRequester
                },
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
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
        stringResource(
            R.string.history_watched_percentage,
            item.percentage.coerceIn(0, 100),
        ),
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
private fun ProgressBar(
    percentage: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
    refreshFocusRequester: FocusRequester,
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
            text = appErrorText(error),
            color = Danger,
            fontSize = 17.sp,
        )
        Spacer(Modifier.height(20.dp))
        KaloscopeButton(
            onClick = onRetry,
            modifier = Modifier.focusProperties {
                up = refreshFocusRequester
            },
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}
