package org.kaloscope.tv.feature.reader.text

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.toDpDimensions
import org.kaloscope.tv.core.model.ImageZoomMode
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.model.ReaderTextBlock
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.ReaderTextRun
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.reader.ReaderBoundary
import org.kaloscope.tv.feature.reader.consumeReaderControlKey
import org.kaloscope.tv.feature.reader.image.ReaderRemoteImage

@Composable
internal fun TextReaderSurface(
    content: ReaderTextContent,
    settings: TextReaderSettings,
    contentRevision: Long,
    controlsVisible: Boolean,
    focusRequester: FocusRequester,
    onToggleControls: () -> Unit,
    onEnterControls: () -> Unit,
    onBoundary: (ReaderBoundary) -> Unit,
    session: Session,
    onPositionChanged: (ReaderPosition) -> Unit = {},
    manualRetryRevision: Int = 0,
    onFailedImagesChanged: (Boolean) -> Unit = {},
) {
    val palette = TextReaderPalettes.forTheme(settings.theme)
    val dimensions = settings.toDpDimensions(LocalDensity.current)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val blocks = remember(content.paragraphs, content.blocks) {
        content.blocks.ifEmpty {
            content.paragraphs.mapIndexed { index, paragraph ->
                ReaderTextBlock.Text("paragraph-$index", listOf(ReaderTextRun(paragraph)))
            }
        }
    }
    val failedImages = remember(contentRevision) { mutableStateMapOf<String, Boolean>() }
    var positionReady by remember(contentRevision) { mutableStateOf(false) }
    LaunchedEffect(failedImages.size) { onFailedImagesChanged(failedImages.isNotEmpty()) }
    LaunchedEffect(contentRevision) {
        positionReady = false
        val position = content.initialPosition
        val index = position.index.coerceIn(0, blocks.lastIndex.coerceAtLeast(0))
        listState.scrollToItem(index)
        withFrameNanos { }
        val size = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size ?: 0
        if (size > 0 && position.offset > 0) {
            listState.scrollToItem(index, (size * position.offset).toInt())
        }
        positionReady = true
    }
    LaunchedEffect(contentRevision, positionReady) {
        if (!positionReady) return@LaunchedEffect
        snapshotFlow {
            val index = listState.firstVisibleItemIndex
            val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            item?.takeIf { it.size > 0 }?.let {
                val offset = (listState.firstVisibleItemScrollOffset.toDouble() / it.size).coerceIn(0.0, 1.0)
                ReaderPosition(index, offset,
                    chapterProgress = if (!listState.canScrollForward) 1.0 else (index + offset) / blocks.size.coerceAtLeast(1),
                )
            }
        }.distinctUntilChanged().collect { it?.let(onPositionChanged) }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val imageHeight = maxHeight * 0.7f
        val viewportPixels = with(LocalDensity.current) { maxHeight.toPx() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .testTag("text-reader-content")
                .onPreviewKeyEvent { event ->
                    if (event.consumeReaderControlKey(controlsVisible, onToggleControls, onEnterControls)) {
                        return@onPreviewKeyEvent true
                    }
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> if (blocks.isEmpty() || !listState.canScrollBackward) {
                            onBoundary(ReaderBoundary.Start)
                        } else scope.launch { listState.animateScrollBy(-viewportPixels * 0.85f) }
                        Key.DirectionDown -> if (blocks.isEmpty() || !listState.canScrollForward) {
                            onBoundary(ReaderBoundary.End)
                        } else scope.launch { listState.animateScrollBy(viewportPixels * 0.85f) }
                        Key.DirectionLeft, Key.DirectionRight -> Unit
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
        ) {
            if (blocks.isEmpty()) {
                Text(
                    stringResource(R.string.reader_empty_text), color = palette.muted,
                    fontSize = 22.sp, modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 1120.dp).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = dimensions.horizontalPadding, end = dimensions.horizontalPadding,
                        top = 12.dp, bottom = 150.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(dimensions.paragraphSpacing),
                ) {
                    itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                        when (block) {
                            is ReaderTextBlock.Text -> {
                                val fontSize = settings.fontSizeSp * when (block.headingLevel) {
                                    1 -> 1.5f
                                    2 -> 1.3f
                                    null -> 1f
                                    else -> 1.15f
                                }
                                Text(
                                    text = block.runs.annotated(),
                                    color = palette.text,
                                    fontFamily = settings.font.toFontFamily(),
                                    fontSize = fontSize.sp,
                                    fontWeight = if (block.headingLevel != null) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (block.quote) FontStyle.Italic else FontStyle.Normal,
                                    lineHeight = (fontSize * settings.lineHeight).sp,
                                    modifier = Modifier.testTag("text-reader-paragraph-$index")
                                        .padding(start = if (block.quote) 20.dp else 0.dp),
                                )
                            }
                            is ReaderTextBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                block.items.forEachIndexed { itemIndex, runs ->
                                    Text(
                                        text = buildAnnotatedString {
                                            append(if (block.ordered) "${block.start + itemIndex}. " else "• ")
                                            append(runs.annotated())
                                        },
                                        color = palette.text, fontFamily = settings.font.toFontFamily(),
                                        fontSize = settings.fontSizeSp.sp,
                                        lineHeight = (settings.fontSizeSp * settings.lineHeight).sp,
                                    )
                                }
                            }
                            is ReaderTextBlock.Image -> if (block.url == null) {
                                Text(
                                    block.alt.ifBlank { stringResource(R.string.reader_image_unavailable) },
                                    color = palette.muted, fontSize = settings.fontSizeSp.sp,
                                )
                            } else {
                                ReaderRemoteImage(
                                    session = session, url = block.url, contentDescription = block.alt,
                                    zoomMode = ImageZoomMode.Auto, horizontalBias = 0f,
                                    manualRetryRevision = manualRetryRevision,
                                    onFinalFailureChanged = { url, failed ->
                                        if (failed) failedImages[url] = true else failedImages.remove(url)
                                    },
                                    viewportHeight = imageHeight,
                                    loadingTestTag = "reader-epub-image-$index-loading",
                                    modifier = Modifier.fillMaxWidth().testTag("reader-epub-image-$index"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<ReaderTextRun>.annotated() = buildAnnotatedString {
    forEach { run ->
        withStyle(SpanStyle(
            fontWeight = if (run.strong) FontWeight.Bold else null,
            fontStyle = if (run.emphasis) FontStyle.Italic else null,
        )) { append(run.text) }
    }
}
