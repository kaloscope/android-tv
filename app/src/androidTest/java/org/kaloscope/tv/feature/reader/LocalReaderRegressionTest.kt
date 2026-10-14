package org.kaloscope.tv.feature.reader

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import java.io.ByteArrayOutputStream
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.*
import org.kaloscope.tv.data.reader.readingRecord

class LocalReaderRegressionTest {
    @get:Rule val composeRule = createComposeRule()
    private val session = Session(SavedServer("test", "Test", "https://library.example"), "fixture-token", SessionUser(1, "reader", "user"))
    private val source = ReaderSource.Local(101, 202, "item:202", "a".repeat(64), "cbz")

    @Test fun pagedChapterChangeReportsFirstPageEvenWhenPageCountIsUnchanged() = withImages { readerSession ->
        var state by mutableStateOf(imageState(listOf(asset(1), asset(2)), ImageReadMode.Paged))
        val positions = mutableListOf<Pair<ReaderContent, ReaderPosition>>()
        setReader({ state }, readerSession, onProgress = { content, position -> positions += content to position })
        composeRule.waitUntil(5_000) { positions.isNotEmpty() }
        composeRule.runOnIdle {
            state = state.copy(
                content = state.content.copy(source = source.copy(sourceItemId = 203, chapterId = "item:203")),
                contentRevision = 1,
            )
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue("The new chapter must be saved before another page turn", positions.any {
                it.first.source == state.content.source && it.second.index == 0
            })
        }
    }

    @Test fun backwardPaginationEntersPreviousLongImageAtItsBottom() = withImages { readerSession ->
        var state by mutableStateOf(imageState(listOf(asset(21), asset(22))).let {
            it.copy(content = it.content.copy(imageOffset = 20, imageCount = 22))
        })
        val positions = mutableListOf<ReaderPosition>()
        setReader({ state }, readerSession, onProgress = { _, position -> positions += position }, onPrevious = {
            state = state.copy(
                content = state.content.copy(images = listOf(asset(20)) + state.content.images, imageOffset = 19),
                contentRevision = 1,
                loadingPrevious = true,
            )
        })
        composeRule.waitUntil(5_000) { positions.isNotEmpty() }
        composeRule.runOnIdle { positions.clear() }
        composeRule.onNodeWithTag("image-reader-scroll").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.waitUntil(5_000) { positions.isNotEmpty() }
        composeRule.runOnIdle {
            assertEquals(0, positions.last().index)
            assertTrue("Previous long image should align at its bottom: $positions", positions.last().offset > 0.5)
        }
    }

    @Test fun lastPagedImageRecordsCompletionWithoutChangingResumeOffset() = withImages { readerSession ->
        val state = imageState(listOf(asset(1), asset(2)), ImageReadMode.Paged).let {
            it.copy(content = it.content.copy(initialPosition = ReaderPosition(1)))
        }
        var position: ReaderPosition? = null
        setReader({ state }, readerSession, onProgress = { _, value -> position = value })
        composeRule.waitUntil(5_000) { position != null }
        composeRule.runOnIdle {
            val record = checkNotNull(state.content.readingRecord(checkNotNull(position)))
            assertEquals(100, record.percentage)
            assertEquals(0.0, record.locator.offset, 0.0)
        }
    }

    @Test fun fullyVisibleTextChapterRecordsCompletion() {
        val content = ReaderTextContent(
            source.copy(sourceItemId = 101, chapterId = "b".repeat(32), format = "txt"), "Short book", "One paragraph",
            chapters = listOf(ReaderChapter("b".repeat(32), "Only chapter")), selectedChapterIndex = 0,
        )
        val state = ReaderUiState.Text("request", session.server.id, content, TextReaderSettings(), ReaderChapterOrder.Ascending)
        var position: ReaderPosition? = null
        setReader({ state }, onProgress = { _, value -> position = value })
        composeRule.waitUntil(5_000) { position != null }
        composeRule.runOnIdle {
            assertEquals(100, content.readingRecord(checkNotNull(position))?.percentage)
        }
    }

    private fun imageState(images: List<String>, mode: ImageReadMode = ImageReadMode.Scroll) = ReaderUiState.Image(
        "request", session.server.id, ReaderImageContent(source, "Comic", images, images.size),
        ImageReaderSettings(readMode = mode, zoomMode = ImageZoomMode.FitWidth), ReaderChapterOrder.Ascending,
    )

    private fun setReader(
        state: () -> ReaderUiState,
        readerSession: Session = session,
        onProgress: (ReaderContent, ReaderPosition) -> Unit = { _, _ -> },
        onPrevious: () -> Unit = {},
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    readerSession, state(), onBack = {}, onSelectChapter = {}, onLoadMoreImages = {},
                    onImageSettings = {}, onTextSettings = {}, onChapterOrder = {},
                    onDismissChapterError = {}, onDismissPageError = {},
                    onProgress = onProgress, onLoadPreviousImages = onPrevious,
                )
            }
        }
    }

    private fun asset(index: Int) = "/_api/media/202/assets/${index.toString(16).padStart(32, '0')}?version=${source.version}"

    private fun withImages(block: (Session) -> Unit) {
        MockWebServer().use { server ->
            val bitmap = Bitmap.createBitmap(100, 300, Bitmap.Config.ARGB_8888)
            val bytes = ByteArrayOutputStream().use { stream ->
                bitmap.eraseColor(android.graphics.Color.BLUE)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
            bitmap.recycle()
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = MockResponse()
                    .setHeader("Content-Type", "image/png").setBody(Buffer().write(bytes))
            }
            server.start()
            block(session.copy(server = session.server.copy(origin = server.url("/").toString().trimEnd('/'))))
        }
    }
}
