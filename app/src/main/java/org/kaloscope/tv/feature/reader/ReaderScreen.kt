package org.kaloscope.tv.feature.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.appErrorText
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
    var titleInteractionRevision by remember(state.requestId) { mutableIntStateOf(0) }
    var transientTitleVisible by remember(state.requestId) { mutableStateOf(true) }
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
    LaunchedEffect(
        state.requestId,
        state.contentRevision,
        titleInteractionRevision,
        controlsVisible,
        drawer,
    ) {
        transientTitleVisible = true
        if (!controlsVisible && drawer == null) {
            delay(TITLE_HIDE_DELAY_MILLIS)
            transientTitleVisible = false
        }
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

    BackHandler {
        when {
            drawer != null -> {
                drawer = null
                controlsVisible = true
                pendingControlFocus = entryControlTarget()
            }

            controlsVisible -> {
                controlsVisible = false
                contentFocus.requestFocus()
            }

            else -> onBack()
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
                controlsVisible = controlsVisible,
                focusRequester = contentFocus,
                onToggleControls = {
                    controlsVisible = !controlsVisible
                    pendingControlFocus = null
                    contentFocus.requestFocus()
                },
                onEnterControls = {
                    requestControl(entryControlTarget())
                },
                onBoundary = ::openBoundary,
                onLoadMore = onLoadMoreImages,
                onNavigate = { titleInteractionRevision += 1 },
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
                onToggleControls = {
                    controlsVisible = !controlsVisible
                    pendingControlFocus = null
                    contentFocus.requestFocus()
                },
                onEnterControls = {
                    requestControl(entryControlTarget())
                },
                onBoundary = ::openBoundary,
                onNavigate = { titleInteractionRevision += 1 },
            )
        }

        AnimatedVisibility(
            visible = transientTitleVisible || controlsVisible || drawer != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTitleOverlay(
                title = content.title,
                chapter = content.selectedChapterIndex?.let(content.chapters::getOrNull),
                textColor = overlayText,
                mutedColor = overlayMuted,
                barColor = overlayBar,
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
                onSelect = { chapterIndex ->
                    drawer = null
                    pendingControlFocus = entryControlTarget()
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
                )

                is ReaderUiState.Text -> TextReaderSettingsDrawer(
                    settings = state.settings,
                    chapterOrder = state.chapterOrder,
                    panelColor = overlayPanel,
                    textColor = overlayText,
                    mutedColor = overlayMuted,
                    onSettings = onTextSettings,
                    onChapterOrder = onChapterOrder,
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
            if (state.isLoadingMore) {
                Text(
                    text = stringResource(R.string.reader_loading_more),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(28.dp)
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(12.dp),
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
    barColor: Color,
    status: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reader-title-overlay")
            .background(
                Brush.verticalGradient(
                    listOf(barColor, Color.Transparent),
                ),
            )
            .padding(horizontal = 38.dp, vertical = 24.dp),
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
            enabled = previousEnabled,
            focusRequester = focusRequesters.getValue(ReaderControlTarget.PreviousChapter),
            onFocused = { onFocused(ReaderControlTarget.PreviousChapter) },
            onClick = onPrevious,
        )
        Spacer(Modifier.width(14.dp))
        ReaderControlButton(
            text = stringResource(R.string.reader_chapters),
            enabled = chaptersEnabled,
            focusRequester = focusRequesters.getValue(ReaderControlTarget.Chapters),
            onFocused = { onFocused(ReaderControlTarget.Chapters) },
            onClick = onChapters,
        )
        Spacer(Modifier.width(14.dp))
        ReaderControlButton(
            text = stringResource(R.string.reader_settings),
            enabled = true,
            focusRequester = focusRequesters.getValue(ReaderControlTarget.Settings),
            onFocused = { onFocused(ReaderControlTarget.Settings) },
            onClick = onSettings,
        )
        if (retryImagesEnabled) {
            Spacer(Modifier.width(14.dp))
            ReaderControlButton(
                text = stringResource(R.string.reader_image_retry),
                enabled = true,
                focusRequester = focusRequesters.getValue(ReaderControlTarget.RetryImages),
                onFocused = { onFocused(ReaderControlTarget.RetryImages) },
                onClick = onRetryImages,
            )
        }
        Spacer(Modifier.width(14.dp))
        ReaderControlButton(
            text = stringResource(R.string.reader_next_chapter),
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
    onSelect: (Int) -> Unit,
) {
    val requesters = remember(chapters) {
        chapters.associate { it.id to FocusRequester() }
    }
    val groups = ReaderChapterPolicy.displayGroups(chapters, order)
    LaunchedEffect(chapters, selectedIndex, order) {
        withFrameNanos { }
        selectedIndex
            ?.let(chapters::getOrNull)
            ?.id
            ?.let(requesters::get)
            ?.requestFocus()
            ?: groups.firstOrNull()?.chapters?.firstOrNull()?.id
                ?.let(requesters::get)
                ?.requestFocus()
    }
    ReaderDrawerFrame(
        title = stringResource(R.string.reader_chapters),
        panelColor = panelColor,
        textColor = textColor,
        alignment = Alignment.CenterStart,
        modifier = Modifier.testTag("reader-chapter-drawer"),
    ) {
        if (chapters.isEmpty()) {
            Text(stringResource(R.string.reader_no_chapters), color = mutedColor)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .focusGroup()
                    .focusProperties {
                        onExit = { cancelFocusChange() }
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.forEach { group ->
                    group.volume?.let { volume ->
                        item(key = "volume:$volume") {
                            Text(
                                text = volume,
                                color = mutedColor,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                    }
                    items(group.chapters, key = ReaderChapter::id) { chapter ->
                        val sourceIndex = chapters.indexOfFirst { it.id == chapter.id }
                        KaloscopeButton(
                            onClick = { onSelect(sourceIndex) },
                            selected = sourceIndex == selectedIndex,
                            size = KaloscopeControlSize.Row,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(requesters.getValue(chapter.id)),
                        ) {
                            Text(chapter.title, maxLines = 2)
                        }
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
) {
    ReaderSettingsDrawerFrame(
        panelColor = Color(0xFF121212),
        textColor = Color.White,
        mutedColor = Color(0xFFAAAAAA),
    ) {
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_chapter_order),
            value = chapterOrder.label(),
            values = ReaderChapterOrder.entries,
            selected = chapterOrder,
            onSelect = onChapterOrder,
            requestInitialFocus = true,
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_image_read_mode),
            value = settings.readMode.label(),
            values = ImageReadMode.entries,
            selected = settings.readMode,
            onSelect = { onSettings(settings.copy(readMode = it)) },
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_image_zoom),
            value = settings.zoomMode.label(),
            values = ImageZoomMode.entries,
            selected = settings.zoomMode,
            onSelect = { onSettings(settings.copy(zoomMode = it)) },
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_page_direction),
            value = settings.pageDirection.label(),
            values = ImagePageDirection.entries,
            selected = settings.pageDirection,
            onSelect = { onSettings(settings.copy(pageDirection = it)) },
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
) {
    ReaderSettingsDrawerFrame(panelColor, textColor, mutedColor) {
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_chapter_order),
            value = chapterOrder.label(),
            values = ReaderChapterOrder.entries,
            selected = chapterOrder,
            onSelect = onChapterOrder,
            requestInitialFocus = true,
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_text_theme),
            value = settings.theme.label(),
            values = TextReaderTheme.entries,
            selected = settings.theme,
            onSelect = { onSettings(settings.copy(theme = it)) },
        )
        ReaderEnumSettingRow(
            title = stringResource(R.string.reader_text_font),
            value = settings.font.label(),
            values = TextReaderFont.entries,
            selected = settings.font,
            onSelect = { onSettings(settings.copy(font = it)) },
        )
        ReaderNumericSettingRow(
            title = stringResource(R.string.reader_font_size),
            value = stringResource(R.string.reader_sp_value, settings.fontSizeSp),
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
    content: @Composable () -> Unit,
) {
    ReaderDrawerFrame(
        title = stringResource(R.string.reader_settings),
        panelColor = panelColor,
        textColor = textColor,
        modifier = Modifier.testTag("reader-settings-drawer"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .focusGroup()
                    .focusProperties {
                        onExit = { cancelFocusChange() }
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        content()
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reader_session_settings_description),
                color = mutedColor,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ReaderDrawerFrame(
    title: String,
    panelColor: Color,
    textColor: Color,
    alignment: Alignment = Alignment.CenterEnd,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f)),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .width(500.dp)
                .background(panelColor.copy(alpha = 0.99f))
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Box(modifier = Modifier.weight(1f)) { content() }
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
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            withFrameNanos { }
            focus.requestFocus()
        }
    }
    fun move(offset: Int) {
        val current = values.indexOf(selected).coerceAtLeast(0)
        val next = (current + offset).coerceIn(values.indices)
        onSelect(values[next])
    }
    ReaderSettingButton(
        title = title,
        value = value,
        focusRequester = focus.takeIf { requestInitialFocus },
        onDecrease = { move(-1) },
        onIncrease = { move(1) },
    )
}

@Composable
private fun ReaderNumericSettingRow(
    title: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    ReaderSettingButton(title, value, null, onDecrease, onIncrease)
}

@Composable
private fun ReaderSettingButton(
    title: String,
    value: String,
    focusRequester: FocusRequester?,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    KaloscopeButton(
        onClick = onIncrease,
        size = KaloscopeControlSize.Row,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onDecrease()
                        true
                    }

                    Key.DirectionRight -> {
                        onIncrease()
                        true
                    }

                    else -> false
                }
            },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, modifier = Modifier.weight(1f), maxLines = 1)
            Text(text = "‹  $value  ›", maxLines = 1)
        }
    }
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
