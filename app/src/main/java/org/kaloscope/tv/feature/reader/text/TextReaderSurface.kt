package org.kaloscope.tv.feature.reader.text

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.toDpDimensions
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.reader.ReaderBoundary
import org.kaloscope.tv.feature.reader.consumeReaderControlKey

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
) {
    val palette = TextReaderPalettes.forTheme(settings.theme)
    val dimensions = settings.toDpDimensions(LocalDensity.current)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val viewportPixels = with(LocalDensity.current) { maxHeight.toPx() }
        LaunchedEffect(contentRevision) {
            scrollState.scrollTo(0)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .testTag("text-reader-content")
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
                    when (event.key) {
                        Key.DirectionUp -> if (scrollState.value == 0) {
                            onBoundary(ReaderBoundary.Start)
                        } else {
                            scope.launch { scrollState.animateScrollBy(-viewportPixels * 0.85f) }
                        }

                        Key.DirectionDown -> if (scrollState.value >= scrollState.maxValue) {
                            onBoundary(ReaderBoundary.End)
                        } else {
                            scope.launch { scrollState.animateScrollBy(viewportPixels * 0.85f) }
                        }

                        Key.DirectionLeft, Key.DirectionRight -> Unit

                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
        ) {
            val paragraphs = content.text
                .split(PARAGRAPH_BREAK)
                .map(String::trim)
                .filter(String::isNotEmpty)
            if (paragraphs.isEmpty()) {
                Text(
                    text = stringResource(R.string.reader_empty_text),
                    color = palette.muted,
                    fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 1120.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(
                            start = dimensions.horizontalPadding,
                            end = dimensions.horizontalPadding,
                            top = 12.dp,
                            bottom = 150.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensions.paragraphSpacing,
                    ),
                ) {
                    paragraphs.forEachIndexed { index, paragraph ->
                        Text(
                            text = paragraph,
                            color = palette.text,
                            fontFamily = settings.font.toFontFamily(),
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * settings.lineHeight).sp,
                            modifier = Modifier.testTag("text-reader-paragraph-$index"),
                        )
                    }
                }
            }
        }
    }
}

private val PARAGRAPH_BREAK = Regex("\\n\\s*\\n")
