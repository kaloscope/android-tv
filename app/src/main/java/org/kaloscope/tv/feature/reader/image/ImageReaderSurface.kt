package org.kaloscope.tv.feature.reader.image

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ImageZoomMode
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.reader.ReaderBoundary
import org.kaloscope.tv.core.reader.ReaderDirection
import org.kaloscope.tv.core.reader.ReaderNavigationStep
import org.kaloscope.tv.core.reader.ReaderRemoteKeyPolicy
import org.kaloscope.tv.feature.reader.consumeReaderControlKey

@Composable
internal fun ImageReaderSurface(
    session: Session,
    content: ReaderImageContent,
    settings: ImageReaderSettings,
    contentRevision: Long,
    initialImageIndex: Int,
    imagesExhausted: Boolean,
    isLoadingMore: Boolean,
    controlsVisible: Boolean,
    focusRequester: FocusRequester,
    onToggleControls: () -> Unit,
    onEnterControls: () -> Unit,
    onBoundary: (ReaderBoundary) -> Unit,
    onLoadMore: () -> Unit,
    onPositionChanged: (Int) -> Unit,
    manualRetryRevision: Int,
    onFailedImagesChanged: (Boolean) -> Unit,
    onProgress: (ReaderPosition) -> Unit = {},
    initialImageOffset: Double = content.initialPosition.offset,
    initialImageAtBottom: Boolean = false,
) {
    val failedImages = remember(contentRevision) { mutableStateMapOf<String, Boolean>() }
    val preloadController = rememberReaderImagePreloadController(session)
    LaunchedEffect(failedImages.size) {
        onFailedImagesChanged(failedImages.isNotEmpty())
    }
    fun updateImageFailure(url: String, failed: Boolean) {
        if (failed) {
            failedImages[url] = true
        } else {
            failedImages.remove(url)
        }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (settings.readMode) {
            ImageReadMode.Scroll -> ScrollingImages(
                session = session,
                content = content,
                settings = settings,
                contentRevision = contentRevision,
                initialImageIndex = initialImageIndex,
                initialImageOffset = initialImageOffset,
                initialImageAtBottom = initialImageAtBottom,
                imagesExhausted = imagesExhausted,
                isLoadingMore = isLoadingMore,
                controlsVisible = controlsVisible,
                viewportHeight = maxHeight,
                focusRequester = focusRequester,
                onToggleControls = onToggleControls,
                onEnterControls = onEnterControls,
                onBoundary = onBoundary,
                onLoadMore = onLoadMore,
                onPositionChanged = onPositionChanged,
                onProgress = onProgress,
                manualRetryRevision = manualRetryRevision,
                onFinalFailureChanged = ::updateImageFailure,
                preloadController = preloadController,
            )

            ImageReadMode.Paged -> PagedImages(
                session = session,
                content = content,
                settings = settings,
                contentRevision = contentRevision,
                initialImageIndex = initialImageIndex,
                imagesExhausted = imagesExhausted,
                isLoadingMore = isLoadingMore,
                controlsVisible = controlsVisible,
                focusRequester = focusRequester,
                onToggleControls = onToggleControls,
                onEnterControls = onEnterControls,
                onBoundary = onBoundary,
                onLoadMore = onLoadMore,
                onPositionChanged = onPositionChanged,
                onProgress = onProgress,
                manualRetryRevision = manualRetryRevision,
                onFinalFailureChanged = ::updateImageFailure,
                preloadController = preloadController,
            )
        }
    }
}

@Composable
private fun ScrollingImages(
    session: Session,
    content: ReaderImageContent,
    settings: ImageReaderSettings,
    contentRevision: Long,
    initialImageIndex: Int,
    initialImageOffset: Double,
    initialImageAtBottom: Boolean,
    imagesExhausted: Boolean,
    isLoadingMore: Boolean,
    controlsVisible: Boolean,
    viewportHeight: Dp,
    focusRequester: FocusRequester,
    onToggleControls: () -> Unit,
    onEnterControls: () -> Unit,
    onBoundary: (ReaderBoundary) -> Unit,
    onLoadMore: () -> Unit,
    onPositionChanged: (Int) -> Unit,
    onProgress: (ReaderPosition) -> Unit,
    manualRetryRevision: Int,
    onFinalFailureChanged: (String, Boolean) -> Unit,
    preloadController: ReaderImagePreloadController,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialImageIndex)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val viewportPixels = with(density) { viewportHeight.roundToPx() }.coerceAtLeast(1)
    val imageHeights = remember(contentRevision, settings.zoomMode, viewportPixels) {
        mutableStateMapOf<Int, Int>()
    }
    var horizontalBias by remember(contentRevision) { mutableFloatStateOf(0f) }
    var scrollInProgress by remember(contentRevision) { mutableStateOf(false) }
    var advanceAfterLoad by remember(contentRevision) { mutableStateOf(false) }
    var loadStartImageCount by remember(contentRevision) {
        mutableIntStateOf(content.images.size)
    }
    var positionReady by remember(contentRevision) { mutableStateOf(false) }
    var initialImageSettled by remember(contentRevision) { mutableStateOf(false) }
    var lastImageSettled by remember(contentRevision, content.images.lastOrNull()) { mutableStateOf(false) }
    LaunchedEffect(contentRevision) {
        // Resume the current image on mode changes; a new chapter supplies index zero.
        positionReady = false
        listState.scrollToItem(initialImageIndex)
        if ((initialImageOffset > 0 || initialImageAtBottom) && settings.zoomMode == ImageZoomMode.FitWidth) {
            // A loading placeholder does not have the final image's aspect ratio.
            snapshotFlow { initialImageSettled }.first { it }
        }
        withFrameNanos { }
        val height = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == initialImageIndex }?.size ?: 0
        if (height > 0 && (initialImageOffset > 0 || initialImageAtBottom)) {
            val offset = if (initialImageAtBottom) {
                ContinuousImageScrollPolicy.bottomOffsetPx(height, viewportPixels)
            } else (height * initialImageOffset).toInt()
            listState.scrollToItem(initialImageIndex, offset)
        }
        horizontalBias = 0f
        positionReady = true
    }
    LaunchedEffect(settings.zoomMode) { horizontalBias = 0f }
    LaunchedEffect(contentRevision, listState, content.images) {
        snapshotFlow {
            ReaderImagePreloadPolicy.scrollingTarget(
                images = content.images,
                visibleItemIndices = listState.layoutInfo.visibleItemsInfo.map { it.index },
            )
        }
            .distinctUntilChanged()
            .collect(preloadController::updateTarget)
    }
    // Appended images change the position callback's bounds, even at the same visible index.
    LaunchedEffect(contentRevision, listState, content.images.size, positionReady, imagesExhausted) {
        if (!positionReady) return@LaunchedEffect
        snapshotFlow {
            val index = listState.firstVisibleItemIndex
            val height = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size ?: 0
            val offset = if (height > 0) {
                (listState.firstVisibleItemScrollOffset.toDouble() / height).coerceIn(0.0, 1.0)
            } else 0.0
            ReaderPosition(
                index, offset,
                chapterProgress = if (imagesExhausted && lastImageSettled && !listState.canScrollForward) 1.0
                    else (content.imageOffset + index + offset) / content.imageCount.coerceAtLeast(1),
            )
        }
            .distinctUntilChanged()
            .collect { position ->
                onPositionChanged(if (content.images.isEmpty()) 0 else position.index + 1)
                if (position.index in content.images.indices) onProgress(position)
            }
    }
    LaunchedEffect(isLoadingMore, content.images.size, advanceAfterLoad) {
        when {
            advanceAfterLoad &&
                !isLoadingMore &&
                content.images.size > loadStartImageCount -> {
                withFrameNanos { }
                listState.animateScrollToItem(loadStartImageCount)
                advanceAfterLoad = false
            }

            isLoadingMore && (advanceAfterLoad || content.images.isEmpty()) -> {
                withFrameNanos { }
                listState.animateScrollToItem(content.images.size)
            }
        }
    }
    val trailingAlignmentHeight = if (!isLoadingMore && content.images.isNotEmpty()) {
        val lastImageHeight = imageHeights[content.images.lastIndex] ?: 0
        with(density) {
            (viewportPixels - lastImageHeight).coerceAtLeast(0).toDp()
        }
    } else {
        0.dp
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .testTag("image-reader-scroll")
            .onPreviewKeyEvent { event ->
                if (
                    event.consumeReaderControlKey(
                        controlsVisible = controlsVisible,
                        onToggleControls = onToggleControls,
                        onEnterControls = onEnterControls,
                    )
                ) {
                    return@onPreviewKeyEvent true
                }
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (scrollInProgress || isLoadingMore || !positionReady) return@onPreviewKeyEvent true
                if (content.images.isEmpty()) {
                    when (event.key) {
                        Key.DirectionUp -> onBoundary(ReaderBoundary.Start)
                        Key.DirectionDown -> if (imagesExhausted) {
                            onBoundary(ReaderBoundary.End)
                        } else {
                            onLoadMore()
                        }

                        else -> return@onPreviewKeyEvent false
                    }
                    return@onPreviewKeyEvent true
                }
                if (settings.zoomMode == ImageZoomMode.FitHeight) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            horizontalBias = (horizontalBias - IMAGE_PAN_STEP).coerceAtLeast(-1f)
                            return@onPreviewKeyEvent true
                        }

                        Key.DirectionRight -> {
                            horizontalBias = (horizontalBias + IMAGE_PAN_STEP).coerceAtMost(1f)
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                val direction = event.key.toReaderDirection()
                    ?: return@onPreviewKeyEvent false
                val step = ReaderRemoteKeyPolicy.verticalStep(direction)
                val currentIndex = listState.firstVisibleItemIndex
                if (currentIndex !in content.images.indices) {
                    return@onPreviewKeyEvent true
                }
                val currentImageHeight = imageHeights[currentIndex]
                    ?: listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == currentIndex }
                        ?.size
                    ?: return@onPreviewKeyEvent true
                val decision = ContinuousImageScrollPolicy.decide(
                    step = step,
                    position = ContinuousImagePosition(
                        imageIndex = currentIndex,
                        offsetPx = listState.firstVisibleItemScrollOffset,
                    ),
                    currentImageHeightPx = currentImageHeight,
                    previousImageHeightPx = imageHeights[currentIndex - 1],
                    imageCount = content.images.size,
                    viewportHeightPx = viewportPixels,
                )
                when (decision) {
                    is ContinuousImageScrollDecision.ScrollTo -> {
                        // A new scroll supersedes the advance pending a pagination retry.
                        advanceAfterLoad = false
                        scrollInProgress = true
                        scope.launch {
                            try {
                                listState.animateScrollToItem(
                                    index = decision.position.imageIndex,
                                    scrollOffset = decision.position.offsetPx,
                                )
                            } finally {
                                scrollInProgress = false
                            }
                        }
                    }

                    is ContinuousImageScrollDecision.MeasurePreviousImage -> {
                        advanceAfterLoad = false
                        scrollInProgress = true
                        scope.launch {
                            try {
                                listState.scrollToItem(decision.imageIndex)
                                withFrameNanos { }
                                val imageHeight = imageHeights[decision.imageIndex]
                                    ?: listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == decision.imageIndex }
                                        ?.size
                                val offset = imageHeight?.let {
                                    ContinuousImageScrollPolicy.bottomOffsetPx(
                                        imageHeightPx = it,
                                        viewportHeightPx = viewportPixels,
                                    )
                                } ?: 0
                                listState.scrollToItem(decision.imageIndex, offset)
                            } finally {
                                scrollInProgress = false
                            }
                        }
                    }

                    ContinuousImageScrollDecision.StartBoundary -> {
                        onBoundary(ReaderBoundary.Start)
                    }

                    ContinuousImageScrollDecision.EndBoundary -> if (imagesExhausted) {
                        advanceAfterLoad = false
                        onBoundary(ReaderBoundary.End)
                    } else {
                        loadStartImageCount = content.images.size
                        advanceAfterLoad = true
                        onLoadMore()
                    }

                    ContinuousImageScrollDecision.Ignore -> Unit
                }
                true
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Keep the same focusable list across empty content, loading, and appended images.
        if (content.images.isEmpty() && !isLoadingMore) {
            item(key = EMPTY_IMAGE_ITEM_KEY) {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.reader_empty_images),
                        color = Color.LightGray,
                        fontSize = 22.sp,
                    )
                }
            }
        }
        itemsIndexed(
            items = content.images,
            key = { _, url -> url },
        ) { index, url ->
            // A chapter may reuse the same URL after the previous chapter exhausted its retries.
            key(contentRevision) {
                ReaderRemoteImage(
                    session = session,
                    url = url,
                    contentDescription = content.title,
                    zoomMode = settings.zoomMode,
                    horizontalBias = horizontalBias,
                    manualRetryRevision = manualRetryRevision,
                    onFinalFailureChanged = onFinalFailureChanged,
                    onImageSettled = {
                        if (index == initialImageIndex) initialImageSettled = true
                        if (index == content.images.lastIndex) lastImageSettled = true
                    },
                    viewportHeight = viewportHeight,
                    loadingTestTag = "reader-image-$index-loading",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reader-image-$index")
                        .onSizeChanged { size ->
                            if (size.height > 0 && imageHeights[index] != size.height) {
                                imageHeights[index] = size.height
                            }
                        },
                )
            }
        }
        if (isLoadingMore) {
            item(key = LOADING_MORE_ITEM_KEY) {
                KaloscopeLoadingLayout(
                    testTag = "reader-image-loading-more-scroll",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(INLINE_LOADING_HEIGHT),
                )
            }
        } else if (trailingAlignmentHeight > 0.dp) {
            item(key = TRAILING_ALIGNMENT_ITEM_KEY) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trailingAlignmentHeight),
                )
            }
        }
    }
}

@Composable
private fun PagedImages(
    session: Session,
    content: ReaderImageContent,
    settings: ImageReaderSettings,
    contentRevision: Long,
    initialImageIndex: Int,
    imagesExhausted: Boolean,
    isLoadingMore: Boolean,
    controlsVisible: Boolean,
    focusRequester: FocusRequester,
    onToggleControls: () -> Unit,
    onEnterControls: () -> Unit,
    onBoundary: (ReaderBoundary) -> Unit,
    onLoadMore: () -> Unit,
    onPositionChanged: (Int) -> Unit,
    onProgress: (ReaderPosition) -> Unit,
    manualRetryRevision: Int,
    onFinalFailureChanged: (String, Boolean) -> Unit,
    preloadController: ReaderImagePreloadController,
) {
    var index by remember(contentRevision) { mutableIntStateOf(initialImageIndex) }
    var advanceAfterLoad by remember(contentRevision) { mutableStateOf(false) }
    var pageTransitioning by remember(contentRevision) { mutableStateOf(false) }
    val previousSize = remember(contentRevision) { mutableIntStateOf(content.images.size) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(content.images.size, advanceAfterLoad) {
        if (advanceAfterLoad && content.images.size > previousSize.intValue) {
            index = if (previousSize.intValue == 0) {
                0
            } else {
                (index + 1).coerceAtMost(content.images.lastIndex)
            }
            advanceAfterLoad = false
            pageTransitioning = true
            scope.launch {
                delay(PAGE_TRANSITION_MILLIS)
                pageTransitioning = false
            }
        }
        previousSize.intValue = content.images.size
    }
    LaunchedEffect(contentRevision, index, content.images.size) {
        onPositionChanged(if (content.images.isEmpty()) 0 else index + 1)
        if (index in content.images.indices) onProgress(ReaderPosition(
            index,
            chapterProgress = (content.imageOffset + index + 1).toDouble() / content.imageCount.coerceAtLeast(1),
        ))
    }
    LaunchedEffect(contentRevision, content.images, index, preloadController) {
        preloadController.updateTarget(
            ReaderImagePreloadPolicy.pagedTarget(content.images, index),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .testTag("image-reader-paged")
            .onPreviewKeyEvent { event ->
                if (
                    event.consumeReaderControlKey(
                        controlsVisible = controlsVisible,
                        onToggleControls = onToggleControls,
                        onEnterControls = onEnterControls,
                    )
                ) {
                    return@onPreviewKeyEvent true
                }
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (pageTransitioning || isLoadingMore) return@onPreviewKeyEvent true
                val direction = event.key.toReaderDirection()
                    ?: return@onPreviewKeyEvent false
                when (ReaderRemoteKeyPolicy.pagedStep(direction, settings.pageDirection)) {
                    ReaderNavigationStep.Backward -> if (index > 0) {
                        // Returning to earlier pages supersedes the advance pending a retry.
                        advanceAfterLoad = false
                        index -= 1
                        pageTransitioning = true
                        scope.launch {
                            delay(PAGE_TRANSITION_MILLIS)
                            pageTransitioning = false
                        }
                    } else {
                        onBoundary(ReaderBoundary.Start)
                    }

                    ReaderNavigationStep.Forward -> if (index < content.images.lastIndex) {
                        index += 1
                        pageTransitioning = true
                        scope.launch {
                            delay(PAGE_TRANSITION_MILLIS)
                            pageTransitioning = false
                        }
                    } else if (!imagesExhausted) {
                        advanceAfterLoad = true
                        onLoadMore()
                    } else {
                        onBoundary(ReaderBoundary.End)
                    }

                    ReaderNavigationStep.None -> Unit
                }
                true
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isLoadingMore) {
            KaloscopeLoadingLayout(
                testTag = "reader-image-loading-more-paged",
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            )
        } else {
            val url = content.images.getOrNull(index)
            if (url == null) {
                Text(
                    text = stringResource(R.string.reader_empty_images),
                    color = Color.LightGray,
                    fontSize = 22.sp,
                )
            } else {
                // Reset image state with the chapter's failure registry, even when the URL repeats.
                key(contentRevision) {
                    ReaderRemoteImage(
                        session = session,
                        url = url,
                        contentDescription = content.title,
                        zoomMode = settings.zoomMode,
                        horizontalBias = 0f,
                        manualRetryRevision = manualRetryRevision,
                        onFinalFailureChanged = onFinalFailureChanged,
                        viewportHeight = Dp.Unspecified,
                        loadingTestTag = "reader-image-current-loading",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberReaderImagePreloadController(
    session: Session,
): ReaderImagePreloadController {
    val context = LocalContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    val controller = remember(
        context,
        imageLoader,
        session.server.origin,
        session.token,
    ) {
        ReaderImagePreloadController { rawUrl ->
            ReaderImageRequestFactory.create(context, session, rawUrl)?.let { request ->
                val disposable = imageLoader.enqueue(request)
                val cancel: () -> Unit = { disposable.dispose() }
                cancel
            }
        }
    }
    DisposableEffect(controller) {
        onDispose(controller::close)
    }
    return controller
}

@Composable
internal fun ReaderRemoteImage(
    session: Session,
    url: String,
    contentDescription: String,
    zoomMode: ImageZoomMode,
    horizontalBias: Float,
    manualRetryRevision: Int,
    onFinalFailureChanged: (String, Boolean) -> Unit,
    viewportHeight: Dp,
    loadingTestTag: String,
    modifier: Modifier,
    onImageSettled: () -> Unit = {},
) {
    val context = LocalContext.current
    var requestGeneration by remember(url) { mutableIntStateOf(0) }
    var automaticRetries by remember(url) { mutableIntStateOf(0) }
    val imageRequest = remember(
        context,
        session.server.origin,
        session.token,
        url,
        requestGeneration,
    ) {
        ReaderImageRequestFactory.create(context, session, url)
    }
    var failed by remember(
        url,
        session.server.origin,
        session.token,
        imageRequest?.data,
    ) {
        mutableStateOf(imageRequest == null)
    }
    var errorSignal by remember(url) { mutableIntStateOf(0) }
    var imageLoading by remember(
        url,
        session.server.origin,
        session.token,
        imageRequest?.data,
        requestGeneration,
    ) {
        mutableStateOf(imageRequest != null)
    }
    LaunchedEffect(url, session.server.origin, session.token, imageRequest?.data) {
        if (imageRequest == null) {
            automaticRetries = MAX_AUTOMATIC_RETRIES
            failed = true
            onFinalFailureChanged(url, false)
        }
    }
    LaunchedEffect(errorSignal) {
        if (errorSignal > 0 && automaticRetries < MAX_AUTOMATIC_RETRIES) {
            automaticRetries += 1
            delay(250L * automaticRetries)
            failed = false
            requestGeneration += 1
        } else if (errorSignal > 0) {
            onFinalFailureChanged(url, true)
        }
    }
    LaunchedEffect(manualRetryRevision) {
        if (
            manualRetryRevision > 0 &&
            failed &&
            !imageLoading &&
            automaticRetries >= MAX_AUTOMATIC_RETRIES &&
            imageRequest != null
        ) {
            automaticRetries = 0
            failed = false
            requestGeneration += 1
        }
    }
    LaunchedEffect(imageLoading) {
        if (!imageLoading) onImageSettled()
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (imageRequest == null) {
            Text(
                text = stringResource(R.string.reader_image_failed),
                color = Color.LightGray,
            )
        } else {
            val imageModifier = when {
                viewportHeight == Dp.Unspecified -> Modifier.fillMaxSize()
                zoomMode == ImageZoomMode.Auto -> Modifier
                    .fillMaxWidth()
                    .height(viewportHeight)
                zoomMode == ImageZoomMode.FitWidth -> Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                else -> Modifier
                    .height(viewportHeight)
                    .fillMaxWidth()
            }
            // Rebuilding an equivalent request does not restart Coil's remembered painter.
            key(requestGeneration) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = contentDescription,
                    modifier = imageModifier.heightIn(min = 1.dp),
                    contentScale = when (zoomMode) {
                        ImageZoomMode.Auto -> ContentScale.Fit
                        ImageZoomMode.FitWidth -> ContentScale.FillWidth
                        ImageZoomMode.FitHeight -> ContentScale.FillHeight
                    },
                    alignment = if (zoomMode == ImageZoomMode.FitHeight) {
                        BiasAlignment(horizontalBias = horizontalBias, verticalBias = 0f)
                    } else {
                        Alignment.Center
                    },
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Empty,
                            is AsyncImagePainter.State.Loading,
                            -> imageLoading = true

                            is AsyncImagePainter.State.Success -> {
                                imageLoading = false
                                failed = false
                                onFinalFailureChanged(url, false)
                            }
                            is AsyncImagePainter.State.Error -> {
                                imageLoading = automaticRetries < MAX_AUTOMATIC_RETRIES
                                failed = true
                                errorSignal += 1
                            }
                        }
                    },
                )
            }
        }
        if (imageRequest != null && imageLoading) {
            KaloscopeLoadingLayout(
                testTag = loadingTestTag,
                modifier = when {
                    viewportHeight == Dp.Unspecified -> Modifier.fillMaxSize()
                    zoomMode == ImageZoomMode.FitWidth -> Modifier
                        .fillMaxWidth()
                        .height(INLINE_LOADING_HEIGHT)
                    else -> Modifier
                        .fillMaxWidth()
                        .height(viewportHeight)
                },
            )
        }
        if (
            imageRequest != null &&
            failed &&
            !imageLoading &&
            automaticRetries >= MAX_AUTOMATIC_RETRIES
        ) {
            Text(
                text = stringResource(R.string.reader_image_failed),
                color = Color.LightGray,
                modifier = Modifier.testTag("reader-image-failed"),
            )
        }
    }
}

private fun Key.toReaderDirection(): ReaderDirection? =
    when (this) {
        Key.DirectionUp -> ReaderDirection.Up
        Key.DirectionDown -> ReaderDirection.Down
        Key.DirectionLeft -> ReaderDirection.Left
        Key.DirectionRight -> ReaderDirection.Right
        else -> null
    }

private const val MAX_AUTOMATIC_RETRIES = 3
private const val IMAGE_PAN_STEP = 0.5f
private const val PAGE_TRANSITION_MILLIS = 200L
private const val EMPTY_IMAGE_ITEM_KEY = "reader-empty-images"
private const val LOADING_MORE_ITEM_KEY = "reader-loading-more"
private const val TRAILING_ALIGNMENT_ITEM_KEY = "reader-trailing-alignment"
private val INLINE_LOADING_HEIGHT = 120.dp
