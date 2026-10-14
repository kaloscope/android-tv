package org.kaloscope.tv.feature.reader

import android.graphics.Bitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.*
import org.kaloscope.tv.feature.detail.MediaDetailScreen
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.library.LibraryItemsState
import org.kaloscope.tv.feature.library.LibraryScreen
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.test.captureDeviceScreen
import java.io.File

class LocalReadingScreenTest {
    @get:Rule val composeRule = createComposeRule()
    private val session = Session(SavedServer("local-test", "Test", "https://library.example"), "fixture-token", SessionUser(1, "reader", "user"))

    @Test fun libraryUsesUpstreamNovelAndComicIcons() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session,
                    state = LibraryUiState.Content(
                        listOf(MediaLibrary(1, "小说", MediaLibraryType.Novel), MediaLibrary(2, "漫画", MediaLibraryType.Comic)),
                        1, items = LibraryItemsState.Empty,
                    ),
                    restoreMediaId = null, requestInitialFocus = true,
                    onSelectLibrary = {}, onQueryChange = {}, onSearch = {}, onRetry = {},
                    onLoadMore = {}, onMediaFocused = {}, onOpenMedia = {},
                )
            }
        }
        composeRule.onNodeWithTag("library-type-icon-novel", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("library-type-icon-comic", useUnmergedTree = true).assertExists()
        val icon = composeRule.onNodeWithTag("library-type-icon-comic", useUnmergedTree = true)
            .captureToImage().asAndroidBitmap()
        val background = icon.getPixel(0, icon.height - 1)
        for (y in (icon.height * 0.91).toInt() until icon.height) {
            for (x in 0 until icon.width) {
                assertEquals("Comic icon must retain the source SVG bounds", background, icon.getPixel(x, y))
            }
        }
        capture("local-reading-library")
    }

    @Test fun novelPrimaryActionOpensReaderWithInitialFocus() {
        var requested: Long? = null
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session, MediaDetailUiState.Content(detail(MediaLibraryType.Novel)), emptyMap(),
                    onBack = {}, onRetry = {}, onChildFocused = {}, onChildViewportChanged = {},
                    onPlayParent = { _, _ -> error("Novel entered player") },
                    onPlayChild = { _, _ -> error("Novel entered player") },
                    onRead = { parent, child -> assertEquals(null, child); requested = parent.id },
                )
            }
        }
        composeRule.onNodeWithTag("detail-primary-action").assertIsFocused()
        composeRule.onNodeWithText("阅读").assertExists()
        capture("local-reading-detail")
        composeRule.onNodeWithTag("detail-primary-action").performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(101L, requested) }
    }

    @Test fun comicChapterOpensReaderUsingItsParentAndChildIds() {
        val child = MediaSummary(202, "第一章", "/fixtures/chapter.cbz", null, null, null, null, null, null)
        var requested: Pair<Long, Long?>? = null
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session, MediaDetailUiState.Content(detail(MediaLibraryType.Comic).copy(children = listOf(child))), emptyMap(),
                    onBack = {}, onRetry = {}, onChildFocused = {}, onChildViewportChanged = {},
                    onPlayParent = { _, _ -> error("Comic entered player") },
                    onPlayChild = { _, _ -> error("Comic entered player") },
                    onRead = { parent, selected -> requested = parent.id to selected },
                )
            }
        }
        composeRule.onNodeWithTag("media-child-card-202").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(101L to 202L, requested) }
    }

    @Test fun epubKeepsRichContentAndUsesTheExistingReaderControls() {
        val content = ReaderTextContent(
            ReaderSource.Local(101, 101, "a".repeat(32), "b".repeat(64), "epub"),
            "本地 EPUB", "", chapters = listOf(ReaderChapter("a".repeat(32), "第一章")), selectedChapterIndex = 0,
            blocks = listOf(
                ReaderTextBlock.Text("heading", listOf(ReaderTextRun("章节标题")), headingLevel = 1),
                ReaderTextBlock.Text("paragraph", listOf(ReaderTextRun("粗体", strong = true), ReaderTextRun("与斜体", emphasis = true))),
                ReaderTextBlock.ListBlock("list", listOf(listOf(ReaderTextRun("列表内容"))), true, 3),
                ReaderTextBlock.Image("missing", null, "缺失插图说明"),
            ),
        )
        setReader(ReaderUiState.Text("request", session.server.id, content, TextReaderSettings(), ReaderChapterOrder.Ascending))
        composeRule.onNodeWithTag("text-reader-content").assertIsFocused()
        composeRule.onNodeWithText("章节标题").assertExists()
        composeRule.onNodeWithText("粗体与斜体").assertExists()
        composeRule.onNodeWithText("3. 列表内容").assertExists()
        capture("local-reading-epub")
        composeRule.onNodeWithTag("text-reader-content").performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("阅读设置").assertIsFocused()
    }

    @Test fun textRestoresParagraphAndReportsScrollingProgress() {
        val content = ReaderTextContent(
            ReaderSource.Local(101, 101, "a".repeat(32), "b".repeat(64), "txt"), "本地小说", "",
            localParagraphs = (0..30).map { "第 $it 段测试文字。".repeat(6) },
            initialPosition = ReaderPosition(12, 0.2),
        )
        var position = ReaderPosition()
        setReader(
            ReaderUiState.Text("request", session.server.id, content, TextReaderSettings(), ReaderChapterOrder.Ascending),
            onProgress = { _, value -> position = value },
        )
        composeRule.onNodeWithTag("text-reader-paragraph-12").assertIsDisplayed()
        composeRule.waitUntil(5_000) { position.index == 12 }
        composeRule.onNodeWithTag("text-reader-content").performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitUntil(5_000) { position.index > 12 }
        assertTrue(position.offset in 0.0..1.0)
    }

    @Test fun resumedComicCanLoadPagesBeforeItsSavedPosition() {
        val content = ReaderImageContent(
            ReaderSource.Local(101, 202, "item:202", "a".repeat(64), "cbz"), "本地漫画", emptyList(), 45,
            imageOffset = 20,
        )
        var requests = 0
        setReader(
            ReaderUiState.Image("request", session.server.id, content, ImageReaderSettings(readMode = ImageReadMode.Paged), ReaderChapterOrder.Ascending),
            onPrevious = { requests++ },
        )
        composeRule.onNodeWithTag("image-reader-paged").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.runOnIdle { assertEquals(1, requests) }
    }

    @Test fun comicRestoresOffsetAfterTheRealImageSizeIsKnown() {
        MockWebServer().use { server ->
            val bitmap = Bitmap.createBitmap(100, 300, Bitmap.Config.ARGB_8888)
            val bytes = ByteArrayOutputStream().use { stream ->
                bitmap.eraseColor(android.graphics.Color.BLUE)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
            bitmap.recycle()
            server.enqueue(MockResponse().setHeader("Content-Type", "image/png")
                .setBody(Buffer().write(bytes)).setBodyDelay(500, TimeUnit.MILLISECONDS))
            server.start()
            val content = ReaderImageContent(
                ReaderSource.Local(101, 101, "item:101", "a".repeat(64), "dir"), "长图漫画",
                listOf("/resume.png"), 1, initialPosition = ReaderPosition(0, 0.4),
            )
            val positions = mutableListOf<ReaderPosition>()
            setReader(
                ReaderUiState.Image("request", session.server.id, content,
                    ImageReaderSettings(zoomMode = ImageZoomMode.FitWidth), ReaderChapterOrder.Ascending),
                onProgress = { _, value -> positions += value },
                readerSession = session.copy(server = session.server.copy(origin = server.url("/").toString().trimEnd('/'))),
            )
            composeRule.waitUntil(10_000) { positions.isNotEmpty() }
            composeRule.runOnIdle {
                assertEquals(0, positions.first().index)
                assertEquals("Restored positions: $positions", 0.4, positions.first().offset, 0.005)
            }
            val request = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertEquals("/resume.png", request.path)
            assertEquals("Token fixture-token", request.getHeader("Authorization"))
        }
    }

    @Test fun localLoadFailureKeepsRetryAndBackAvailable() {
        var retries = 0
        setReader(ReaderUiState.Error("request", AppError.Offline, retryable = true), onRetry = { retries++ })
        composeRule.onNodeWithText("重试").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(1, retries) }
        composeRule.onNodeWithText("返回").assertExists()
    }

    private fun setReader(
        state: ReaderUiState,
        onProgress: (ReaderContent, ReaderPosition) -> Unit = { _, _ -> },
        onPrevious: () -> Unit = {},
        onRetry: () -> Unit = {},
        readerSession: Session = session,
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    readerSession, state, onBack = {}, onSelectChapter = {}, onLoadMoreImages = {},
                    onImageSettings = {}, onTextSettings = {}, onChapterOrder = {},
                    onDismissChapterError = {}, onDismissPageError = {},
                    onProgress = onProgress, onLoadPreviousImages = onPrevious, onRetry = onRetry,
                )
            }
        }
    }

    private fun detail(type: MediaLibraryType) = MediaDetail(
        101, MediaLibrary(1, "测试媒体库", type), "本地阅读测试", "/fixtures/book", null, null, 2026,
        null, null, null, null, "用于验证本地阅读入口和遥控器操作。", listOf("测试"),
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), authors = listOf("测试作者"),
    )

    private fun capture(name: String) {
        val image = captureDeviceScreen()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "local-reading-checks")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
