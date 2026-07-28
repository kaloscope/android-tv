package org.kaloscope.tv.feature.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeDetailSkeleton
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.designsystem.ServerBackdrop
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
fun MediaDetailScreen(
    session: Session,
    state: MediaDetailUiState,
    resumePositionSeconds: Long?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectChild: (Long) -> Unit,
    onPlay: (MediaDetail, Long?) -> Unit,
) {
    KaloscopeBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 44.dp, vertical = 30.dp),
        ) {
            when (state) {
                MediaDetailUiState.Loading -> KaloscopeDetailSkeleton()

                is MediaDetailUiState.Error -> DetailError(
                    error = state.error,
                    onRetry = onRetry,
                )

                is MediaDetailUiState.Content -> DetailContent(
                    session = session,
                    state = state,
                    resumePositionSeconds = resumePositionSeconds,
                    onBack = onBack,
                    onSelectChild = onSelectChild,
                    onPlay = onPlay,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    session: Session,
    state: MediaDetailUiState.Content,
    resumePositionSeconds: Long?,
    onBack: () -> Unit,
    onSelectChild: (Long) -> Unit,
    onPlay: (MediaDetail, Long?) -> Unit,
) {
    val backFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val firstChildFocus = remember { FocusRequester() }
    val displayed = state.selectedChild ?: state.parent

    LaunchedEffect(state.parent.id) {
        if (state.parent.children.isEmpty()) {
            playFocus.requestFocus()
        } else {
            firstChildFocus.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("detail-cinematic-surface"),
    ) {
        ServerBackdrop(
            session = session,
            backdropPath = displayed.backdropPath,
            title = displayed.title,
            policy = ServerImagePolicy.Store,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xFA060912),
                        0.62f to Color(0xE6060912),
                        1f to Color(0x99060912),
                    ),
                ),
        )
        Column(modifier = Modifier.fillMaxSize()) {
        DetailBackButton(
            onBack = onBack,
            modifier = Modifier.focusRequester(backFocus),
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            ServerImage(
                session = session,
                rawValue = displayed.posterPath,
                fallbackText = displayed.title,
                contentDescription = null,
                policy = ServerImagePolicy.Store,
                modifier = Modifier
                    .width(250.dp)
                    .aspectRatio(2f / 3f),
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text(
                        text = displayed.title,
                        color = OnBackground,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    DetailMetadata(displayed)
                    if (state.parent.children.isEmpty() || state.selectedChild != null) {
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (resumePositionSeconds != null && resumePositionSeconds > 0) {
                                KaloscopeButton(
                                    onClick = { onPlay(displayed, resumePositionSeconds) },
                                    modifier = Modifier.focusRequester(playFocus),
                                    variant = KaloscopeControlVariant.Filled,
                                    size = KaloscopeControlSize.Compact,
                                ) {
                                    Text(stringResource(R.string.resume_playback))
                                }
                                KaloscopeButton(
                                    onClick = { onPlay(displayed, null) },
                                    variant = KaloscopeControlVariant.Filled,
                                    size = KaloscopeControlSize.Compact,
                                ) {
                                    Text(stringResource(R.string.play_from_start))
                                }
                            } else {
                                KaloscopeButton(
                                    onClick = { onPlay(displayed, null) },
                                    modifier = Modifier.focusRequester(playFocus),
                                    variant = KaloscopeControlVariant.Filled,
                                    size = KaloscopeControlSize.Compact,
                                ) {
                                    Text(stringResource(R.string.play))
                                }
                            }
                        }
                    }
                    displayed.plot?.takeIf(String::isNotBlank)?.let { plot ->
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = plot,
                            color = OnBackground,
                            fontSize = 17.sp,
                            lineHeight = 26.sp,
                        )
                    }
                    DetailCredits(displayed)
                    CastStrip(
                        session = session,
                        actors = displayed.actors,
                    )
                    if (state.parent.children.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.episodes),
                            color = OnBackground,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(
                                items = state.parent.children,
                                key = MediaSummary::id,
                            ) { child ->
                                EpisodeCard(
                                    session = session,
                                    episode = child,
                                    selected = state.selectedChild?.id == child.id,
                                    loading = state.loadingChildId == child.id,
                                    onClick = { onSelectChild(child.id) },
                                    modifier = if (child == state.parent.children.first()) {
                                        Modifier.focusRequester(firstChildFocus)
                                    } else {
                                        Modifier
                                    },
                                )
                            }
                        }
                        state.childError?.let { error ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = detailErrorText(error),
                                color = Danger,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun DetailBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.back)
    KaloscopeIconButton(
        onClick = onBack,
        variant = KaloscopeControlVariant.Filled,
        size = KaloscopeControlSize.Compact,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .width(54.dp)
            .height(46.dp)
            .semantics { contentDescription = label },
    ) {
        Text(
            text = "←",
            fontSize = 23.sp,
        )
    }
}

@Composable
private fun DetailMetadata(detail: MediaDetail) {
    val metadata = listOfNotNull(
        detail.year?.toString(),
        detail.season?.let { season ->
            detail.episode?.let { episode ->
                stringResource(R.string.season_episode, season, episode)
            }
        },
        detail.rating?.let { stringResource(R.string.rating, it) },
        detail.aired,
    ).joinToString("  ·  ")
    if (metadata.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = metadata,
            color = Muted,
            fontSize = 16.sp,
        )
    }
    if (detail.genres.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = detail.genres.joinToString("  ·  "),
            color = Primary,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun DetailCredits(detail: MediaDetail) {
    val credits = listOfNotNull(
        detail.directors.takeIf(List<String>::isNotEmpty)?.let {
            stringResource(R.string.directors, it.joinToString("、"))
        },
    )
    if (credits.isNotEmpty()) {
        Spacer(Modifier.height(18.dp))
        credits.forEach { line ->
            Text(
                text = line,
                color = Muted,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun DetailError(
    error: AppError,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(30.dp),
    ) {
        Text(
            text = stringResource(R.string.detail_load_failed),
            color = OnBackground,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = detailErrorText(error),
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

@Composable
private fun detailErrorText(error: AppError): String =
    when (error) {
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        AppError.Forbidden -> stringResource(R.string.error_forbidden)
        AppError.NotFound -> stringResource(R.string.error_not_found)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.Offline -> stringResource(R.string.error_offline)
        is AppError.Api -> stringResource(R.string.error_api, error.code.orEmpty())
        is AppError.InvalidData -> stringResource(R.string.error_invalid_data)
    }
