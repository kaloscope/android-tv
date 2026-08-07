package org.kaloscope.tv.feature.reader

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.TextReaderSettings

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
    fun centerKeepsContentFocusAndDownEntersVisibleControls() {
        setReader(textState(text = "正文"))
        val content = composeRule.onNodeWithTag("text-reader-content")

        content.performKeyInput { pressKey(Key.DirectionCenter) }

        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        content.assertIsFocused()

        content.performKeyInput { pressKey(Key.DirectionDown) }
        control("章节").assertIsFocused()
    }

    @Test
    fun reopeningControlsReturnsToLastFocusedAvailableAction() {
        setReader(textState(text = "正文"))
        val content = composeRule.onNodeWithTag("text-reader-content")
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        content.performKeyInput { pressKey(Key.DirectionDown) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").assertIsFocused()

        pressBack()
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        content.performKeyInput { pressKey(Key.DirectionDown) }

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
        content.performKeyInput { pressKey(Key.DirectionDown) }
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
    fun titleAutoHidesAndVisibleControlsRevealItAgain() {
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
    }

    @Test
    fun chapterAndSettingsDrawersOpenOnOppositeSides() {
        setReader(textState(text = "正文"))
        val content = composeRule.onNodeWithTag("text-reader-content")
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        content.performKeyInput { pressKey(Key.DirectionDown) }
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

private fun textState(text: String) = ReaderUiState.Text(
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
    settings = TextReaderSettings(),
    chapterOrder = ReaderChapterOrder.Ascending,
)

private fun imageState() = ReaderUiState.Image(
    requestId = "reader-request",
    serverId = "server-id",
    content = ReaderImageContent.network(
        indexerId = 7,
        resourceId = "image-resource",
        chapterId = "chapter-2",
        title = "测试图片",
        images = emptyList(),
        imageCount = 0,
        chapters = chapters,
        selectedChapterIndex = 1,
    ),
    settings = ImageReaderSettings(readMode = ImageReadMode.Scroll),
    chapterOrder = ReaderChapterOrder.Ascending,
)

private fun session() = Session(
    server = SavedServer("server-id", "Fixture", "https://example.test"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
