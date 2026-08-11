package org.kaloscope.tv.feature.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.ServerBackdrop
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
internal fun MediaDetailCinematicLayout(
    session: Session,
    parent: MediaDetail,
    focusedChild: MediaSummary?,
    initialChildId: Long?,
    childViewport: GridViewportSnapshot,
    resumePositionSeconds: Long?,
    resumePositionsByMediaId: Map<Long, Long>,
    childFocusRequester: FocusRequester,
    primaryActionFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onChildFocused: (Long) -> Unit,
    onChildViewportChanged: (GridViewportSnapshot) -> Unit,
    onResumePlayback: () -> Unit,
    onStartOverPlayback: () -> Unit,
    onPlayChild: (MediaSummary, Long?) -> Unit,
) {
    BackHandler(onBack = onBack)
    val detailScrollState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()

    fun scrollToTop(requestPrimaryActionFocus: Boolean) {
        if (requestPrimaryActionFocus) {
            primaryActionFocusRequester.requestFocus()
        }
        scrollScope.launch {
            if (requestPrimaryActionFocus) {
                withFrameNanos { }
            }
            detailScrollState.scrollToItem(0)
        }
    }

    fun scrollToBottom() {
        scrollScope.launch {
            val lastItemIndex = detailScrollState.layoutInfo.totalItemsCount - 1
            if (lastItemIndex >= 0) {
                detailScrollState.scrollToItem(lastItemIndex)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("detail-cinematic-surface"),
    ) {
        val horizontalSafePadding = maxOf(28.dp, maxWidth * 0.045f)
        val posterWidth = maxWidth * 0.14f
        val childCardWidth = maxWidth * 0.18f
        val sectionKind = childSectionKind(parent)

        ServerBackdrop(
            session = session,
            backdropPath = resolveDetailBackdrop(parent, focusedChild),
            title = parent.title,
            policy = ServerImagePolicy.Store,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xFA060912),
                        0.58f to Color(0xD9060912),
                        1f to Color(0x52060912),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.56f to Color(0x52060912),
                        1f to Color(0xFF060912),
                    ),
                ),
        )

        LazyColumn(
            state = detailScrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item(key = "detail-first-viewport-${parent.id}") {
                Column(
                    modifier = if (parent.children.isNotEmpty()) {
                        Modifier.heightIn(min = maxHeight)
                    } else {
                        Modifier
                    },
                ) {
                    Spacer(Modifier.height(92.dp))
                    DetailHero(
                        session = session,
                        parent = parent,
                        focusedChild = focusedChild,
                        sectionKind = sectionKind,
                        posterWidth = posterWidth,
                        horizontalSafePadding = horizontalSafePadding,
                        resumePositionSeconds = resumePositionSeconds,
                        primaryActionFocusRequester = primaryActionFocusRequester,
                        onNavigateUp = { scrollToTop(requestPrimaryActionFocus = false) },
                        onNavigateDown = if (parent.children.isEmpty()) {
                            ::scrollToBottom
                        } else {
                            null
                        },
                        onResumePlayback = onResumePlayback,
                        onStartOverPlayback = onStartOverPlayback,
                    )
                    if (parent.children.isNotEmpty()) {
                        Spacer(Modifier.height(30.dp))
                        DetailChildRibbon(
                            session = session,
                            parent = parent,
                            focusedChild = focusedChild,
                            initialChildId = initialChildId,
                            sectionKind = sectionKind,
                            childViewport = childViewport,
                            childCardWidth = childCardWidth,
                            horizontalSafePadding = horizontalSafePadding,
                            resumePositionsByMediaId = resumePositionsByMediaId,
                            childFocusRequester = childFocusRequester,
                            onNavigateUp = { scrollToTop(requestPrimaryActionFocus = true) },
                            onNavigateDown = ::scrollToBottom,
                            onChildFocused = onChildFocused,
                            onChildViewportChanged = onChildViewportChanged,
                            onPlayChild = onPlayChild,
                        )
                    }
                }
            }
            if (parent.directors.isNotEmpty() || parent.actors.isNotEmpty()) {
                item(key = "detail-credits-${parent.id}") {
                    DetailCreditsAndCast(
                        session = session,
                        parent = parent,
                        horizontalSafePadding = horizontalSafePadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHero(
    session: Session,
    parent: MediaDetail,
    focusedChild: MediaSummary?,
    sectionKind: MediaChildSectionKind,
    posterWidth: Dp,
    horizontalSafePadding: Dp,
    resumePositionSeconds: Long?,
    primaryActionFocusRequester: FocusRequester,
    onNavigateUp: () -> Unit,
    onNavigateDown: (() -> Unit)?,
    onResumePlayback: () -> Unit,
    onStartOverPlayback: () -> Unit,
) {
    val accentPalette = LocalAccentPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalSafePadding),
    ) {
        ServerImage(
            session = session,
            rawValue = parent.posterPath,
            fallbackText = parent.title,
            contentDescription = null,
            policy = ServerImagePolicy.Store,
            modifier = Modifier
                .width(posterWidth)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .testTag("detail-parent-poster-${parent.id}"),
        )
        Spacer(Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = parent.title,
                color = OnBackground,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DetailMetadata(parent)
            focusedChild?.let { child ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = focusedChildPreview(sectionKind, child),
                    color = accentPalette.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (parent.children.isEmpty() || focusedChild != null) {
                Spacer(Modifier.height(18.dp))
                DetailPlaybackActions(
                    resumePositionSeconds = resumePositionSeconds,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    onNavigateUp = onNavigateUp,
                    onNavigateDown = onNavigateDown,
                    onResumePlayback = onResumePlayback,
                    onStartOverPlayback = onStartOverPlayback,
                )
            }
            parent.plot?.takeIf(String::isNotBlank)?.let { plot ->
                Spacer(Modifier.height(18.dp))
                Text(
                    text = plot,
                    color = OnBackground,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DetailPlaybackActions(
    resumePositionSeconds: Long?,
    primaryActionFocusRequester: FocusRequester,
    onNavigateUp: () -> Unit,
    onNavigateDown: (() -> Unit)?,
    onResumePlayback: () -> Unit,
    onStartOverPlayback: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.detailVerticalBoundaryKeys(
            onUp = onNavigateUp,
            onDown = onNavigateDown,
        ),
    ) {
        if (resumePositionSeconds != null) {
            KaloscopeButton(
                onClick = onResumePlayback,
                modifier = Modifier.focusRequester(primaryActionFocusRequester),
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
            ) {
                Text(stringResource(R.string.resume_playback))
            }
            KaloscopeButton(
                onClick = onStartOverPlayback,
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
            ) {
                Text(stringResource(R.string.play_from_start))
            }
        } else {
            KaloscopeButton(
                onClick = onStartOverPlayback,
                modifier = Modifier.focusRequester(primaryActionFocusRequester),
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
            ) {
                Text(stringResource(R.string.play))
            }
        }
    }
}

@Composable
private fun DetailChildRibbon(
    session: Session,
    parent: MediaDetail,
    focusedChild: MediaSummary?,
    initialChildId: Long?,
    sectionKind: MediaChildSectionKind,
    childViewport: GridViewportSnapshot,
    childCardWidth: Dp,
    horizontalSafePadding: Dp,
    resumePositionsByMediaId: Map<Long, Long>,
    childFocusRequester: FocusRequester,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onChildFocused: (Long) -> Unit,
    onChildViewportChanged: (GridViewportSnapshot) -> Unit,
    onPlayChild: (MediaSummary, Long?) -> Unit,
) {
    Text(
        text = stringResource(
            when (sectionKind) {
                MediaChildSectionKind.Episodes -> R.string.episodes
                MediaChildSectionKind.Parts -> R.string.parts
            },
        ),
        color = OnBackground,
        fontSize = 21.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = horizontalSafePadding),
    )
    Spacer(Modifier.height(12.dp))
    val initialTargetIndex = parent.children
        .indexOfFirst { it.id == initialChildId }
        .takeIf { it >= 0 }
        ?: 0
    val initialTargetId = parent.children[initialTargetIndex].id
    key(parent.id, initialTargetId) {
        val restoreIndex = childViewport.firstVisibleItemIndex
            .coerceIn(0, parent.children.lastIndex.coerceAtLeast(0))
        val childListState = rememberLazyListState(
            initialFirstVisibleItemIndex = restoreIndex,
            initialFirstVisibleItemScrollOffset = childViewport
                .firstVisibleItemScrollOffset
                .coerceAtLeast(0),
        )
        LaunchedEffect(childListState, initialTargetId) {
            val initiallyVisibleIndices = snapshotFlow {
                childListState.layoutInfo.visibleItemsInfo.map { it.index }
            }.first { it.isNotEmpty() }
            if (initialTargetIndex !in initiallyVisibleIndices) {
                childListState.scrollToItem(initialTargetIndex)
            }
            snapshotFlow {
                childListState.layoutInfo.visibleItemsInfo.any {
                    it.index == initialTargetIndex
                }
            }.first { it }
            withFrameNanos { }
            childFocusRequester.requestFocus()
        }
        LaunchedEffect(childListState, parent.children.size) {
            snapshotFlow {
                GridViewportSnapshot(
                    firstVisibleItemIndex = childListState.firstVisibleItemIndex
                        .coerceAtMost(parent.children.lastIndex.coerceAtLeast(0)),
                    firstVisibleItemScrollOffset = childListState.firstVisibleItemScrollOffset
                        .coerceAtLeast(0),
                )
            }.distinctUntilChanged().collect(onChildViewportChanged)
        }
        LazyRow(
            state = childListState,
            modifier = Modifier.detailVerticalBoundaryKeys(
                onUp = onNavigateUp,
                onDown = onNavigateDown,
            ),
            contentPadding = PaddingValues(
                start = horizontalSafePadding + 10.dp,
                end = horizontalSafePadding + 10.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = parent.children,
                key = MediaSummary::id,
            ) { child ->
                val initialFocusModifier = if (child.id == focusedChild?.id) {
                    Modifier.focusRequester(childFocusRequester)
                } else {
                    Modifier
                }
                MediaChildCard(
                    session = session,
                    child = child,
                    focusedTarget = child.id == focusedChild?.id,
                    onFocused = { onChildFocused(child.id) },
                    onClick = {
                        onPlayChild(
                            child,
                            resumePositionsByMediaId[child.id]?.takeIf { it > 0 },
                        )
                    },
                    modifier = initialFocusModifier.width(childCardWidth),
                )
            }
        }
    }
}

@Composable
private fun focusedChildPreview(
    sectionKind: MediaChildSectionKind,
    child: MediaSummary,
): String = if (
    sectionKind == MediaChildSectionKind.Episodes &&
    child.season != null &&
    child.episode != null
) {
    stringResource(R.string.episode_preview, child.season, child.episode, child.title)
} else {
    child.title
}

@Composable
private fun DetailMetadata(detail: MediaDetail) {
    val accentPalette = LocalAccentPalette.current
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (detail.genres.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = detail.genres.joinToString("  ·  "),
            color = accentPalette.primary,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailCreditsAndCast(
    session: Session,
    parent: MediaDetail,
    horizontalSafePadding: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalSafePadding,
                top = 34.dp,
                end = horizontalSafePadding,
            )
            .padding(16.dp),
    ) {
        parent.directors.takeIf(List<String>::isNotEmpty)?.let { directors ->
            Text(
                text = stringResource(R.string.directors, directors.joinToString("、")),
                color = Muted,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (parent.directors.isNotEmpty() && parent.actors.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
        }
        CastStrip(
            session = session,
            actors = parent.actors,
        )
    }
}

private fun Modifier.detailVerticalBoundaryKeys(
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
): Modifier = onPreviewKeyEvent { event ->
    val action = when (event.key) {
        Key.DirectionUp -> onUp
        Key.DirectionDown -> onDown
        else -> null
    } ?: return@onPreviewKeyEvent false

    if (event.type == KeyEventType.KeyDown) {
        action()
    }
    true
}
