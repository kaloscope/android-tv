package org.kaloscope.tv.feature.reader

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.designsystem.KaloscopeControlTokens
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderSettingsPolicy
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.TextReaderTheme

class ReaderScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun textStartBoundaryOpensControlsWithPreviousChapterFocused() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        control("上一章").assertIsFocused()
    }

    @Test
    fun emptyScrollingImagesEndBoundaryFocusesNextChapter() {
        setReader(imageState())

        composeRule.onNodeWithTag("image-reader-scroll")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        control("下一章").assertIsFocused()
    }

    @Test
    fun centerOpensControlsWithDefaultActionFocused() {
        setReader(textState(text = "正文"))
        val content = composeRule.onNodeWithTag("text-reader-content")

        content.performKeyInput { pressKey(Key.DirectionCenter) }

        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        composeRule.onNodeWithTag("reader-chapter-drawer").assertDoesNotExist()
        control("章节").assertIsFocused()
    }

    @Test
    fun imageCenterOpensControlsWithDefaultActionFocused() {
        setReader(imageState())
        val content = composeRule.onNodeWithTag("image-reader-scroll")

        content.performKeyInput { pressKey(Key.DirectionCenter) }

        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        composeRule.onNodeWithTag("reader-chapter-drawer").assertDoesNotExist()
        control("章节").assertIsFocused()
    }

    @Test
    fun reopeningControlsReturnsToLastFocusedAvailableAction() {
        setReader(textState(text = "正文"))
        val content = composeRule.onNodeWithTag("text-reader-content")
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").assertIsFocused()

        pressBack()
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()

        control("阅读设置").assertIsFocused()
    }

    @Test
    fun backClosesDrawerThenControlsThenReader() {
        var backCount by mutableIntStateOf(0)
        setReader(
            state = textState(text = "正文"),
            onBack = { backCount += 1 },
        )
        val content = composeRule.onNodeWithTag("text-reader-content")
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        control("章节").performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-chapter-drawer").assertExists()

        pressBack()

        composeRule.onNodeWithTag("reader-chapter-drawer").assertDoesNotExist()
        control("章节").assertIsFocused()

        pressBack()

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        content.assertIsFocused()

        pressBack()

        composeRule.runOnIdle { assertEquals(1, backCount) }
    }

    @Test
    fun titleInitiallyAutoHidesAndThenFollowsControlsVisibility() {
        composeRule.mainClock.autoAdvance = false
        setReader(textState(text = "正文"))
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()

        composeRule.mainClock.advanceTimeBy(3_400)

        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()
        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()

        pressBack()
        composeRule.mainClock.advanceTimeBy(500)

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()
    }

    @Test
    fun textTitleUsesOpaqueActiveThemeBackgroundWithoutMovingContent() {
        composeRule.mainClock.autoAdvance = false
        val paragraphs = List(80) { index -> "第 $index 段正文" }
        setReader(
            textState(
                text = paragraphs.joinToString("\n\n"),
                settings = TextReaderSettings(theme = TextReaderTheme.Cream),
            ),
        )
        composeRule.mainClock.advanceTimeBy(500)

        val bitmap = composeRule.onNodeWithTag(
            testTag = "reader-title-solid-scrim",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()

        assertEquals(Color(0xFFFDF6E3).toArgb(), bitmap.getPixel(bitmap.width / 2, 1))

        composeRule.mainClock.advanceTimeBy(3_400)
        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.mainClock.advanceTimeBy(1_000)
        val screen = composeRule.onNodeWithTag("reader-screen")
            .fetchSemanticsNode().boundsInRoot
        val visibleParagraphIndex = paragraphs.indices.first { index ->
            val bounds = composeRule.onNodeWithTag("text-reader-paragraph-$index")
                .fetchSemanticsNode().boundsInRoot
            bounds.height > 0f && bounds.top >= screen.top && bounds.bottom <= screen.bottom
        }
        val hiddenParagraphTop = composeRule.onNodeWithTag(
            "text-reader-paragraph-$visibleParagraphIndex",
        ).fetchSemanticsNode().boundsInRoot.top
        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.mainClock.advanceTimeBy(500)
        val revealedParagraphTop = composeRule.onNodeWithTag(
            "text-reader-paragraph-$visibleParagraphIndex",
        )
            .fetchSemanticsNode().boundsInRoot.top

        assertEquals(hiddenParagraphTop, revealedParagraphTop, 0.5f)
    }

    @Test
    fun imageTitleUsesOpaqueBlackBackground() {
        composeRule.mainClock.autoAdvance = false
        setReader(imageState())
        composeRule.mainClock.advanceTimeBy(500)

        val bitmap = composeRule.onNodeWithTag(
            testTag = "reader-title-solid-scrim",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()

        assertEquals(Color.Black.toArgb(), bitmap.getPixel(bitmap.width / 2, 1))
    }

    @Test
    fun titleGradientStartsBelowAllTitleText() {
        composeRule.mainClock.autoAdvance = false
        setReader(textState(text = "正文"))
        composeRule.mainClock.advanceTimeBy(500)

        val solid = composeRule.onNodeWithTag(
            testTag = "reader-title-solid-scrim",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val gradient = composeRule.onNodeWithTag(
            testTag = "reader-title-gradient-tail",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithText("测试文本")
            .fetchSemanticsNode().boundsInRoot
        val chapter = composeRule.onNodeWithText("第二章")
            .fetchSemanticsNode().boundsInRoot
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        assertTrue(title.bottom <= solid.bottom)
        assertTrue(chapter.bottom <= solid.bottom)
        assertEquals(solid.bottom, gradient.top, 1f)
        assertEquals(28f * density, gradient.height, 1f)
    }

    @Test
    fun textScrollingDoesNotRevealHiddenTitle() {
        composeRule.mainClock.autoAdvance = false
        val text = List(80) { index -> "第 $index 段测试正文，用于确认遥控器滚动不会显示标题栏。" }
            .joinToString("\n\n")
        setReader(textState(text = text))
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(3_400)
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()
    }

    @Test
    fun imageScrollingDoesNotRevealHiddenTitle() {
        composeRule.mainClock.autoAdvance = false
        setReader(
            imageState(
                images = listOf(
                    "https://cdn.example.test/page-1.jpg",
                    "https://cdn.example.test/page-2.jpg",
                ),
            ),
        )
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(3_400)
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()

        composeRule.onNodeWithTag("image-reader-scroll")
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()
    }

    @Test
    fun imagePagingDoesNotRevealHiddenTitle() {
        composeRule.mainClock.autoAdvance = false
        setReader(
            imageState(
                readMode = ImageReadMode.Paged,
                images = listOf(
                    "https://cdn.example.test/page-1.jpg",
                    "https://cdn.example.test/page-2.jpg",
                ),
            ),
        )
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(3_400)
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()

        composeRule.onNodeWithTag("image-reader-paged")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()
    }

    @Test
    fun contentRevisionDoesNotRevealHiddenTitle() {
        composeRule.mainClock.autoAdvance = false
        var state by mutableStateOf(textState(text = "正文"))
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(3_400)
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()

        composeRule.runOnIdle {
            state = state.copy(contentRevision = state.contentRevision + 1)
        }
        composeRule.mainClock.advanceTimeBy(500)

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()
    }

    @Test
    fun chapterAndSettingsDrawersOpenOnOppositeSides() {
        setReader(textState(text = "正文"))
        val content = composeRule.onNodeWithTag("text-reader-content")
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        control("章节").performKeyInput { pressKey(Key.Enter) }

        val screenCenter = composeRule.onNodeWithTag("reader-screen")
            .fetchSemanticsNode().boundsInRoot.center.x
        val chapterCenter = composeRule.onNodeWithTag("reader-chapter-drawer")
            .fetchSemanticsNode().boundsInRoot.center.x
        assertTrue(chapterCenter < screenCenter)

        pressBack()
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").assertIsFocused().performKeyInput { pressKey(Key.Enter) }

        val settingsCenter = composeRule.onNodeWithTag("reader-settings-drawer")
            .fetchSemanticsNode().boundsInRoot.center.x
        assertTrue(settingsCenter > screenCenter)
    }

    @Test
    fun textSettingsThemeSwatchTracksTheSessionTheme() {
        var state by mutableStateOf(textState(text = "正文"))
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = { settings ->
                        state = state.copy(settings = settings)
                    },
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }

        val content = composeRule.onNodeWithTag("text-reader-content")
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }

        val themeRow = composeRule.onNodeWithTag("reader-text-theme-setting")
        fun currentSwatchColor(): Int {
            val bitmap = composeRule.onNodeWithTag(
                testTag = "reader-current-theme-swatch",
                useUnmergedTree = true,
            ).captureToImage().asAndroidBitmap()
            return bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        }

        assertEquals(Color(0xFFFAFAF5).toArgb(), currentSwatchColor())
        themeRow
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()

        assertEquals(Color(0xFFFDF6E3).toArgb(), currentSwatchColor())
    }

    @Test
    fun imageSettingsEnumArrowsDisableAtBoundariesWithoutWrapping() {
        var state by mutableStateOf(imageState())
        var updates = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = { chapterOrder ->
                        updates += 1
                        state = state.copy(chapterOrder = chapterOrder)
                    },
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }

        composeRule.onNodeWithTag("image-reader-scroll")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }

        val row = composeRule.onNodeWithTag("reader-chapter-order-setting")
            .assertIsFocused()
        composeRule.onNodeWithTag(
            testTag = "reader-chapter-order-decrease",
            useUnmergedTree = true,
        ).assertIsNotEnabled()
        composeRule.onNodeWithTag(
            testTag = "reader-chapter-order-increase",
            useUnmergedTree = true,
        ).assertIsEnabled()
        assertEquals(
            textLayoutForTag("reader-chapter-order-increase")
                .layoutInput.style.color.copy(alpha = KaloscopeControlTokens.DisabledAlpha),
            textLayoutForTag("reader-chapter-order-decrease").layoutInput.style.color,
        )

        row.performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.runOnIdle { assertEquals(0, updates) }

        row.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(
            testTag = "reader-chapter-order-decrease",
            useUnmergedTree = true,
        ).assertIsEnabled()
        composeRule.onNodeWithTag(
            testTag = "reader-chapter-order-increase",
            useUnmergedTree = true,
        ).assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(1, updates) }

        row.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.runOnIdle { assertEquals(1, updates) }
        composeRule.onNodeWithTag(
            testTag = "reader-session-settings-hint-icon",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun textNumericArrowsDisableAtPolicyBoundsWithoutInvalidUpdates() {
        var state by mutableStateOf(
            textState(
                text = "正文",
                settings = TextReaderSettings(
                    fontSizeSp = ReaderSettingsPolicy.MIN_FONT_SIZE_SP,
                ),
            ),
        )
        var updates = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = { settings ->
                        updates += 1
                        state = state.copy(settings = settings)
                    },
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }

        val row = composeRule.onNodeWithTag("reader-font-size-setting")
        composeRule.onNodeWithTag(
            testTag = "reader-font-size-decrease",
            useUnmergedTree = true,
        ).assertIsNotEnabled()
        composeRule.onNodeWithTag(
            testTag = "reader-font-size-increase",
            useUnmergedTree = true,
        ).assertIsEnabled()
        row.performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.runOnIdle { assertEquals(0, updates) }

        composeRule.runOnIdle {
            state = state.copy(
                settings = state.settings.copy(
                    fontSizeSp = ReaderSettingsPolicy.MAX_FONT_SIZE_SP,
                ),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(
            testTag = "reader-font-size-decrease",
            useUnmergedTree = true,
        ).assertIsEnabled()
        composeRule.onNodeWithTag(
            testTag = "reader-font-size-increase",
            useUnmergedTree = true,
        ).assertIsNotEnabled()
        row.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.runOnIdle { assertEquals(0, updates) }
        composeRule.onNodeWithTag(
            testTag = "reader-session-settings-hint-icon",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun readerSettingsHintUsesSmallVerticallyCenteredIcon() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }

        val icon = composeRule.onNodeWithTag(
            testTag = "reader-session-settings-hint-icon",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val text = composeRule.onNodeWithTag(
            testTag = "reader-session-settings-hint-text",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        assertEquals(14f * density, icon.width, 0.5f)
        assertEquals(text.center.y, icon.center.y, density)
    }

    @Test
    fun pagedLoadingUsesCenteredIndicatorWithoutLegacyText() {
        setReader(
            imageState(
                readMode = ImageReadMode.Paged,
                images = listOf("https://cdn.example.test/page-1.jpg"),
                isLoadingMore = true,
            ),
        )

        val screen = composeRule.onNodeWithTag("reader-screen")
            .fetchSemanticsNode().boundsInRoot
        val indicator = composeRule.onNodeWithTag(
            testTag = "reader-image-loading-more-paged-indicator",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        assertEquals(screen.center.x, indicator.center.x, 1f)
        assertEquals(screen.center.y, indicator.center.y, 1f)
        composeRule.onNodeWithText("正在加载后续图片…").assertDoesNotExist()
    }

    @Test
    fun scrollingLoadingAppearsInlineAfterTheLastImage() {
        setReader(
            imageState(
                images = listOf("https://cdn.example.test/page-1.jpg"),
                isLoadingMore = true,
            ),
        )

        val screen = composeRule.onNodeWithTag("reader-screen")
            .fetchSemanticsNode().boundsInRoot
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                val loadingSlot = composeRule.onNodeWithTag(
                    testTag = "reader-image-loading-more-scroll",
                    useUnmergedTree = true,
                ).fetchSemanticsNode().boundsInRoot
                loadingSlot.center.y > screen.center.y && loadingSlot.bottom <= screen.bottom + 1f
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(
            testTag = "reader-image-loading-more-scroll-indicator",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithText("正在加载后续图片…").assertDoesNotExist()
    }

    private fun textLayoutForTag(tag: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag(tag, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(results)
            }
        return results.single()
    }

    private fun setReader(
        state: ReaderUiState.Active,
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = onBack,
                    onSelectChapter = {},
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
    }

    private fun control(label: String) =
        composeRule.onNode(hasClickAction() and hasText(label))

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()
    }
}

private val chapters = listOf(
    ReaderChapter("chapter-1", "第一章", "第一卷"),
    ReaderChapter("chapter-2", "第二章", "第一卷"),
    ReaderChapter("chapter-3", "第三章", "第一卷"),
)

private fun textState(
    text: String,
    settings: TextReaderSettings = TextReaderSettings(),
) = ReaderUiState.Text(
    requestId = "reader-request",
    serverId = "server-id",
    content = ReaderTextContent.network(
        indexerId = 7,
        resourceId = "text-resource",
        chapterId = "chapter-2",
        title = "测试文本",
        text = text,
        chapters = chapters,
        selectedChapterIndex = 1,
    ),
    settings = settings,
    chapterOrder = ReaderChapterOrder.Ascending,
)

private fun imageState(
    readMode: ImageReadMode = ImageReadMode.Scroll,
    images: List<String> = emptyList(),
    isLoadingMore: Boolean = false,
) = ReaderUiState.Image(
    requestId = "reader-request",
    serverId = "server-id",
    content = ReaderImageContent.network(
        indexerId = 7,
        resourceId = "image-resource",
        chapterId = "chapter-2",
        title = "测试图片",
        images = images,
        imageCount = images.size,
        chapters = chapters,
        selectedChapterIndex = 1,
    ),
    settings = ImageReaderSettings(readMode = readMode),
    chapterOrder = ReaderChapterOrder.Ascending,
    isLoadingMore = isLoadingMore,
    imagesExhausted = !isLoadingMore,
)

private fun session() = Session(
    server = SavedServer("server-id", "Fixture", "https://example.test"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
