package org.kaloscope.tv.feature.reader.image

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.kaloscope.tv.R
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ImageZoomMode
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy
import org.kaloscope.tv.core.network.ServerImageResolver
import org.kaloscope.tv.core.reader.ReaderBoundary
import org.kaloscope.tv.core.reader.ReaderDirection
import org.kaloscope.tv.core.reader.ReaderNavigationStep
import org.kaloscope.tv.core.reader.ReaderRemoteKeyPolicy

@Composable
internal fun ImageReaderSurface(
    session: Session,
    content: ReaderImageContent,
    settings: ImageReaderSettings,
    contentRevision: Long,
    imagesExhausted: Boolean,
    controlsVisible: Boolean,
    focusRequester: FocusRequester,
    onToggleControls: () -> Unit,
    onEnterControls: () -> Unit,
    onBoundary: (ReaderBoundary) -> Unit,
    onLoadMore: () -> Unit,
    onNavigate: () -> Unit,
    onPositionChanged: (Int) -> Unit,
    manualRetryRevision: Int,
    onFailedImagesChanged: (Boolean) -> Unit,
) {
    val failedImages = remember(contentRevision) { mutableStateMapOf<String, Boolean>() }
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
                imagesExhausted = imagesExhausted,
                controlsVisible = controlsVisible,
                viewportHeight = maxHeight,
                focusRequester = focusRequester,
                onToggleControls = onToggleControls,
                onEnterControls = onEnterControls,
                onBoundary = onBoundary,
                onLoadMore = onLoadMore,
                onNavigate = onNavigate,
                onPositionChanged = onPositionChanged,
                manualRetryRevision = manualRetryRevision,
                onFinalFailureChanged = ::updateImageFailure,
            )

            ImageReadMode.Paged -> PagedImages(
                session = session,
                content = content,
                settings = settings,
                contentRevision = contentRevision,
                imagesExhausted = imagesExhausted,
                controlsVisible = controlsVisible,
                focusRequester = focusRequester,
                onToggleControls = onToggleControls,
                onEnterControls = onEnterControls,
                onBoundary = onBoundary,
                onLoadMore = onLoadMore,
                onNavigate = onNavigate,
                onPositionChanged = onPositionChanged,
                manualRetryRevision = manualRetryRevision,
                onFinalFailureChanged = ::updateImageFailure,
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
    imagesExhausted: Boolean,
    controlsVisible: Boolean,
    viewportHeight: Dp,
    focusRequester: FocusRequester,
    onToggleControls: () -> Unit,
    onEnterControls: () -> Unit,
    onBoundary: (ReaderBoundary) -> Unit,
    onLoadMore: () -> Unit,
    onNavigate: () -> Unit,
    onPositionChanged: (Int) -> Unit,
    manualRetryRevision: Int,
    onFinalFailureChanged: (String, Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val viewportPixels = with(LocalDensity.current) { viewportHeight.toPx() }
    var horizontalBias by remember(contentRevision) { mutableFloatStateOf(0f) }
    LaunchedEffect(contentRevision) {
        listState.scrollToItem(0)
        horizontalBias = 0f
    }
    LaunchedEffect(settings.zoomMode) { horizontalBias = 0f }
    if (content.images.isEmpty()) {
        LaunchedEffect(contentRevision) { onPositionChanged(0) }
        EmptyImageContent(
            imagesExhausted = imagesExhausted,
            controlsVisible = controlsVisible,
            focusRequester = focusRequester,
            onToggleControls = onToggleControls,
            onEnterControls = onEnterControls,
            onBoundary = onBoundary,
            onLoadMore = onLoadMore,
        )
        return
    }
    LaunchedEffect(contentRevision, listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { onPositionChanged(it + 1) }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .testTag("image-reader-scroll")
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                    onToggleControls()
                    return@onPreviewKeyEvent true
                }
                if (controlsVisible && event.key == Key.DirectionDown) {
                    onEnterControls()
                    return@onPreviewKeyEvent true
                }
                if (settings.zoomMode == ImageZoomMode.FitHeight) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            horizontalBias = (horizontalBias - IMAGE_PAN_STEP).coerceAtLeast(-1f)
                            onNavigate()
                            return@onPreviewKeyEvent true
                        }

                        Key.DirectionRight -> {
                            horizontalBias = (horizontalBias + IMAGE_PAN_STEP).coerceAtMost(1f)
                            onNavigate()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                val direction = event.key.toReaderDirection()
                    ?: return@onPreviewKeyEvent false
                val step = ReaderRemoteKeyPolicy.verticalStep(direction)
                when (step) {
                    ReaderNavigationStep.Backward -> if (!listState.canScrollBackward) {
                        onBoundary(ReaderBoundary.Start)
                    } else {
                        onNavigate()
                        scope.launch { listState.animateScrollBy(-viewportPixels * 0.85f) }
                    }

                    ReaderNavigationStep.Forward -> if (!listState.canScrollForward) {
                        if (imagesExhausted) onBoundary(ReaderBoundary.End) else onLoadMore()
                    } else {
                        onNavigate()
                        scope.launch { listState.animateScrollBy(viewportPixels * 0.85f) }
                    }

                    ReaderNavigationStep.None -> Unit
                }
                true
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(
            items = content.images,
            key = { index, url -> "$index:$url" },
        ) { index, url ->
            ReaderRemoteImage(
                session = session,
                url = url,
                contentDescription = content.title,
                zoomMode = settings.zoomMode,
                horizontalBias = horizontalBias,
                manualRetryRevision = manualRetryRevision,
                onFinalFailureChanged = onFinalFailureChanged,
                viewportHeight = viewportHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reader-image-$index"),
            )
        }
    }
}

@Composable
private fun PagedImages(
    session: Session,
    content: ReaderImageContent,
    settings: ImageReaderSettings,
    contentRevision: Long,
    imagesExhausted: Boolean,
    controlsVisible: Boolean,
    focusRequester: FocusRequester,
    onToggleControls: () -> Unit,
    onEnterControls: () -> Unit,
    onBoundary: (ReaderBoundary) -> Unit,
    onLoadMore: () -> Unit,
    onNavigate: () -> Unit,
    onPositionChanged: (Int) -> Unit,
    manualRetryRevision: Int,
    onFinalFailureChanged: (String, Boolean) -> Unit,
) {
    var index by remember(contentRevision) { mutableIntStateOf(0) }
    var advanceAfterLoad by remember(contentRevision) { mutableStateOf(false) }
    var pageTransitioning by remember(contentRevision) { mutableStateOf(false) }
    val previousSize = remember(contentRevision) { mutableIntStateOf(content.images.size) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(content.images.size, advanceAfterLoad) {
        if (advanceAfterLoad && content.images.size > previousSize.intValue) {
            index = (index + 1).coerceAtMost(content.images.lastIndex)
            advanceAfterLoad = false
            pageTransitioning = true
            onNavigate()
            scope.launch {
                delay(PAGE_TRANSITION_MILLIS)
                pageTransitioning = false
            }
        }
        previousSize.intValue = content.images.size
    }
    LaunchedEffect(index, content.images.size) {
        onPositionChanged(if (content.images.isEmpty()) 0 else index + 1)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .testTag("image-reader-paged")
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                    onToggleControls()
                    return@onPreviewKeyEvent true
                }
                if (controlsVisible && event.key == Key.DirectionDown) {
                    onEnterControls()
                    return@onPreviewKeyEvent true
                }
                if (pageTransitioning) return@onPreviewKeyEvent true
                val direction = event.key.toReaderDirection()
                    ?: return@onPreviewKeyEvent false
                when (ReaderRemoteKeyPolicy.pagedStep(direction, settings.pageDirection)) {
                    ReaderNavigationStep.Backward -> if (index > 0) {
                        index -= 1
                        pageTransitioning = true
                        onNavigate()
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
                        onNavigate()
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
        val url = content.images.getOrNull(index)
        if (url == null) {
            Text(
                text = stringResource(R.string.reader_empty_images),
                color = Color.LightGray,
                fontSize = 22.sp,
            )
        } else {
            ReaderRemoteImage(
                session = session,
                url = url,
                contentDescription = content.title,
                zoomMode = settings.zoomMode,
                horizontalBias = 0f,
                manualRetryRevision = manualRetryRevision,
                onFinalFailureChanged = onFinalFailureChanged,
                viewportHeight = Dp.Unspecified,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EmptyImageContent(
    imagesExhausted: Boolean,
    controlsVisible: Boolean,
    focusRequester: FocusRequester,
    onToggleControls: () -> Unit,
    onEnterControls: () -> Unit,
    onBoundary: (ReaderBoundary) -> Unit,
    onLoadMore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .testTag("image-reader-scroll")
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter -> onToggleControls()
                    Key.DirectionUp -> onBoundary(ReaderBoundary.Start)
                    Key.DirectionDown -> when {
                        controlsVisible -> onEnterControls()
                        imagesExhausted -> onBoundary(ReaderBoundary.End)
                        else -> onLoadMore()
                    }

                    else -> return@onPreviewKeyEvent false
                }
                true
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.reader_empty_images),
            color = Color.LightGray,
            fontSize = 22.sp,
        )
    }
}

@Composable
private fun ReaderRemoteImage(
    session: Session,
    url: String,
    contentDescription: String,
    zoomMode: ImageZoomMode,
    horizontalBias: Float,
    manualRetryRevision: Int,
    onFinalFailureChanged: (String, Boolean) -> Unit,
    viewportHeight: Dp,
    modifier: Modifier,
) {
    val resolved = remember(session.server.origin, session.token, url) {
        ServerImageResolver.resolve(session, url, ServerImagePolicy.Auto)
    }
    var requestGeneration by remember(url) { mutableIntStateOf(0) }
    var automaticRetries by remember(url) { mutableIntStateOf(0) }
    var failed by remember(url, resolved) { mutableStateOf(resolved == null) }
    var errorSignal by remember(url) { mutableIntStateOf(0) }
    LaunchedEffect(resolved) {
        if (resolved == null) {
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
            automaticRetries >= MAX_AUTOMATIC_RETRIES &&
            resolved != null
        ) {
            automaticRetries = 0
            failed = false
            requestGeneration += 1
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (resolved == null) {
            Text(
                text = stringResource(R.string.reader_image_failed),
                color = Color.LightGray,
            )
        } else {
            val context = LocalContext.current
            val headers = remember(resolved.authorization) {
                resolved.authorization?.let { authorization ->
                    NetworkHeaders.Builder().set("Authorization", authorization).build()
                }
            }
            val imageRequest = remember(resolved.url, headers, requestGeneration) {
                ImageRequest.Builder(context)
                    .data(resolved.url)
                    .apply { if (headers != null) httpHeaders(headers) }
                    .build()
            }
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
                        is AsyncImagePainter.State.Success -> {
                            failed = false
                            onFinalFailureChanged(url, false)
                        }
                        is AsyncImagePainter.State.Error -> {
                            failed = true
                            errorSignal += 1
                        }
                        else -> Unit
                    }
                },
            )
        }
        if (resolved != null && failed && automaticRetries >= MAX_AUTOMATIC_RETRIES) {
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
