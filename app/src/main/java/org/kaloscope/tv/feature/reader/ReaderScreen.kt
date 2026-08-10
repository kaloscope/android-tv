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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelAdjustmentRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSectionHeader
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSelectionRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSessionHint
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSide
import org.kaloscope.tv.core.designsystem.appErrorText
import org.kaloscope.tv.core.designsystem.readerBackgroundColor
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
    val overlayPanel = textPalette?.panel ?: Color(0xFF121212)
    val overlayBar = textPalette?.overlay ?: Color.Black.copy(alpha = 0.82f)
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

            is ReaderUiState.Text -> TextReaderSurface(
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

        AnimatedVisibility(
            visible = initialTitleVisible || controlsVisible,
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
                panelColor = overlayBar,
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
                panelColor = overlayPanel,
                textColor = overlayText,
                mutedColor = overlayMuted,
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
                    panelColor = overlayPanel,
                    textColor = overlayText,
                    mutedColor = overlayMuted,
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
private fun ReaderTitleOverlay(
    title: String,
    chapter: ReaderChapter?,
    textColor: Color,
    mutedColor: Color,
    scrimColor: Color,
    status: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reader-title-overlay"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reader-title-solid-scrim")
                .background(scrimColor)
                .padding(
                    start = 38.dp,
                    top = 24.dp,
                    end = 38.dp,
                    bottom = 12.dp,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag("reader-title-gradient-tail")
                .background(
                    Brush.verticalGradient(
                        listOf(scrimColor, scrimColor.copy(alpha = 0f)),
                    ),
                ),
        )
    }
}

@Composable
private fun ReaderBottomControls(
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    chaptersEnabled: Boolean,
    retryImagesEnabled: Boolean,
    panelColor: Color,
    focusRequesters: Map<ReaderControlTarget, FocusRequester>,
    onFocused: (ReaderControlTarget) -> Unit,
    onPrevious: () -> Unit,
    onChapters: () -> Unit,
    onSettings: () -> Unit,
    onRetryImages: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, panelColor)),
            )
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
            textColor = textColor,
            mutedColor = mutedColor,
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
        panelColor = Color(0xFF121212),
        textColor = Color.White,
        mutedColor = Color(0xFFAAAAAA),
        onDismiss = onDismiss,
    ) {
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_chapter_order),
            value = chapterOrder.label(),
            values = ReaderChapterOrder.entries,
            selected = chapterOrder,
            onSelect = onChapterOrder,
            requestInitialFocus = true,
            testTag = "reader-chapter-order-setting",
            adjustmentTestTagPrefix = "reader-chapter-order",
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_image_read_mode),
            value = settings.readMode.label(),
            values = ImageReadMode.entries,
            selected = settings.readMode,
            onSelect = { onSettings(settings.copy(readMode = it)) },
            testTag = "reader-image-read-mode-setting",
            adjustmentTestTagPrefix = "reader-image-read-mode",
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_image_zoom),
            value = settings.zoomMode.label(),
            values = ImageZoomMode.entries,
            selected = settings.zoomMode,
            onSelect = { onSettings(settings.copy(zoomMode = it)) },
            testTag = "reader-image-zoom-setting",
            adjustmentTestTagPrefix = "reader-image-zoom",
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_page_direction),
            value = settings.pageDirection.label(),
            values = ImagePageDirection.entries,
            selected = settings.pageDirection,
            onSelect = { onSettings(settings.copy(pageDirection = it)) },
            testTag = "reader-page-direction-setting",
            adjustmentTestTagPrefix = "reader-page-direction",
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
    onSettings: (TextReaderSettings) -> Unit,
    onChapterOrder: (ReaderChapterOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    ReaderSettingsDrawerFrame(
        panelColor = panelColor,
        textColor = textColor,
        mutedColor = mutedColor,
        onDismiss = onDismiss,
    ) {
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_chapter_order),
            value = chapterOrder.label(),
            values = ReaderChapterOrder.entries,
            selected = chapterOrder,
            onSelect = onChapterOrder,
            requestInitialFocus = true,
            testTag = "reader-chapter-order-setting",
            adjustmentTestTagPrefix = "reader-chapter-order",
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_text_theme),
            value = settings.theme.label(),
            values = TextReaderTheme.entries,
            selected = settings.theme,
            onSelect = { onSettings(settings.copy(theme = it)) },
            valueSwatchColor = settings.theme.readerBackgroundColor(),
            testTag = "reader-text-theme-setting",
            adjustmentTestTagPrefix = "reader-text-theme",
            swatchTestTag = "reader-current-theme-swatch",
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_text_font),
            value = settings.font.label(),
            values = TextReaderFont.entries,
            selected = settings.font,
            onSelect = { onSettings(settings.copy(font = it)) },
            testTag = "reader-text-font-setting",
            adjustmentTestTagPrefix = "reader-text-font",
        )
        ReaderNumericSettingRow(
            title = stringResource(R.string.reader_font_size),
            value = stringResource(R.string.reader_sp_value, settings.fontSizeSp),
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
            value = stringResource(R.string.reader_em_value, settings.paragraphSpacingEm),
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
            value = stringResource(R.string.reader_dp_value, settings.horizontalPaddingDp),
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
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    KaloscopeSidePanel(
        title = stringResource(R.string.reader_settings),
        palette = KaloscopeSidePanelPalette(
            panelColor = panelColor,
            textColor = textColor,
            mutedColor = mutedColor,
        ),
        onDismiss = onDismiss,
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
                    content()
                }
            }
        }
    }
}

@Composable
private fun <T> ReaderEnumSettingRow(
    title: String,
    value: String,
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    requestInitialFocus: Boolean = false,
    valueSwatchColor: Color? = null,
    testTag: String? = null,
    adjustmentTestTagPrefix: String? = null,
    swatchTestTag: String? = null,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            withFrameNanos { }
            focus.requestFocus()
        }
    }
    val currentIndex = values.indexOf(selected).coerceAtLeast(0)
    val canDecrease = currentIndex > 0
    val canIncrease = currentIndex < values.lastIndex
    fun move(offset: Int) {
        values.getOrNull(currentIndex + offset)?.let(onSelect)
    }
    KaloscopeSidePanelAdjustmentRow(
        title = title,
        value = value,
        canDecrease = canDecrease,
        canIncrease = canIncrease,
        onDecrease = { move(-1) },
        onIncrease = { move(1) },
        modifier = Modifier
            .then(
                if (requestInitialFocus) {
                    Modifier.focusRequester(focus)
                } else {
                    Modifier
                },
            )
            .then(testTag?.let(Modifier::testTag) ?: Modifier),
        valueSwatchColor = valueSwatchColor,
        adjustmentTestTagPrefix = adjustmentTestTagPrefix,
        swatchTestTag = swatchTestTag,
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

@Composable
private fun ReaderChapterOrder.label(): String =
    stringResource(
        if (this == ReaderChapterOrder.Ascending) {
            R.string.reader_order_ascending
        } else {
            R.string.reader_order_descending
        },
    )

@Composable
private fun ImageReadMode.label(): String = stringResource(
    if (this == ImageReadMode.Scroll) R.string.reader_mode_scroll else R.string.reader_mode_paged,
)

@Composable
private fun ImageZoomMode.label(): String = stringResource(
    when (this) {
        ImageZoomMode.Auto -> R.string.reader_zoom_auto
        ImageZoomMode.FitWidth -> R.string.reader_zoom_fit_width
        ImageZoomMode.FitHeight -> R.string.reader_zoom_fit_height
    },
)

@Composable
private fun ImagePageDirection.label(): String = stringResource(
    when (this) {
        ImagePageDirection.Right -> R.string.reader_direction_right
        ImagePageDirection.Left -> R.string.reader_direction_left
        ImagePageDirection.Down -> R.string.reader_direction_down
    },
)

@Composable
private fun TextReaderTheme.label(): String = stringResource(
    when (this) {
        TextReaderTheme.White -> R.string.reader_theme_white
        TextReaderTheme.Cream -> R.string.reader_theme_cream
        TextReaderTheme.Sepia -> R.string.reader_theme_sepia
        TextReaderTheme.LightGray -> R.string.reader_theme_light_gray
        TextReaderTheme.Green -> R.string.reader_theme_green
        TextReaderTheme.Dark -> R.string.reader_theme_dark
        TextReaderTheme.Slate -> R.string.reader_theme_slate
        TextReaderTheme.Black -> R.string.reader_theme_black
    },
)

@Composable
private fun TextReaderFont.label(): String = stringResource(
    when (this) {
        TextReaderFont.System -> R.string.reader_font_system
        TextReaderFont.Sans -> R.string.reader_font_sans
        TextReaderFont.Serif -> R.string.reader_font_serif
        TextReaderFont.Kai -> R.string.reader_font_kai
        TextReaderFont.Monospace -> R.string.reader_font_monospace
    },
)

private fun defaultControlTarget(chapters: List<ReaderChapter>): ReaderControlTarget =
    if (chapters.size > 1) ReaderControlTarget.Chapters else ReaderControlTarget.Settings

private enum class ReaderDrawer {
    Chapters,
    Settings,
}

private const val TITLE_HIDE_DELAY_MILLIS = 3_000L
