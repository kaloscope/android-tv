package org.kaloscope.tv.feature.reader

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialog
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialogOption
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelAdjustmentRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelChoiceRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSectionHeader
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSelectionRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSessionHint
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSide
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.appErrorText
import org.kaloscope.tv.core.designsystem.imagePageDirectionLabel
import org.kaloscope.tv.core.designsystem.imageReadModeLabel
import org.kaloscope.tv.core.designsystem.imageZoomModeLabel
import org.kaloscope.tv.core.designsystem.readerChapterOrderLabel
import org.kaloscope.tv.core.designsystem.readerBackgroundColor
import org.kaloscope.tv.core.designsystem.textReaderFontLabel
import org.kaloscope.tv.core.designsystem.textReaderThemeLabel
import org.kaloscope.tv.core.designsystem.toDpDimensions
import org.kaloscope.tv.core.model.ImagePageDirection
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ImageZoomMode
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderSettingsPolicy
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.TextReaderFont
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.TextReaderTheme
import org.kaloscope.tv.core.reader.ReaderBoundary
import org.kaloscope.tv.core.reader.ReaderChapterPolicy
import org.kaloscope.tv.core.reader.ReaderControlTarget
import org.kaloscope.tv.core.reader.ReaderRemoteKeyPolicy
import org.kaloscope.tv.feature.reader.image.ImageReaderSurface
import org.kaloscope.tv.feature.reader.text.TextReaderPalettes
import org.kaloscope.tv.feature.reader.text.TextReaderSurface
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    session: Session,
    state: ReaderUiState,
    onBack: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onLoadMoreImages: () -> Unit,
    onImageSettings: (ImageReaderSettings) -> Unit,
    onTextSettings: (TextReaderSettings) -> Unit,
    onChapterOrder: (ReaderChapterOrder) -> Unit,
    onDismissChapterError: () -> Unit,
    onDismissPageError: () -> Unit,
) {
    when (state) {
        ReaderUiState.Idle -> ReaderUnavailable(onBack)
        is ReaderUiState.Error -> ReaderUnavailable(onBack, state)
        is ReaderUiState.Active -> ActiveReader(
            session = session,
            state = state,
            onBack = onBack,
            onSelectChapter = onSelectChapter,
            onLoadMoreImages = onLoadMoreImages,
            onImageSettings = onImageSettings,
            onTextSettings = onTextSettings,
            onChapterOrder = onChapterOrder,
            onDismissChapterError = onDismissChapterError,
            onDismissPageError = onDismissPageError,
        )
    }
}

@Composable
private fun ActiveReader(
    session: Session,
    state: ReaderUiState.Active,
    onBack: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onLoadMoreImages: () -> Unit,
    onImageSettings: (ImageReaderSettings) -> Unit,
    onTextSettings: (TextReaderSettings) -> Unit,
    onChapterOrder: (ReaderChapterOrder) -> Unit,
    onDismissChapterError: () -> Unit,
    onDismissPageError: () -> Unit,
) {
    val contentFocus = remember { FocusRequester() }
    val loadingFocus = remember { FocusRequester() }
    val controlFocus = remember {
        ReaderControlTarget.entries.associateWith { FocusRequester() }
    }
    var controlsVisible by remember(state.requestId) { mutableStateOf(false) }
    var drawer by remember(state.requestId) { mutableStateOf<ReaderDrawer?>(null) }
    var lastControlTarget by remember(state.requestId) {
        mutableStateOf<ReaderControlTarget?>(null)
    }
    var pendingControlFocus by remember(state.requestId) {
        mutableStateOf<ReaderControlTarget?>(null)
    }
    var lastRequestedChapterIndex by remember(state.requestId) {
        mutableStateOf<Int?>(null)
    }
    var initialTitleVisible by remember(state.requestId) { mutableStateOf(true) }
    var imagePosition by remember(state.requestId, state.contentRevision) {
        mutableIntStateOf(
            if ((state as? ReaderUiState.Image)?.content?.images?.isNotEmpty() == true) 1 else 0,
        )
    }
    var failedImagesAvailable by remember(state.requestId, state.contentRevision) {
        mutableStateOf(false)
    }
    var imageRetryRevision by remember(state.requestId, state.contentRevision) {
        mutableIntStateOf(0)
    }
    val content = when (state) {
        is ReaderUiState.Image -> state.content
        is ReaderUiState.Text -> state.content
    }
    val previousChapter = ReaderChapterPolicy.previousIndex(
        content.chapters,
        content.selectedChapterIndex,
    )
    val nextChapter = ReaderChapterPolicy.nextIndex(
        content.chapters,
        content.selectedChapterIndex,
    )
    val textPalette = (state as? ReaderUiState.Text)
        ?.let { TextReaderPalettes.forTheme(it.settings.theme) }
    val background = textPalette?.background ?: Color.Black
    val overlayText = textPalette?.text ?: Color.White
    val overlayMuted = textPalette?.muted ?: Color(0xFFAAAAAA)

    fun requestControl(target: ReaderControlTarget) {
        controlsVisible = true
        lastControlTarget = target
        pendingControlFocus = target
    }

    fun controlTargetIsEnabled(target: ReaderControlTarget): Boolean =
        when (target) {
            ReaderControlTarget.PreviousChapter -> previousChapter != null
            ReaderControlTarget.Chapters -> content.chapters.size > 1
            ReaderControlTarget.Settings -> true
            ReaderControlTarget.RetryImages -> failedImagesAvailable
            ReaderControlTarget.NextChapter -> nextChapter != null
        }

    fun entryControlTarget(): ReaderControlTarget =
        lastControlTarget
            ?.takeIf(::controlTargetIsEnabled)
            ?: defaultControlTarget(content.chapters)

    fun dismissDrawer() {
        drawer = null
        controlsVisible = true
        pendingControlFocus = entryControlTarget()
    }

    fun hideControls() {
        controlsVisible = false
        pendingControlFocus = null
        contentFocus.requestFocus()
    }

    fun toggleControls() {
        if (controlsVisible) {
            hideControls()
        } else {
            requestControl(entryControlTarget())
        }
    }

    fun openBoundary(boundary: ReaderBoundary) {
        requestControl(
            ReaderRemoteKeyPolicy.boundaryTarget(
                boundary = boundary,
                hasAdjacentChapter = if (boundary == ReaderBoundary.Start) {
                    previousChapter != null
                } else {
                    nextChapter != null
                },
                hasMultipleChapters = content.chapters.size > 1,
            ),
        )
    }

    fun selectChapter(index: Int) {
        lastRequestedChapterIndex = index
        onSelectChapter(index)
    }

    LaunchedEffect(state.requestId, state.contentRevision) {
        withFrameNanos { }
        contentFocus.requestFocus()
    }
    LaunchedEffect(state.requestId) {
        initialTitleVisible = true
        delay(TITLE_HIDE_DELAY_MILLIS)
        initialTitleVisible = false
    }
    LaunchedEffect(controlsVisible, pendingControlFocus, drawer) {
        val target = pendingControlFocus
        if (controlsVisible && drawer == null && target != null) {
            withFrameNanos { }
            controlFocus.getValue(target).requestFocus()
            pendingControlFocus = null
        }
    }
    LaunchedEffect(state.isChapterLoading) {
        withFrameNanos { }
        if (state.isChapterLoading) {
            loadingFocus.requestFocus()
        } else if (drawer == null) {
            if (controlsVisible) {
                requestControl(entryControlTarget())
            } else {
                contentFocus.requestFocus()
            }
        }
    }
    LaunchedEffect(failedImagesAvailable) {
        if (
            controlsVisible &&
            lastControlTarget == ReaderControlTarget.RetryImages &&
            !failedImagesAvailable
        ) {
            requestControl(entryControlTarget())
        }
    }

    BackHandler(enabled = drawer == null) {
        if (controlsVisible) {
            hideControls()
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .testTag("reader-screen"),
    ) {
        when (state) {
            is ReaderUiState.Image -> ImageReaderSurface(
                session = session,
                content = state.content,
                settings = state.settings,
                contentRevision = state.contentRevision,
                imagesExhausted = state.imagesExhausted,
                isLoadingMore = state.isLoadingMore,
                controlsVisible = controlsVisible,
                focusRequester = contentFocus,
                onToggleControls = ::toggleControls,
                onEnterControls = {
                    requestControl(entryControlTarget())
                },
                onBoundary = ::openBoundary,
                onLoadMore = onLoadMoreImages,
                onPositionChanged = {
                    imagePosition = it.coerceIn(0, state.content.images.size)
                },
                manualRetryRevision = imageRetryRevision,
                onFailedImagesChanged = { failedImagesAvailable = it },
            )

            is ReaderUiState.Text -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = READER_EDGE_HEIGHT),
            ) {
                TextReaderSurface(
                    content = state.content,
                    settings = state.settings,
                    contentRevision = state.contentRevision,
                    controlsVisible = controlsVisible,
                    focusRequester = contentFocus,
                    onToggleControls = ::toggleControls,
                    onEnterControls = {
                        requestControl(entryControlTarget())
                    },
                    onBoundary = ::openBoundary,
                )
            }
        }

        AnimatedVisibility(
            visible = state is ReaderUiState.Text || initialTitleVisible || controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTitleOverlay(
                title = content.title,
                chapter = content.selectedChapterIndex?.let(content.chapters::getOrNull),
                textColor = overlayText,
                mutedColor = overlayMuted,
                scrimColor = background,
                status = (state as? ReaderUiState.Image)?.content?.let { imageContent ->
                    imagePosition.takeIf { it > 0 }?.let { position ->
                        stringResource(
                            R.string.reader_image_progress,
                            position,
                            imageContent.imageCount.coerceAtLeast(imageContent.images.size),
                        )
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderBottomControls(
                previousEnabled = previousChapter != null,
                nextEnabled = nextChapter != null,
                chaptersEnabled = content.chapters.size > 1,
                retryImagesEnabled = failedImagesAvailable,
                scrimColor = background,
                focusRequesters = controlFocus,
                onFocused = { lastControlTarget = it },
                onPrevious = {
                    previousChapter?.let {
                        lastControlTarget = ReaderControlTarget.PreviousChapter
                        selectChapter(it)
                    }
                },
                onChapters = {
                    lastControlTarget = ReaderControlTarget.Chapters
                    drawer = ReaderDrawer.Chapters
                },
                onSettings = {
                    lastControlTarget = ReaderControlTarget.Settings
                    drawer = ReaderDrawer.Settings
                },
                onRetryImages = {
                    lastControlTarget = ReaderControlTarget.RetryImages
                    imageRetryRevision += 1
                },
                onNext = {
                    nextChapter?.let {
                        lastControlTarget = ReaderControlTarget.NextChapter
                        selectChapter(it)
                    }
                },
            )
        }

        if (drawer == ReaderDrawer.Chapters) {
            ReaderChapterDrawer(
                chapters = content.chapters,
                selectedIndex = content.selectedChapterIndex,
                order = state.chapterOrder,
                panelColor = ReaderDrawerPanelColor,
                textColor = ReaderDrawerTextColor,
                mutedColor = ReaderDrawerMutedColor,
                controlContentColor = ReaderDrawerTextColor,
                onDismiss = ::dismissDrawer,
                onSelect = { chapterIndex ->
                    dismissDrawer()
                    selectChapter(chapterIndex)
                },
            )
        }
        if (drawer == ReaderDrawer.Settings) {
            when (state) {
                is ReaderUiState.Image -> ImageReaderSettingsDrawer(
                    settings = state.settings,
                    chapterOrder = state.chapterOrder,
                    onSettings = onImageSettings,
                    onChapterOrder = onChapterOrder,
                    onDismiss = ::dismissDrawer,
                )

                is ReaderUiState.Text -> TextReaderSettingsDrawer(
                    settings = state.settings,
                    chapterOrder = state.chapterOrder,
                    panelColor = ReaderDrawerPanelColor,
                    textColor = ReaderDrawerTextColor,
                    mutedColor = ReaderDrawerMutedColor,
                    controlContentColor = ReaderDrawerTextColor,
                    onSettings = onTextSettings,
                    onChapterOrder = onChapterOrder,
                    onDismiss = ::dismissDrawer,
                )
            }
        }

        state.chapterError?.let { error ->
            ReaderRecoverableError(
                message = stringResource(R.string.reader_switch_chapter_failed),
                detail = appErrorText(error),
                onDismiss = onDismissChapterError,
                onRetry = lastRequestedChapterIndex?.let { index ->
                    { selectChapter(index) }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        if (state is ReaderUiState.Image) {
            state.pageError?.let { error ->
                ReaderRecoverableError(
                    message = stringResource(R.string.reader_load_more_failed),
                    detail = appErrorText(error),
                    onDismiss = onDismissPageError,
                    onRetry = onLoadMoreImages,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        if (state.isChapterLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .focusRequester(loadingFocus)
                    .focusable()
                    .onPreviewKeyEvent { true }
                    .testTag("reader-chapter-loading"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.reader_switching_chapter),
                    color = Color.White,
                    fontSize = 22.sp,
                )
            }
        }
    }
}

@Composable
internal fun ReaderTitleOverlay(
    title: String,
    chapter: ReaderChapter?,
    textColor: Color,
    mutedColor: Color,
    scrimColor: Color,
    status: String?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(READER_EDGE_HEIGHT)
            .testTag("reader-title-overlay"),
    ) {
        ReaderEdgeGradient(
            color = scrimColor,
            edge = ReaderEdge.Top,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 38.dp,
                    top = 24.dp,
                    end = 38.dp,
                ),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                status?.let {
                    Spacer(Modifier.width(18.dp))
                    Text(
                        text = it,
                        color = mutedColor,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
            }
            chapter?.let {
                Text(
                    text = it.title,
                    color = mutedColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

internal enum class ReaderEdge {
    Top,
    Bottom,
}

@Composable
internal fun ReaderEdgeGradient(
    color: Color,
    edge: ReaderEdge,
    modifier: Modifier = Modifier,
) {
    val opaqueColor = color.copy(alpha = 0.8f)
    val transparentColor = color.copy(alpha = 0f)
    val colors = when (edge) {
        ReaderEdge.Top -> listOf(opaqueColor, transparentColor)
        ReaderEdge.Bottom -> listOf(transparentColor, opaqueColor)
    }
    Box(
        modifier = modifier
            .height(READER_EDGE_HEIGHT)
            .background(Brush.verticalGradient(colors)),
    )
}

private val READER_EDGE_HEIGHT = 80.dp
private val READER_DISABLED_CONTROL_SURFACE = Color(0xFF626D7D)

@Composable
private fun ReaderBottomControls(
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    chaptersEnabled: Boolean,
    retryImagesEnabled: Boolean,
    scrimColor: Color,
    focusRequesters: Map<ReaderControlTarget, FocusRequester>,
    onFocused: (ReaderControlTarget) -> Unit,
    onPrevious: () -> Unit,
    onChapters: () -> Unit,
    onSettings: () -> Unit,
    onRetryImages: () -> Unit,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        ReaderEdgeGradient(
            color = scrimColor,
            edge = ReaderEdge.Bottom,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .testTag("reader-bottom-gradient"),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp, top = 54.dp, bottom = 30.dp)
                .testTag("reader-bottom-controls"),
            horizontalArrangement = Arrangement.Center,
        ) {
            ReaderControlButton(
                text = stringResource(R.string.reader_previous_chapter),
                iconRes = R.drawable.ic_action_previous,
                iconTag = "reader-previous-chapter-icon",
                enabled = previousEnabled,
                focusRequester = focusRequesters.getValue(ReaderControlTarget.PreviousChapter),
                onFocused = { onFocused(ReaderControlTarget.PreviousChapter) },
                onClick = onPrevious,
            )
            Spacer(Modifier.width(14.dp))
            ReaderControlButton(
                text = stringResource(R.string.reader_chapters),
                iconRes = R.drawable.ic_settings_reading,
                iconTag = "reader-chapters-icon",
                enabled = chaptersEnabled,
                focusRequester = focusRequesters.getValue(ReaderControlTarget.Chapters),
                onFocused = { onFocused(ReaderControlTarget.Chapters) },
                onClick = onChapters,
            )
            Spacer(Modifier.width(14.dp))
            ReaderControlButton(
                text = stringResource(R.string.reader_settings),
                iconRes = R.drawable.ic_nav_settings,
                iconTag = "reader-settings-icon",
                enabled = true,
                focusRequester = focusRequesters.getValue(ReaderControlTarget.Settings),
                onFocused = { onFocused(ReaderControlTarget.Settings) },
                onClick = onSettings,
            )
            if (retryImagesEnabled) {
                Spacer(Modifier.width(14.dp))
                ReaderControlButton(
                    text = stringResource(R.string.reader_image_retry),
                    iconRes = R.drawable.ic_refresh,
                    iconTag = "reader-retry-images-icon",
                    enabled = true,
                    focusRequester = focusRequesters.getValue(ReaderControlTarget.RetryImages),
                    onFocused = { onFocused(ReaderControlTarget.RetryImages) },
                    onClick = onRetryImages,
                )
            }
            Spacer(Modifier.width(14.dp))
            ReaderControlButton(
                text = stringResource(R.string.reader_next_chapter),
                iconRes = R.drawable.ic_action_next,
                iconTag = "reader-next-chapter-icon",
                enabled = nextEnabled,
                focusRequester = focusRequesters.getValue(ReaderControlTarget.NextChapter),
                onFocused = { onFocused(ReaderControlTarget.NextChapter) },
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun ReaderControlButton(
    text: String,
    @DrawableRes iconRes: Int,
    iconTag: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    KaloscopeButton(
        onClick = onClick,
        enabled = enabled,
        size = KaloscopeControlSize.Row,
        disabledContainerColor = READER_DISABLED_CONTROL_SURFACE,
        modifier = Modifier
            .width(160.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() },
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(22.dp)
                .testTag(iconTag),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, maxLines = 1)
    }
}

@Composable
private fun ReaderChapterDrawer(
    chapters: List<ReaderChapter>,
    selectedIndex: Int?,
    order: ReaderChapterOrder,
    panelColor: Color,
    textColor: Color,
    mutedColor: Color,
    controlContentColor: Color,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val requesters = remember(chapters) {
        chapters.associate { it.id to FocusRequester() }
    }
    val groups = ReaderChapterPolicy.displayGroups(chapters, order)
    val listState = rememberLazyListState()
    val selectedChapterId = selectedIndex
        ?.let(chapters::getOrNull)
        ?.id
    val selectedListIndex = remember(groups, selectedChapterId) {
        var itemIndex = 0
        var match: Int? = null
        groups.forEach { group ->
            if (group.volume != null) {
                itemIndex += 1
            }
            group.chapters.forEach { chapter ->
                if (chapter.id == selectedChapterId) {
                    match = itemIndex
                }
                itemIndex += 1
            }
        }
        match
    }
    LaunchedEffect(chapters, selectedIndex, order) {
        selectedListIndex?.let { listState.scrollToItem(it) }
        withFrameNanos { }
        selectedChapterId
            ?.let(requesters::get)
            ?.requestFocus()
            ?: groups.firstOrNull()?.chapters?.firstOrNull()?.id
                ?.let(requesters::get)
                ?.requestFocus()
    }
    KaloscopeSidePanel(
        title = stringResource(R.string.reader_chapters),
        palette = KaloscopeSidePanelPalette(
            panelColor = panelColor,
            panelAlpha = 1f,
            textColor = textColor,
            mutedColor = mutedColor,
            controlContentColor = controlContentColor,
        ),
        onDismiss = onDismiss,
        side = KaloscopeSidePanelSide.Start,
        modifier = Modifier.testTag("reader-chapter-drawer"),
    ) {
        if (chapters.isEmpty()) {
            Text(stringResource(R.string.reader_no_chapters), color = mutedColor)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                groups.forEach { group ->
                    group.volume?.let { volume ->
                        item(key = "volume:$volume") {
                            KaloscopeSidePanelSectionHeader(
                                title = volume,
                                color = mutedColor,
                            )
                        }
                    }
                    items(group.chapters, key = ReaderChapter::id) { chapter ->
                        val sourceIndex = chapters.indexOfFirst { it.id == chapter.id }
                        KaloscopeSidePanelSelectionRow(
                            title = chapter.title,
                            onClick = { onSelect(sourceIndex) },
                            selected = sourceIndex == selectedIndex,
                            modifier = Modifier
                                .focusRequester(requesters.getValue(chapter.id)),
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageReaderSettingsDrawer(
    settings: ImageReaderSettings,
    chapterOrder: ReaderChapterOrder,
    onSettings: (ImageReaderSettings) -> Unit,
    onChapterOrder: (ReaderChapterOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    ReaderSettingsDrawerFrame(
        panelColor = ReaderDrawerPanelColor,
        textColor = ReaderDrawerTextColor,
        mutedColor = ReaderDrawerMutedColor,
        controlContentColor = ReaderDrawerTextColor,
        onDismiss = onDismiss,
    ) { onOpenChoice ->
        ReaderChoiceSettingRow(
            title = stringResource(R.string.reader_chapter_order),
            values = ReaderChapterOrder.entries,
            selected = chapterOrder,
            label = ::readerChapterOrderLabel,
            onSelect = onChapterOrder,
            onOpenChoice = onOpenChoice,
            optionTestTag = {
                "reader-chapter-order-option-${it.name.lowercase()}"
            },
            requestInitialFocus = true,
            testTag = "reader-chapter-order-setting",
        )
        ReaderChoiceSettingRow(
            title = stringResource(R.string.reader_image_read_mode),
            values = ImageReadMode.entries,
            selected = settings.readMode,
            label = ::imageReadModeLabel,
            onSelect = { onSettings(settings.copy(readMode = it)) },
            onOpenChoice = onOpenChoice,
            testTag = "reader-image-read-mode-setting",
        )
        ReaderChoiceSettingRow(
            title = stringResource(R.string.reader_image_zoom),
            values = ImageZoomMode.entries,
            selected = settings.zoomMode,
            label = ::imageZoomModeLabel,
            onSelect = { onSettings(settings.copy(zoomMode = it)) },
            onOpenChoice = onOpenChoice,
            testTag = "reader-image-zoom-setting",
        )
        ReaderChoiceSettingRow(
            title = stringResource(R.string.reader_page_direction),
            values = ImagePageDirection.entries,
            selected = settings.pageDirection,
            label = ::imagePageDirectionLabel,
            onSelect = { onSettings(settings.copy(pageDirection = it)) },
            onOpenChoice = onOpenChoice,
            testTag = "reader-page-direction-setting",
        )
    }
}

@Composable
private fun TextReaderSettingsDrawer(
    settings: TextReaderSettings,
    chapterOrder: ReaderChapterOrder,
    panelColor: Color,
    textColor: Color,
    mutedColor: Color,
    controlContentColor: Color,
    onSettings: (TextReaderSettings) -> Unit,
    onChapterOrder: (ReaderChapterOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val dimensions = settings.toDpDimensions(LocalDensity.current)
    ReaderSettingsDrawerFrame(
        panelColor = panelColor,
        textColor = textColor,
        mutedColor = mutedColor,
        controlContentColor = controlContentColor,
        onDismiss = onDismiss,
    ) { onOpenChoice ->
        ReaderChoiceSettingRow(
            title = stringResource(R.string.reader_chapter_order),
            values = ReaderChapterOrder.entries,
            selected = chapterOrder,
            label = ::readerChapterOrderLabel,
            onSelect = onChapterOrder,
            onOpenChoice = onOpenChoice,
            optionTestTag = {
                "reader-chapter-order-option-${it.name.lowercase()}"
            },
            requestInitialFocus = true,
            testTag = "reader-chapter-order-setting",
        )
        ReaderChoiceSettingRow(
            title = stringResource(R.string.reader_text_theme),
            values = TextReaderTheme.entries,
            selected = settings.theme,
            label = ::textReaderThemeLabel,
            onSelect = { onSettings(settings.copy(theme = it)) },
            onOpenChoice = onOpenChoice,
            swatchColor = TextReaderTheme::readerBackgroundColor,
            optionTestTag = { "reader-theme-option-${it.name.lowercase()}" },
            optionSwatchTestTag = { "reader-theme-swatch-${it.name.lowercase()}" },
            testTag = "reader-text-theme-setting",
        )
        ReaderChoiceSettingRow(
            title = stringResource(R.string.reader_text_font),
            values = TextReaderFont.entries,
            selected = settings.font,
            label = ::textReaderFontLabel,
            onSelect = { onSettings(settings.copy(font = it)) },
            onOpenChoice = onOpenChoice,
            testTag = "reader-text-font-setting",
        )
        ReaderNumericSettingRow(
            title = stringResource(R.string.reader_font_size),
            value = stringResource(
                R.string.reader_dp_value,
                dimensions.fontSize.value.roundToInt(),
            ),
            canDecrease = settings.fontSizeSp > ReaderSettingsPolicy.MIN_FONT_SIZE_SP,
            canIncrease = settings.fontSizeSp < ReaderSettingsPolicy.MAX_FONT_SIZE_SP,
            testTag = "reader-font-size-setting",
            adjustmentTestTagPrefix = "reader-font-size",
            onDecrease = {
                onSettings(
                    settings.copy(
                        fontSizeSp = settings.fontSizeSp - ReaderSettingsPolicy.FONT_SIZE_STEP_SP,
                    ),
                )
            },
            onIncrease = {
                onSettings(
                    settings.copy(
                        fontSizeSp = settings.fontSizeSp + ReaderSettingsPolicy.FONT_SIZE_STEP_SP,
                    ),
                )
            },
        )
        ReaderNumericSettingRow(
            title = stringResource(R.string.reader_line_height),
            value = stringResource(R.string.reader_multiplier_value, settings.lineHeight),
            canDecrease = settings.lineHeight > ReaderSettingsPolicy.MIN_LINE_HEIGHT,
            canIncrease = settings.lineHeight < ReaderSettingsPolicy.MAX_LINE_HEIGHT,
            testTag = "reader-line-height-setting",
            adjustmentTestTagPrefix = "reader-line-height",
            onDecrease = {
                onSettings(
                    settings.copy(
                        lineHeight = settings.lineHeight - ReaderSettingsPolicy.LINE_HEIGHT_STEP,
                    ),
                )
            },
            onIncrease = {
                onSettings(
                    settings.copy(
                        lineHeight = settings.lineHeight + ReaderSettingsPolicy.LINE_HEIGHT_STEP,
                    ),
                )
            },
        )
        ReaderNumericSettingRow(
            title = stringResource(R.string.reader_paragraph_spacing),
            value = stringResource(
                R.string.reader_dp_value,
                dimensions.paragraphSpacing.value.roundToInt(),
            ),
            canDecrease = settings.paragraphSpacingEm >
                ReaderSettingsPolicy.MIN_PARAGRAPH_SPACING_EM,
            canIncrease = settings.paragraphSpacingEm <
                ReaderSettingsPolicy.MAX_PARAGRAPH_SPACING_EM,
            testTag = "reader-paragraph-spacing-setting",
            adjustmentTestTagPrefix = "reader-paragraph-spacing",
            onDecrease = {
                onSettings(
                    settings.copy(
                        paragraphSpacingEm = settings.paragraphSpacingEm -
                            ReaderSettingsPolicy.PARAGRAPH_SPACING_STEP_EM,
                    ),
                )
            },
            onIncrease = {
                onSettings(
                    settings.copy(
                        paragraphSpacingEm = settings.paragraphSpacingEm +
                            ReaderSettingsPolicy.PARAGRAPH_SPACING_STEP_EM,
                    ),
                )
            },
        )
        ReaderNumericSettingRow(
            title = stringResource(R.string.reader_horizontal_padding),
            value = stringResource(
                R.string.reader_dp_value,
                dimensions.horizontalPadding.value.roundToInt(),
            ),
            canDecrease = settings.horizontalPaddingDp >
                ReaderSettingsPolicy.MIN_HORIZONTAL_PADDING_DP,
            canIncrease = settings.horizontalPaddingDp <
                ReaderSettingsPolicy.MAX_HORIZONTAL_PADDING_DP,
            testTag = "reader-horizontal-padding-setting",
            adjustmentTestTagPrefix = "reader-horizontal-padding",
            onDecrease = {
                onSettings(
                    settings.copy(
                        horizontalPaddingDp = settings.horizontalPaddingDp -
                            ReaderSettingsPolicy.HORIZONTAL_PADDING_STEP_DP,
                    ),
                )
            },
            onIncrease = {
                onSettings(
                    settings.copy(
                        horizontalPaddingDp = settings.horizontalPaddingDp +
                            ReaderSettingsPolicy.HORIZONTAL_PADDING_STEP_DP,
                    ),
                )
            },
        )
    }
}

@Composable
private fun ReaderSettingsDrawerFrame(
    panelColor: Color,
    textColor: Color,
    mutedColor: Color,
    controlContentColor: Color = textColor,
    onDismiss: () -> Unit,
    content: @Composable (
        onOpenChoice: (FocusRequester, ReaderSettingsChoice) -> Unit,
    ) -> Unit,
) {
    var activeChoice by remember { mutableStateOf<ReaderSettingsChoice?>(null) }
    var choiceTrigger by remember { mutableStateOf<FocusRequester?>(null) }
    var focusToRestore by remember { mutableStateOf<FocusRequester?>(null) }
    LaunchedEffect(focusToRestore) {
        val requester = focusToRestore ?: return@LaunchedEffect
        withFrameNanos { }
        requester.requestFocus()
        focusToRestore = null
    }
    fun dismissChoice() {
        activeChoice = null
        focusToRestore = choiceTrigger
        choiceTrigger = null
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        KaloscopeSidePanel(
            title = stringResource(R.string.reader_settings),
            palette = KaloscopeSidePanelPalette(
                panelColor = panelColor,
                panelAlpha = 1f,
                textColor = textColor,
                mutedColor = mutedColor,
                controlContentColor = controlContentColor,
            ),
            onDismiss = onDismiss,
            dismissEnabled = activeChoice == null,
            modifier = Modifier.testTag("reader-settings-drawer"),
            footer = {
                KaloscopeSidePanelSessionHint(
                    text = stringResource(R.string.reader_session_settings_description),
                    color = mutedColor,
                    iconTestTag = "reader-session-settings-hint-icon",
                    textTestTag = "reader-session-settings-hint-text",
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        content { trigger, choice ->
                            choiceTrigger = trigger
                            activeChoice = choice
                        }
                    }
                }
            }
        }

        activeChoice?.let { choice ->
            KaloscopeChoiceDialog(
                title = choice.title,
                options = choice.options,
                viewportSize = DpSize(maxWidth, maxHeight),
                onDismiss = ::dismissChoice,
            )
        }
    }
}

@Composable
private fun <T> ReaderChoiceSettingRow(
    title: String,
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onOpenChoice: (FocusRequester, ReaderSettingsChoice) -> Unit,
    requestInitialFocus: Boolean = false,
    swatchColor: ((T) -> Color)? = null,
    optionTestTag: ((T) -> String)? = null,
    optionSwatchTestTag: ((T) -> String)? = null,
    testTag: String,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            withFrameNanos { }
            focus.requestFocus()
        }
    }
    val choice = ReaderSettingsChoice(
        title = title,
        options = values.map { option ->
            KaloscopeChoiceDialogOption(
                label = label(option),
                selected = { option == selected },
                swatchColor = swatchColor?.invoke(option),
                testTag = optionTestTag?.invoke(option),
                swatchTestTag = optionSwatchTestTag?.invoke(option),
                onSelect = { onSelect(option) },
            )
        },
    )
    KaloscopeSidePanelChoiceRow(
        title = title,
        value = label(selected),
        onClick = { onOpenChoice(focus, choice) },
        modifier = Modifier
            .focusRequester(focus)
            .testTag(testTag),
    )
}

@Composable
private fun ReaderNumericSettingRow(
    title: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    testTag: String,
    adjustmentTestTagPrefix: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    KaloscopeSidePanelAdjustmentRow(
        title = title,
        value = value,
        canDecrease = canDecrease,
        canIncrease = canIncrease,
        onDecrease = onDecrease,
        onIncrease = onIncrease,
        modifier = Modifier.testTag(testTag),
        adjustmentTestTagPrefix = adjustmentTestTagPrefix,
    )
}

@Composable
private fun ReaderRecoverableError(
    message: String,
    detail: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)?,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .padding(bottom = 122.dp)
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(12.dp)
            .testTag("reader-recoverable-error"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(360.dp)) {
            Text(message, color = Color.White, fontWeight = FontWeight.Bold)
            Text(detail, color = Color.LightGray, fontSize = 12.sp)
        }
        onRetry?.let { retry ->
            KaloscopeButton(onClick = retry) {
                Text(stringResource(R.string.retry))
            }
        }
        Spacer(Modifier.width(8.dp))
        KaloscopeButton(onClick = onDismiss) {
            Text(stringResource(R.string.reader_close))
        }
    }
}

@Composable
private fun ReaderUnavailable(
    onBack: () -> Unit,
    error: ReaderUiState.Error? = null,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.reader_request_missing),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = error?.let { appErrorText(it.error) }
                    ?: stringResource(R.string.reader_request_missing_description),
                color = Color.LightGray,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(18.dp))
            KaloscopeButton(
                onClick = onBack,
                modifier = Modifier.focusRequester(focus),
            ) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

private fun defaultControlTarget(chapters: List<ReaderChapter>): ReaderControlTarget =
    if (chapters.size > 1) ReaderControlTarget.Chapters else ReaderControlTarget.Settings

private enum class ReaderDrawer {
    Chapters,
    Settings,
}

private data class ReaderSettingsChoice(
    val title: String,
    val options: List<KaloscopeChoiceDialogOption>,
)

private val ReaderDrawerPanelColor = Color.Black
private val ReaderDrawerTextColor = OnBackground
private val ReaderDrawerMutedColor = Muted

private const val TITLE_HIDE_DELAY_MILLIS = 3_000L
