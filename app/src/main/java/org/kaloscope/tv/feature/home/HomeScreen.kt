package org.kaloscope.tv.feature.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.Primary
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
            Column {
                Text(
                    text = stringResource(R.string.home),
                    color = OnBackground,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.recent_watch_description),
                    color = Muted,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.colors(
                    containerColor = Color(0xFF202738),
                    focusedContainerColor = Primary,
                ),
            ) {
                Text(stringResource(R.string.refresh))
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
    val featured = items.first()
    FeaturedHistoryCard(
        session = session,
        item = featured,
        restoreFocus = featured.mediaId == restoreMediaId,
        onOpenMedia = onOpenMedia,
        onPlayHistory = onPlayHistory,
    )
    if (items.size > 1) {
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.more_history),
            color = OnBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(
                items = items.drop(1),
                key = WatchHistoryItem::historyId,
            ) { item ->
                CompactHistoryCard(
                    session = session,
                    item = item,
                    restoreFocus = item.mediaId == restoreMediaId,
                    onPlayHistory = onPlayHistory,
                )
            }
        }
    }
}

@Composable
private fun FeaturedHistoryCard(
    session: Session,
    item: WatchHistoryItem,
    restoreFocus: Boolean,
    onOpenMedia: (Long) -> Unit,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    val detailFocus = remember(item.mediaId) { FocusRequester() }
    LaunchedEffect(restoreFocus) {
        if (restoreFocus) {
            withFrameNanos { }
            detailFocus.requestFocus()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(HomeCard)
            .testTag("home-hero"),
    ) {
        ServerImage(
            session = session,
            rawValue = item.backdropPath ?: item.posterPath,
            fallbackText = item.title,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xFA070B14),
                        0.53f to Color(0xCC070B14),
                        1f to Color(0x26070B14),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.66f)
                .padding(horizontal = 34.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.continue_watching),
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.title,
                color = OnBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            HistoryMetadata(item)
            Spacer(Modifier.height(24.dp))
            ProgressBar(item.percentage)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.watched_percent, item.percentage),
                color = Muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onPlayHistory(item) },
                    modifier = Modifier.focusRequester(detailFocus),
                    colors = ButtonDefaults.colors(focusedContainerColor = Primary),
                ) {
                    Text(stringResource(R.string.resume_playback))
                }
                Button(
                    onClick = { onOpenMedia(item.mediaId) },
                    colors = ButtonDefaults.colors(focusedContainerColor = Primary),
                ) {
                    Text(stringResource(R.string.view_detail))
                }
            }
        }
    }
}

@Composable
private fun CompactHistoryCard(
    session: Session,
    item: WatchHistoryItem,
    restoreFocus: Boolean,
    onPlayHistory: (WatchHistoryItem) -> Unit,
) {
    val cardFocus = remember(item.mediaId) { FocusRequester() }
    LaunchedEffect(restoreFocus) {
        if (restoreFocus) {
            withFrameNanos { }
            cardFocus.requestFocus()
        }
    }
    KaloscopeFocusSurface(
        onClick = { onPlayHistory(item) },
        shape = RoundedCornerShape(16.dp),
        containerColor = Panel.copy(alpha = 0.72f),
        focusedContainerColor = PanelElevated,
        focusScale = 1.04f,
        modifier = Modifier
            .width(258.dp)
            .focusRequester(cardFocus),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            ServerImage(
                session = session,
                rawValue = item.backdropPath ?: item.posterPath,
                fallbackText = item.title,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.title,
                color = OnBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            ProgressBar(item.percentage)
        }
    }
}

@Composable
private fun HomeEmpty(onOpenLibrary: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Panel, RoundedCornerShape(20.dp))
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
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.no_history_description),
            color = Muted,
            fontSize = 17.sp,
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

@Composable
private fun HistoryMetadata(item: WatchHistoryItem) {
    val metadata = listOfNotNull(
        item.year?.toString(),
        item.season?.let { season ->
            item.episode?.let { episode ->
                stringResource(R.string.season_episode, season, episode)
            }
        },
        item.rating?.let { stringResource(R.string.rating, it) },
    ).joinToString("  ·  ")
    if (metadata.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = metadata,
            color = Muted,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun ProgressBar(percentage: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
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
