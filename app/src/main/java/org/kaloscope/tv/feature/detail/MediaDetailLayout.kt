package org.kaloscope.tv.feature.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Outline
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.RatingBadge
import org.kaloscope.tv.core.designsystem.ServerBackdrop
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.RatingDisplayPolicy
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
internal fun MediaDetailCinematicLayout(
    session: Session,
    parent: MediaDetail,
    focusedChild: MediaSummary?,
    focusedChildDetail: MediaDetail?,
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
    val castFocusRequester = remember(parent.id) { FocusRequester() }
    val moreInfoFocusRequester = remember(parent.id) { FocusRequester() }
    val moreInfoCloseFocusRequester = remember(parent.id) { FocusRequester() }
    var moreInfoOpen by remember(parent.id) { mutableStateOf(false) }
    val displayedPlot = focusedChildDetail
        ?.plot
        ?.takeIf(String::isNotBlank)
        ?: parent.plot

    fun dismissMoreInfo() {
        moreInfoOpen = false
        scrollScope.launch {
            withFrameNanos { }
            moreInfoFocusRequester.requestFocus()
        }
    }

    fun navigateUp(onAlreadyAtTop: (() -> Unit)? = null) {
        if (!detailScrollState.canScrollBackward) {
            onAlreadyAtTop?.invoke()
            return
        }
        scrollScope.launch {
            detailScrollState.scrollToItem(0)
        }
    }

    fun scrollToBottom() {
        if (!detailScrollState.canScrollForward && parent.actors.isNotEmpty()) {
            castFocusRequester.requestFocus()
            return
        }
        scrollScope.launch {
            while (detailScrollState.canScrollForward) {
                val viewportHeight = detailScrollState.layoutInfo.viewportSize.height
                if (
                    viewportHeight <= 0 ||
                    detailScrollState.scrollBy(viewportHeight.toFloat()) <= 0f
                ) {
                    break
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("detail-cinematic-surface"),
    ) {
        val horizontalSafePadding = maxOf(28.dp, maxWidth * 0.045f)
        val compactSeriesLayout = parent.children.isNotEmpty() && maxHeight <= 600.dp
        val posterWidth = if (compactSeriesLayout) {
            (maxWidth * 0.13f).coerceIn(128.dp, 176.dp)
        } else {
            (maxWidth * 0.14f).coerceIn(136.dp, 196.dp)
        }
        val childCardWidth = if (compactSeriesLayout) {
            (maxWidth * 0.16f).coerceIn(156.dp, 220.dp)
        } else {
            (maxWidth * 0.18f).coerceIn(176.dp, 260.dp)
        }
        val sectionKind = childSectionKind(parent)

        ServerBackdrop(
            session = session,
            backdropPath = resolveDetailBackdrop(parent, focusedChild),
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
                    Spacer(Modifier.height(if (compactSeriesLayout) 24.dp else 60.dp))
                    DetailHero(
                        session = session,
                        parent = parent,
                        focusedChild = focusedChild,
                        plot = displayedPlot,
                        compactSeriesLayout = compactSeriesLayout,
                        blockParentBringIntoView = parent.children.isNotEmpty(),
                        sectionKind = sectionKind,
                        posterWidth = posterWidth,
                        horizontalSafePadding = horizontalSafePadding,
                        resumePositionSeconds = resumePositionSeconds,
                        primaryActionFocusRequester = primaryActionFocusRequester,
                        moreInfoFocusRequester = moreInfoFocusRequester,
                        onNavigateUp = { navigateUp() },
                        onNavigateDown = if (parent.children.isEmpty()) {
                            ::scrollToBottom
                        } else {
                            {
                                childFocusRequester.requestFocus()
                                Unit
                            }
                        },
                        onResumePlayback = onResumePlayback,
                        onStartOverPlayback = onStartOverPlayback,
                        onShowMoreInfo = { moreInfoOpen = true },
                    )
                    if (parent.children.isNotEmpty()) {
                        Spacer(Modifier.height(if (compactSeriesLayout) 16.dp else 30.dp))
                        DetailChildRibbon(
                            session = session,
                            parent = parent,
                            focusedChild = focusedChild,
                            initialChildId = initialChildId,
                            sectionKind = sectionKind,
                            childViewport = childViewport,
                            childCardWidth = childCardWidth,
                            compactLayout = compactSeriesLayout,
                            horizontalSafePadding = horizontalSafePadding,
                            resumePositionsByMediaId = resumePositionsByMediaId,
                            childFocusRequester = childFocusRequester,
                            onNavigateUp = {
                                navigateUp {
                                    primaryActionFocusRequester.requestFocus()
                                }
                            },
                            onNavigateDown = ::scrollToBottom,
                            onInitialFocusSettled = {
                                detailScrollState.scrollToItem(0)
                            },
                            onChildFocused = onChildFocused,
                            onChildViewportChanged = onChildViewportChanged,
                            onPlayChild = onPlayChild,
                        )
                    }
                }
            }
            if (
                parent.directors.isNotEmpty() ||
                parent.writers.isNotEmpty() ||
                parent.studios.isNotEmpty() ||
                parent.actors.isNotEmpty()
            ) {
                item(key = "detail-credits-${parent.id}") {
                    DetailCreditsAndCast(
                        session = session,
                        parent = parent,
                        horizontalSafePadding = horizontalSafePadding,
                        castFocusRequester = castFocusRequester,
                        onNavigateUp = {
                            navigateUp {
                                primaryActionFocusRequester.requestFocus()
                            }
                        },
                    )
                }
            }
        }
        if (moreInfoOpen) {
            DetailMoreInfoPanel(
                viewportSize = DpSize(maxWidth, maxHeight),
                title = focusedChild?.let(::mediaChildDisplayTitle) ?: parent.title,
                plot = displayedPlot,
                genres = parent.genres,
                closeFocusRequester = moreInfoCloseFocusRequester,
                onDismiss = ::dismissMoreInfo,
            )
        }
    }
}

@Composable
private fun DetailHero(
    session: Session,
    parent: MediaDetail,
    focusedChild: MediaSummary?,
    plot: String?,
    compactSeriesLayout: Boolean,
    blockParentBringIntoView: Boolean,
    sectionKind: MediaChildSectionKind,
    posterWidth: Dp,
    horizontalSafePadding: Dp,
    resumePositionSeconds: Long?,
    primaryActionFocusRequester: FocusRequester,
    moreInfoFocusRequester: FocusRequester,
    onNavigateUp: () -> Unit,
    onNavigateDown: (() -> Unit)?,
    onResumePlayback: () -> Unit,
    onStartOverPlayback: () -> Unit,
    onShowMoreInfo: () -> Unit,
) {
    val accentPalette = LocalAccentPalette.current
    val synopsis = plot?.takeIf(String::isNotBlank)
    val synopsisLines = if (compactSeriesLayout) 3 else 4
    val synopsisHeight = with(LocalDensity.current) {
        if (compactSeriesLayout) 66.sp.toDp() else 100.sp.toDp()
    }
    val bringIntoViewBoundaryModifier = if (blockParentBringIntoView) {
        Modifier.blockParentBringIntoView()
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(bringIntoViewBoundaryModifier)
            .padding(horizontal = horizontalSafePadding),
    ) {
        ServerImage(
            session = session,
            rawValue = parent.posterPath,
            contentDescription = null,
            policy = ServerImagePolicy.Store,
            modifier = Modifier
                .width(posterWidth)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .testTag("detail-parent-poster-${parent.id}"),
        )
        Spacer(Modifier.width(24.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 720.dp),
        ) {
            Text(
                text = parent.title,
                color = OnBackground,
                fontSize = if (compactSeriesLayout) 32.sp else 36.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DetailMetadata(parent)
            focusedChild?.let { child ->
                Spacer(Modifier.height(if (compactSeriesLayout) 6.dp else 10.dp))
                Text(
                    text = focusedChildPreview(sectionKind, child),
                    color = accentPalette.primary,
                    fontSize = if (compactSeriesLayout) 15.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (parent.children.isEmpty() || focusedChild != null) {
                Spacer(Modifier.height(if (compactSeriesLayout) 12.dp else 18.dp))
                DetailPlaybackActions(
                    resumePositionSeconds = resumePositionSeconds,
                    primaryActionFocusRequester = primaryActionFocusRequester,
                    moreInfoAvailable = !plot.isNullOrBlank() || parent.genres.isNotEmpty(),
                    moreInfoFocusRequester = moreInfoFocusRequester,
                    onNavigateUp = onNavigateUp,
                    onNavigateDown = onNavigateDown,
                    onResumePlayback = onResumePlayback,
                    onStartOverPlayback = onStartOverPlayback,
                    onShowMoreInfo = onShowMoreInfo,
                )
            }
            if (synopsis != null || parent.children.isNotEmpty()) {
                Spacer(Modifier.height(if (compactSeriesLayout) 10.dp else 18.dp))
                Text(
                    text = synopsis.orEmpty(),
                    color = OnBackground,
                    fontSize = if (compactSeriesLayout) 15.sp else 17.sp,
                    lineHeight = if (compactSeriesLayout) 22.sp else 25.sp,
                    maxLines = synopsisLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (parent.children.isNotEmpty()) {
                        Modifier.height(synopsisHeight)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailPlaybackActions(
    resumePositionSeconds: Long?,
    primaryActionFocusRequester: FocusRequester,
    moreInfoAvailable: Boolean,
    moreInfoFocusRequester: FocusRequester,
    onNavigateUp: () -> Unit,
    onNavigateDown: (() -> Unit)?,
    onResumePlayback: () -> Unit,
    onStartOverPlayback: () -> Unit,
    onShowMoreInfo: () -> Unit,
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
                modifier = Modifier
                    .focusRequester(primaryActionFocusRequester)
                    .testTag("detail-primary-action"),
                selected = true,
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
            ) {
                DetailPrimaryPlayActionLabel(stringResource(R.string.resume_playback))
            }
            KaloscopeButton(
                onClick = onStartOverPlayback,
                modifier = Modifier.testTag("detail-start-over-action"),
                variant = KaloscopeControlVariant.Ghost,
                size = KaloscopeControlSize.Compact,
            ) {
                DetailStartOverActionLabel()
            }
        } else {
            KaloscopeButton(
                onClick = onStartOverPlayback,
                modifier = Modifier
                    .focusRequester(primaryActionFocusRequester)
                    .testTag("detail-primary-action"),
                selected = true,
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
            ) {
                DetailPrimaryPlayActionLabel(stringResource(R.string.play))
            }
        }
        if (moreInfoAvailable) {
            KaloscopeButton(
                onClick = onShowMoreInfo,
                modifier = Modifier
                    .focusRequester(moreInfoFocusRequester)
                    .testTag("detail-more-info-action"),
                variant = KaloscopeControlVariant.Ghost,
                size = KaloscopeControlSize.Compact,
            ) {
                Text(stringResource(R.string.detail_more_info))
            }
        }
    }
}

@Composable
private fun DetailStartOverActionLabel() {
    Icon(
        painter = painterResource(R.drawable.ic_action_restart),
        contentDescription = null,
        modifier = Modifier
            .size(22.dp)
            .testTag("detail-start-over-icon"),
    )
    Spacer(Modifier.width(7.dp))
    Text(stringResource(R.string.play_from_start))
}

@Composable
private fun DetailPrimaryPlayActionLabel(text: String) {
    Icon(
        painter = painterResource(R.drawable.ic_action_play),
        contentDescription = null,
        modifier = Modifier
            .size(24.dp)
            .testTag("detail-primary-play-icon"),
    )
    Spacer(Modifier.width(7.dp))
    Text(text)
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
    compactLayout: Boolean,
    horizontalSafePadding: Dp,
    resumePositionsByMediaId: Map<Long, Long>,
    childFocusRequester: FocusRequester,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onInitialFocusSettled: suspend () -> Unit,
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
        fontSize = if (compactLayout) 19.sp else 21.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = horizontalSafePadding),
    )
    Spacer(Modifier.height(if (compactLayout) 8.dp else 12.dp))
    val initialTargetIndex = parent.children
        .indexOfFirst { it.id == initialChildId }
        .takeIf { it >= 0 }
        ?: 0
    val initialTargetId = parent.children[initialTargetIndex].id
    key(parent.id, initialTargetId) {
        val restoreIndex = childViewport.firstVisibleItemIndex
            .coerceIn(0, parent.children.lastIndex.coerceAtLeast(0))
        val restoreOffset = childViewport.firstVisibleItemScrollOffset.coerceAtLeast(0)
        val childListState = rememberLazyListState(
            initialFirstVisibleItemIndex = restoreIndex,
            initialFirstVisibleItemScrollOffset = restoreOffset,
        )
        val canScrollBackward by remember {
            derivedStateOf { childListState.canScrollBackward }
        }
        val canScrollForward by remember {
            derivedStateOf { childListState.canScrollForward }
        }
        val carouselEdgeOffset = with(LocalDensity.current) { 48.dp.roundToPx() }
        val childCardHeight = childCardWidth * 9f / 16f +
            if (compactLayout) 52.dp else 56.dp
        var animateFocusedItem by remember { mutableStateOf(false) }
        var lastFocusedItemIndex by remember { mutableStateOf(initialTargetIndex) }
        var pendingFocusedItemIndex by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(childListState, initialTargetId) {
            val initiallyVisibleIndices = snapshotFlow {
                childListState.layoutInfo.visibleItemsInfo.map { it.index }
            }.first { it.isNotEmpty() }
            val targetWasInitiallyVisible = initialTargetIndex in initiallyVisibleIndices
            if (!targetWasInitiallyVisible) {
                childListState.scrollToItem(initialTargetIndex)
            }
            snapshotFlow {
                childListState.layoutInfo.visibleItemsInfo.any {
                    it.index == initialTargetIndex
                }
            }.first { it }
            withFrameNanos { }
            childFocusRequester.requestFocus()
            withFrameNanos { }
            if (targetWasInitiallyVisible) {
                childListState.scrollToItem(
                    index = restoreIndex,
                    scrollOffset = restoreOffset,
                )
                withFrameNanos { }
            }
            onInitialFocusSettled()
            animateFocusedItem = true
        }
        LaunchedEffect(pendingFocusedItemIndex) {
            pendingFocusedItemIndex?.let { targetIndex ->
                childListState.animateScrollToItem(
                    index = targetIndex,
                    scrollOffset = -carouselEdgeOffset,
                )
                if (pendingFocusedItemIndex == targetIndex) {
                    pendingFocusedItemIndex = null
                }
            }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(childCardHeight + if (compactLayout) 12.dp else 16.dp)
                .blockParentBringIntoView()
                .testTag("detail-child-carousel"),
        ) {
            LazyRow(
                state = childListState,
                modifier = Modifier
                    .fillMaxSize()
                    .detailVerticalBoundaryKeys(
                        onUp = onNavigateUp,
                        onDown = onNavigateDown,
                    ),
                contentPadding = PaddingValues(
                    start = horizontalSafePadding + 10.dp,
                    top = if (compactLayout) 6.dp else 8.dp,
                    end = horizontalSafePadding + 10.dp,
                    bottom = if (compactLayout) 6.dp else 8.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = parent.children,
                    key = { _, child -> child.id },
                ) { index, child ->
                    val initialFocusModifier = if (child.id == focusedChild?.id) {
                        Modifier.focusRequester(childFocusRequester)
                    } else {
                        Modifier
                    }
                    MediaChildCard(
                        session = session,
                        child = child,
                        focusedTarget = child.id == focusedChild?.id,
                        onFocused = {
                            if (animateFocusedItem && index != lastFocusedItemIndex) {
                                pendingFocusedItemIndex = index
                            }
                            lastFocusedItemIndex = index
                            onChildFocused(child.id)
                        },
                        onClick = {
                            onPlayChild(
                                child,
                                resumePositionsByMediaId[child.id]?.takeIf { it > 0 },
                            )
                        },
                        modifier = initialFocusModifier
                            .width(childCardWidth)
                            .height(childCardHeight),
                    )
                }
            }
            if (canScrollBackward) {
                DetailCarouselEdgeFade(start = true)
            }
            if (canScrollForward) {
                DetailCarouselEdgeFade(start = false)
            }
        }
    }
}

@Composable
private fun BoxScope.DetailCarouselEdgeFade(start: Boolean) {
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
                    "detail-child-carousel-start-fade"
                } else {
                    "detail-child-carousel-end-fade"
                },
            ),
    )
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
    val seasonEpisode = detail.season?.let { season ->
        detail.episode?.let { episode ->
            stringResource(R.string.season_episode, season, episode)
        }
    }
    val rating = RatingDisplayPolicy.format(detail.rating)
    if (detail.year != null || seasonEpisode != null || rating != null || detail.aired != null) {
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            detail.year?.let { year ->
                DetailMetadataBadge(
                    text = year.toString(),
                    modifier = Modifier.testTag("detail-metadata-year"),
                )
            }
            seasonEpisode?.let { value ->
                DetailMetadataBadge(
                    text = value,
                    modifier = Modifier.testTag("detail-metadata-season-episode"),
                )
            }
            rating?.let { value ->
                RatingBadge(
                    rating = value,
                    testTag = "detail-rating-badge",
                )
            }
            detail.aired?.let { aired ->
                Text(
                    text = aired,
                    color = Muted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("detail-metadata-aired"),
                )
            }
        }
    }
    if (detail.genres.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            detail.genres.take(4).forEachIndexed { index, genre ->
                Text(
                    text = genre,
                    color = accentPalette.soft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier
                        .background(
                            color = accentPalette.panelSelected,
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("detail-genre-$index"),
                )
            }
            if (detail.genres.size > 4) {
                Text(
                    text = "+${detail.genres.size - 4}",
                    color = Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(Panel.copy(alpha = 0.78f), RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailMetadataBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = OnBackground,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .background(Panel.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
            .border(1.dp, Outline, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun DetailCreditsAndCast(
    session: Session,
    parent: MediaDetail,
    horizontalSafePadding: Dp,
    castFocusRequester: FocusRequester,
    onNavigateUp: () -> Unit,
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
        val hasCredits = parent.directors.isNotEmpty() ||
            parent.writers.isNotEmpty() ||
            parent.studios.isNotEmpty()
        if (hasCredits) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                parent.directors.takeIf(List<String>::isNotEmpty)?.let { directors ->
                    DetailCreditColumn(
                        label = stringResource(R.string.director_title),
                        values = directors,
                        modifier = Modifier.weight(1f),
                    )
                }
                parent.writers.takeIf(List<String>::isNotEmpty)?.let { writers ->
                    DetailCreditColumn(
                        label = stringResource(R.string.writer_title),
                        values = writers,
                        modifier = Modifier.weight(1f),
                    )
                }
                parent.studios.takeIf(List<String>::isNotEmpty)?.let { studios ->
                    DetailCreditColumn(
                        label = stringResource(R.string.studio_title),
                        values = studios,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (hasCredits && parent.actors.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
        }
        CastStrip(
            session = session,
            actors = parent.actors,
            focusRequester = castFocusRequester,
            onNavigateUp = onNavigateUp,
        )
    }
}

@Composable
private fun DetailCreditColumn(
    label: String,
    values: List<String>,
    modifier: Modifier = Modifier,
) {
    val accentPalette = LocalAccentPalette.current
    Column(modifier = modifier) {
        Text(
            text = label,
            color = accentPalette.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = values.joinToString("、"),
            color = Muted,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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

private fun Modifier.blockParentBringIntoView(): Modifier =
    this then BlockParentBringIntoViewElement

private data object BlockParentBringIntoViewElement :
    ModifierNodeElement<BlockParentBringIntoViewNode>() {
    override fun create() = BlockParentBringIntoViewNode()

    override fun update(node: BlockParentBringIntoViewNode) = Unit
}

private class BlockParentBringIntoViewNode : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) = Unit
}
