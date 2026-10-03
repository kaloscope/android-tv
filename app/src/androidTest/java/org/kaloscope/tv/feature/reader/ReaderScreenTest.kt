package org.kaloscope.tv.feature.reader

import android.graphics.Color as AndroidColor
import android.view.KeyEvent as AndroidKeyEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.model.ImagePageDirection
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
import org.kaloscope.tv.test.captureToImage
import kotlin.math.roundToInt

class ReaderScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun textReadingKeepsScreenOnUntilExit() {
        assertReaderKeepsScreenOnWhileActive(textState(text = "正文"))
    }

    @Test
    fun scrollingImageReadingKeepsScreenOnUntilExit() {
        assertReaderKeepsScreenOnWhileActive(imageState())
    }

    @Test
    fun pagedImageReadingKeepsScreenOnUntilExit() {
        assertReaderKeepsScreenOnWhileActive(imageState(readMode = ImageReadMode.Paged))
    }

    @Test
    fun textStartBoundaryOpensControlsWithPreviousChapterFocused() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        control("上一章").assertIsFocused()
    }

    @Test
    fun textParagraphsRefreshWhenChapterContentChanges() {
        var state by mutableStateOf(textState(text = "  第一段 \n\n 第二段  \n\n "))
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

        composeRule.onNodeWithTag("text-reader-paragraph-0").assertTextEquals("第一段")
        composeRule.onNodeWithTag("text-reader-paragraph-1").assertTextEquals("第二段")
        composeRule.onNodeWithTag("text-reader-paragraph-2").assertDoesNotExist()

        composeRule.runOnIdle {
            state = state.copy(
                content = state.content.copy(text = " 替换正文 "),
                contentRevision = state.contentRevision + 1,
            )
        }
        composeRule.onNodeWithTag("text-reader-paragraph-0").assertTextEquals("替换正文")
        composeRule.onNodeWithTag("text-reader-paragraph-1").assertDoesNotExist()

        composeRule.runOnIdle {
            state = state.copy(
                content = state.content.copy(text = " \n\t\r\n "),
                contentRevision = state.contentRevision + 1,
            )
        }
        composeRule.onNodeWithText("本章没有文本内容").assertExists()
        composeRule.onNodeWithTag("text-reader-paragraph-0").assertDoesNotExist()

        composeRule.runOnIdle {
            state = state.copy(
                content = state.content.copy(text = " 恢复正文 "),
                contentRevision = state.contentRevision + 1,
            )
        }
        composeRule.onNodeWithTag("text-reader-paragraph-0").assertTextEquals("恢复正文")
        composeRule.onNodeWithText("本章没有文本内容").assertDoesNotExist()
    }

    @Test
    fun emptyTextSupportsChapterBoundariesAndLayeredBack() {
        var exits = 0
        setReader(textState(text = ""), onBack = { exits += 1 })

        composeRule.onNodeWithText("本章没有文本内容").assertExists()
        val content = composeRule.onNodeWithTag("text-reader-content")
        content.assertIsFocused()
            .performKeyInput {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
            }
            .assertIsFocused()
        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()

        content.performKeyInput { pressKey(Key.DirectionDown) }
        control("下一章").assertIsFocused()
        pressBack()
        content.assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        control("上一章").assertIsFocused()
        pressBack()
        content.assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        control("上一章").assertIsFocused()
        pressBack()
        content.assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, exits) }

        pressBack()
        composeRule.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun blankSingleChapterTextUsesSettingsAtBothBoundaries() {
        setReader(
            textState(
                text = " \n\t\r\n ",
                chapterItems = listOf(chapters.first()),
                selectedChapterIndex = 0,
            ),
        )

        composeRule.onNodeWithText("本章没有文本内容").assertExists()
        val content = composeRule.onNodeWithTag("text-reader-content")
        content.assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        control("阅读设置").assertIsFocused().assertIsEnabled()
        pressBack()
        content.assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        control("阅读设置").assertIsFocused().assertIsEnabled()
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
    fun emptyScrollingImagesKeepFocusThroughLoadingFailureAndRetry() {
        var state by mutableStateOf(
            imageState().let {
                it.copy(
                    content = it.content.copy(imageCount = 2),
                    imagesExhausted = false,
                )
            },
        )
        var loadMoreRequests = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {
                        loadMoreRequests += 1
                        state = state.copy(isLoadingMore = true, pageError = null)
                    },
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
        composeRule.onNodeWithTag("image-reader-scroll")
            .assertIsFocused()
            .performKeyInput {
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
            }
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        control("上一章").assertIsFocused()
        pressBack()
        composeRule.onNodeWithTag("image-reader-scroll")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithTag("reader-image-loading-more-scroll").assertExists()
        composeRule.onNodeWithTag("image-reader-scroll").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(1, loadMoreRequests)
            state = state.copy(isLoadingMore = false, pageError = AppError.Offline)
        }
        composeRule.onNodeWithTag("reader-recoverable-error").assertExists()
        composeRule.onNodeWithTag("image-reader-scroll")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithTag("reader-image-loading-more-scroll").assertExists()
        composeRule.onNodeWithTag("image-reader-scroll").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(2, loadMoreRequests)
            state = state.copy(
                content = state.content.copy(
                    images = listOf(
                        "https://cdn.example.test/page-1.jpg",
                        "https://cdn.example.test/page-2.jpg",
                    ),
                ),
                isLoadingMore = false,
                imagesExhausted = true,
            )
        }
        composeRule.onNodeWithTag("reader-recoverable-error").assertDoesNotExist()
        composeRule.onNodeWithTag("image-reader-scroll")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()
        val viewport = composeRule.onNodeWithTag("image-reader-scroll")
            .fetchSemanticsNode().boundsInRoot
        val secondImage = composeRule.onNodeWithTag("reader-image-1")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.top, secondImage.top, 1f)

        composeRule.onNodeWithTag("image-reader-scroll")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        // Reopening restores the action selected by the earlier start boundary.
        control("上一章").assertIsFocused()
        pressBack()
        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("image-reader-scroll").assertIsFocused()
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
    fun textConfirmationKeysOpenControlsOnlyOnRelease() {
        assertReaderConfirmationKeys(textState(text = "正文"), "text-reader-content")
    }

    @Test
    fun scrollingImageConfirmationKeysOpenControlsOnlyOnRelease() {
        assertReaderConfirmationKeys(imageState(), "image-reader-scroll")
    }

    @Test
    fun pagedImageConfirmationKeysOpenControlsOnlyOnRelease() {
        assertReaderConfirmationKeys(
            imageState(readMode = ImageReadMode.Paged),
            "image-reader-paged",
        )
    }

    @Test
    fun textBottomControlsShowIconsBesideEveryVisibleAction() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        listOf(
            "reader-previous-chapter-icon",
            "reader-chapters-icon",
            "reader-settings-icon",
            "reader-next-chapter-icon",
        ).forEach { tag ->
            val bounds = composeRule.onNodeWithTag(
                testTag = tag,
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot
            assertEquals(22f * density, bounds.width, 1f)
            assertEquals(22f * density, bounds.height, 1f)
        }

        val chaptersIcon = composeRule.onNodeWithTag(
            testTag = "reader-chapters-icon",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val chaptersLabel = composeRule.onNodeWithText(
            text = "章节",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue(chaptersIcon.right <= chaptersLabel.left)
    }

    @Test
    fun chapterNavigationIconsPreserveSourceSvgCutouts() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }

        listOf(
            "reader-previous-chapter-icon",
            "reader-next-chapter-icon",
        ).forEach { tag ->
            val bitmap = composeRule.onNodeWithTag(
                testTag = tag,
                useUnmergedTree = true,
            ).captureToImage().asAndroidBitmap()
            val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
            val brightestRed = (0 until bitmap.height).maxOf { y ->
                (0 until bitmap.width).maxOf { x ->
                    AndroidColor.red(bitmap.getPixel(x, y))
                }
            }

            assertTrue("$tag center should remain transparent", AndroidColor.red(center) < 80)
            assertTrue("$tag outline should remain visible", brightestRed > 200)
        }
    }

    @Test
    fun imageBottomControlsUseTheSharedActionIcons() {
        setReader(imageState())

        composeRule.onNodeWithTag("image-reader-scroll")
            .performKeyInput { pressKey(Key.DirectionCenter) }

        listOf(
            "reader-previous-chapter-icon",
            "reader-chapters-icon",
            "reader-settings-icon",
            "reader-next-chapter-icon",
        ).forEach { tag ->
            composeRule.onNodeWithTag(
                testTag = tag,
                useUnmergedTree = true,
            ).assertExists()
        }
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
    fun imageTitleInitiallyAutoHidesAndThenFollowsControlsVisibility() {
        composeRule.mainClock.autoAdvance = false
        setReader(imageState())
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()

        composeRule.mainClock.advanceTimeBy(3_400)

        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()
        composeRule.onNodeWithTag("image-reader-scroll")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(192)
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()
        control("章节").assertIsFocused()

        pressBack()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(64)

        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()
        composeRule.mainClock.advanceTimeBy(80)

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()
        composeRule.onNodeWithTag("image-reader-scroll").assertIsFocused()
    }

    @Test
    fun textTitleRemainsVisibleWhenControlsFadeOut() {
        composeRule.mainClock.autoAdvance = false
        setReader(textState(text = "正文"))
        composeRule.mainClock.advanceTimeBy(3_400)
        val content = composeRule.onNodeWithTag("text-reader-content")
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(192)
        control("章节").assertIsFocused()
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()

        pressBack()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(64)

        composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()
        composeRule.mainClock.advanceTimeBy(80)

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()
        content.assertIsFocused()
    }

    @Test
    fun textTitleUsesActiveThemeBackground() {
        composeRule.mainClock.autoAdvance = false
        setReader(
            textState(
                text = "正文",
                settings = TextReaderSettings(theme = TextReaderTheme.Cream),
            ),
        )
        composeRule.mainClock.advanceTimeBy(500)

        val bitmap = composeRule.onNodeWithTag(
            testTag = "reader-title-overlay",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()

        assertEquals(Color(0xFFFDF6E3).toArgb(), bitmap.getPixel(bitmap.width / 2, 1))
    }

    @Test
    fun textContentViewportStartsBelowFixedTitle() {
        setReader(textState(text = "正文"))

        val title = composeRule.onNodeWithTag("reader-title-overlay")
            .fetchSemanticsNode().boundsInRoot
        val content = composeRule.onNodeWithTag("text-reader-content")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(title.bottom, content.top, 1f)
    }

    @Test
    fun imageTitleUsesBlackBackground() {
        composeRule.mainClock.autoAdvance = false
        setReader(imageState())
        composeRule.mainClock.advanceTimeBy(500)

        val bitmap = composeRule.onNodeWithTag(
            testTag = "reader-title-overlay",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()

        assertEquals(Color.Black.toArgb(), bitmap.getPixel(bitmap.width / 2, 1))
    }

    @Test
    fun titleUsesSingle80DpLinearGradient() {
        setTitleOverlayOn(Color.Red)

        val overlay = composeRule.onNodeWithTag(
            testTag = "reader-title-overlay",
            useUnmergedTree = true,
        )
        val bounds = overlay.fetchSemanticsNode().boundsInRoot
        val bitmap = overlay.captureToImage().asAndroidBitmap()
        val x = bitmap.width / 2
        val topRed = AndroidColor.red(bitmap.getPixel(x, 1))
        val midpointRed = AndroidColor.red(bitmap.getPixel(x, bitmap.height / 2))
        val bottomRed = AndroidColor.red(bitmap.getPixel(x, bitmap.height - 2))
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        assertEquals(80f * density, bounds.height, 1f)
        assertTrue(topRed in 48..56)
        assertTrue(midpointRed in 148..158)
        assertTrue(bottomRed in 247..255)
    }

    @Test
    fun titleContentFitsWithinGradient() {
        composeRule.mainClock.autoAdvance = false
        setReader(textState(text = "正文"))
        composeRule.mainClock.advanceTimeBy(500)

        val gradient = composeRule.onNodeWithTag(
            testTag = "reader-title-overlay",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithText("测试文本")
            .fetchSemanticsNode().boundsInRoot
        val chapter = composeRule.onNodeWithText("第二章")
            .fetchSemanticsNode().boundsInRoot
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        assertTrue(title.bottom <= gradient.bottom)
        assertTrue(chapter.bottom <= gradient.bottom)
        assertEquals(80f * density, gradient.height, 1f)
    }

    @Test
    fun bottomEdgeGradientFadesTowardBottom() {
        setEdgeGradientOn(
            backgroundColor = Color.Red,
            gradientColor = Color.Black,
            edge = ReaderEdge.Bottom,
        )

        val gradient = composeRule.onNodeWithTag(
            testTag = "reader-edge-gradient-test",
            useUnmergedTree = true,
        )
        val bounds = gradient.fetchSemanticsNode().boundsInRoot
        val bitmap = gradient.captureToImage().asAndroidBitmap()
        val x = bitmap.width / 2
        val topRed = AndroidColor.red(bitmap.getPixel(x, 1))
        val midpointRed = AndroidColor.red(bitmap.getPixel(x, bitmap.height / 2))
        val bottomRed = AndroidColor.red(bitmap.getPixel(x, bitmap.height - 2))
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        assertEquals(80f * density, bounds.height, 1f)
        assertTrue(topRed in 247..255)
        assertTrue(midpointRed in 148..158)
        assertTrue(bottomRed in 48..56)
    }

    @Test
    fun bottomControlsUse80DpGradientAtScreenEdge() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }

        val screen = composeRule.onNodeWithTag("reader-screen")
            .fetchSemanticsNode().boundsInRoot
        val gradient = composeRule.onNodeWithTag(
            testTag = "reader-bottom-gradient",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        assertEquals(80f * density, gradient.height, 1f)
        assertEquals(screen.bottom, gradient.bottom, 1f)
    }

    @Test
    fun disabledBottomControlUsesDistinctOpaqueSurfaceOverLightTextContent() {
        setReader(
            textState(
                text = "正文",
                settings = TextReaderSettings(theme = TextReaderTheme.White),
                selectedChapterIndex = 0,
            ),
        )
        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }

        val disabledButton = control("上一章")
            .assertIsNotEnabled()
            .captureToImage()
            .asAndroidBitmap()
        val enabledButton = control("下一章")
            .assertIsEnabled()
            .captureToImage()
            .asAndroidBitmap()
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val sampleX = (12 * density).toInt()
        val enabledSurface = enabledButton.getPixel(
            sampleX.coerceIn(0, enabledButton.width - 1),
            enabledButton.height / 2,
        )
        val disabledSurface = disabledButton.getPixel(
            sampleX.coerceIn(0, disabledButton.width - 1),
            disabledButton.height / 2,
        )

        assertEquals(Color(0xFF626D7D).toArgb(), disabledSurface)
        assertTrue(
            "Disabled and enabled surfaces should differ",
            disabledSurface != enabledSurface,
        )
    }

    @Test
    fun textTitleStaysVisibleWhileContentScrolls() {
        composeRule.mainClock.autoAdvance = false
        val text = List(80) { index -> "第 $index 段测试正文，用于确认遥控器滚动不会显示标题栏。" }
            .joinToString("\n\n")
        setReader(textState(text = text))
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(3_400)
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()
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
    fun scrollingDownAlignsNextViewportSizedImageTop() {
        setReader(
            imageState(
                images = listOf(
                    "https://cdn.example.test/page-1.jpg",
                    "https://cdn.example.test/page-2.jpg",
                ),
            ),
        )
        val viewport = composeRule.onNodeWithTag("image-reader-scroll")
            .fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithTag("image-reader-scroll")
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        val secondImage = composeRule.onNodeWithTag("reader-image-1")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.top, secondImage.top, 1f)
    }

    @Test
    fun scrollingPaginationUpdatesTheDisplayedPageNumber() {
        val images = (1..3).map { "https://cdn.example.test/page-$it.jpg" }
        var state by mutableStateOf(
            imageState(images = images.take(1)).let {
                it.copy(
                    content = it.content.copy(imageCount = images.size),
                    imagesExhausted = false,
                )
            },
        )
        var loadMoreRequests = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {
                        loadMoreRequests += 1
                        state = state.copy(isLoadingMore = true)
                    },
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
        val content = composeRule.onNodeWithTag("image-reader-scroll")
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithTag("reader-image-loading-more-scroll").assertExists()
        composeRule.runOnIdle {
            assertEquals(1, loadMoreRequests)
            state = state.copy(
                content = state.content.copy(images = images),
                isLoadingMore = false,
                imagesExhausted = true,
            )
        }

        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("第 2 / 3 页").assertExists()
        control("章节").assertIsFocused()
        pressBack()
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("第 3 / 3 页").assertExists()
        pressBack()
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.waitForIdle()
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("第 2 / 3 页").assertExists()
        composeRule.runOnIdle { assertEquals(1, loadMoreRequests) }
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
    fun pagedLoadingFromEmptyContentShowsFirstImage() {
        assertPagedAppendPosition(initialImages = emptyList(), expectedPosition = 1)
    }

    @Test
    fun pagedLoadingAfterLastImageShowsFirstAppendedImage() {
        assertPagedAppendPosition(
            initialImages = listOf("https://cdn.example.test/page-1.jpg"),
            expectedPosition = 2,
        )
    }

    @Test
    fun imageContentRevisionDoesNotRevealHiddenTitle() {
        composeRule.mainClock.autoAdvance = false
        var state by mutableStateOf(imageState())
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
    fun chapterDrawerUsesStandardStartGeometryAndFocusesDeepSelection() {
        val manyChapters = List(24) { index ->
            ReaderChapter(
                id = "chapter-$index",
                title = "第 ${index + 1} 章",
                volume = "长篇",
            )
        }
        setReader(
            textState(
                text = "正文",
                chapterItems = manyChapters,
                selectedChapterIndex = 15,
            ),
        )

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.Enter) }

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val screen = composeRule.onNodeWithTag("reader-screen")
            .fetchSemanticsNode().boundsInRoot
        val drawer = composeRule.onNodeWithTag("reader-chapter-drawer")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(500f * density, drawer.width, density)
        assertEquals(screen.left, drawer.left, 1f)
        control("第 16 章").assertIsFocused()
    }

    @Test
    fun selectingChapterClosesDrawerAndRestoresChapterControlAfterLoading() {
        var selectedIndex = -1
        var state by mutableStateOf(textState(text = "正文"))
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = { index ->
                        selectedIndex = index
                        state = state.copy(isChapterLoading = true)
                    },
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("第三章")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("reader-chapter-drawer").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(2, selectedIndex) }
        composeRule.runOnIdle {
            state = state.copy(
                content = state.content.copy(selectedChapterIndex = selectedIndex),
                contentRevision = state.contentRevision + 1,
                isChapterLoading = false,
            )
        }
        composeRule.waitForIdle()

        control("章节").assertIsFocused()
    }

    @Test
    fun chapterLoadingCoversReaderWithIndicatorAndMessageBelow() {
        setReader(textState(text = "正文").copy(isChapterLoading = true))

        val screen = composeRule.onNodeWithTag("reader-screen")
            .fetchSemanticsNode().boundsInRoot
        val loading = composeRule.onNodeWithTag("reader-chapter-loading")
            .assertIsFocused()
            .fetchSemanticsNode().boundsInRoot
        val indicator = composeRule.onNodeWithTag("reader-chapter-loading-indicator")
            .fetchSemanticsNode().boundsInRoot
        val message = composeRule.onNodeWithText("正在切换章节…")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(screen, loading)
        assertTrue(message.top > indicator.bottom)
    }

    @Test
    fun closingChapterErrorRestoresTheLastVisibleControl() {
        var state by mutableStateOf(textState(text = "正文"))
        var chapterRequests = 0
        var dismissals = 0
        var exits = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = { exits += 1 },
                    onSelectChapter = {
                        chapterRequests += 1
                        state = state.copy(chapterError = AppError.Offline)
                    },
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {
                        dismissals += 1
                        state = state.copy(chapterError = null)
                    },
                    onDismissPageError = {},
                )
            }
        }
        val content = composeRule.onNodeWithTag("text-reader-content")
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        control("下一章")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-recoverable-error").assertExists()
        control("关闭")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("reader-recoverable-error").assertDoesNotExist()
        control("下一章").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(1, chapterRequests)
            assertEquals(1, dismissals)
            assertEquals(0, exits)
        }
        pressBack()
        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        content.assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, exits) }
        pressBack()
        composeRule.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun closingPageErrorRestoresReadingFocusWithoutOpeningControls() {
        var state by mutableStateOf(
            imageState(
                images = listOf(
                    "https://cdn.example.test/page-1.jpg",
                    "https://cdn.example.test/page-2.jpg",
                ),
            ).copy(pageError = AppError.Offline),
        )
        var dismissals = 0
        var loadMoreRequests = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = { loadMoreRequests += 1 },
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {
                        dismissals += 1
                        state = state.copy(pageError = null)
                    },
                )
            }
        }
        composeRule.onNodeWithTag("image-reader-scroll").assertIsFocused()
        control("关闭")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("reader-recoverable-error").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        val content = composeRule.onNodeWithTag("image-reader-scroll")
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("第 2 / 2 页").assertExists()
        control("章节").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(0, loadMoreRequests)
        }
    }

    @Test
    fun retryingImagePageRestoresScrollFocusThroughFailureAndSuccess() {
        assertPageRetryFocus(ImageReadMode.Scroll, showControls = false)
    }

    @Test
    fun retryingImagePageRestoresPagedControlsThroughFailureAndSuccess() {
        assertPageRetryFocus(ImageReadMode.Paged, showControls = true)
    }

    @Test
    fun chapterLoadingAllowsBackToExitReader() {
        var exits = 0
        setReader(
            state = textState(text = "正文").copy(isChapterLoading = true),
            onBack = { exits += 1 },
        )

        composeRule.onNodeWithTag("reader-chapter-loading").assertIsFocused()
        pressBack()

        composeRule.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun backDuringChapterLoadingKeepsCoveredControlsBlocked() {
        var exits = 0
        var state by mutableStateOf(textState(text = "正文"))
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = { exits += 1 },
                    onSelectChapter = { state = state.copy(isChapterLoading = true) },
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("下一章")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-chapter-loading").assertIsFocused()

        pressBack()

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-chapter-loading")
            .assertIsFocused()
            .performKeyInput {
                pressKey(Key.DirectionUp)
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionCenter)
            }
            .assertIsFocused()
        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, exits) }

        pressBack()

        composeRule.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun textSettingsStayDarkOnLightReadingTheme() {
        setReader(
            textState(
                text = "正文",
                settings = TextReaderSettings(theme = TextReaderTheme.White),
            ),
        )

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-text-theme-setting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(500)

        val panel = composeRule.onNodeWithTag("reader-settings-drawer")
            .captureToImage().asAndroidBitmap()
        assertEquals(Color.Black.toArgb(), panel.getPixel(2, panel.height / 2))
        assertEquals(
            OnBackground,
            textLayoutForText("章节显示顺序").layoutInput.style.color,
        )
    }

    @Test
    fun textChapterDrawerStaysDarkOnLightReadingTheme() {
        setReader(
            textState(
                text = "正文",
                settings = TextReaderSettings(theme = TextReaderTheme.White),
            ),
        )

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.Enter) }
        composeRule.mainClock.advanceTimeBy(500)

        val panel = composeRule.onNodeWithTag("reader-chapter-drawer")
            .captureToImage().asAndroidBitmap()
        assertEquals(Color.Black.toArgb(), panel.getPixel(2, panel.height / 2))
        assertEquals(
            OnBackground,
            textLayoutForText("第一章").layoutInput.style.color,
        )
    }

    @Test
    fun textSettingsUseSharedLightTextOnDarkReadingTheme() {
        setReader(
            textState(
                text = "正文",
                settings = TextReaderSettings(theme = TextReaderTheme.Dark),
            ),
        )

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-text-theme-setting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(500)

        assertEquals(
            OnBackground,
            textLayoutForText("章节显示顺序").layoutInput.style.color,
        )
    }

    @Test
    fun textSettingsUseWebUiThemeFieldName() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }

        composeRule.onNode(hasClickAction() and hasText("背景")).assertExists()
    }

    @Test
    fun textFontDialogUsesBuiltInFontFamilyLabels() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-text-font-setting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        val labels = listOf("默认", "SansSerif", "Serif", "Cursive", "Monospace")
        labels.forEachIndexed { index, label ->
            val option = composeRule.onNode(
                hasClickAction() and hasTextExactly(label) and isFocused(),
            ).assertExists()
            if (index != labels.lastIndex) {
                option.performKeyInput { pressKey(Key.DirectionDown) }
            }
        }
    }

    @Test
    fun textSettingsShowUnifiedDpValuesBetweenAdjustmentArrows() {
        setReader(textState(text = "正文"), fontScale = 1f)

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }

        listOf(
            "reader-font-size-setting" to "28dp",
            "reader-paragraph-spacing-setting" to "28dp",
            "reader-horizontal-padding-setting" to "48dp",
        ).forEach { (tag, value) ->
            composeRule.onNodeWithTag(tag)
                .assert(hasText(value, substring = false))
        }
        listOf("(sp)", "(em)", "(dp)", "28", "1.0", "48").forEach { oldValue ->
            composeRule.onNodeWithText(oldValue, useUnmergedTree = true)
                .assertDoesNotExist()
        }

        val decrease = composeRule.onNodeWithTag(
            testTag = "reader-font-size-decrease",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val value = composeRule.onAllNodesWithText(
            text = "28dp",
            useUnmergedTree = true,
        )[0].fetchSemanticsNode().boundsInRoot
        val increase = composeRule.onNodeWithTag(
            testTag = "reader-font-size-increase",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        assertTrue(decrease.right <= value.left)
        assertTrue(value.right <= increase.left)
    }

    @Test
    fun textSettingsKeepAbsoluteValuesIndependentOfFontScale() {
        val expectedFontRelativeDp = with(Density(density = 1f, fontScale = 2f)) {
            28.sp.toDp().value.roundToInt()
        }
        setReader(textState(text = "正文"), fontScale = 2f)

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("reader-font-size-setting")
            .assert(hasText("${expectedFontRelativeDp}dp", substring = false))
        composeRule.onNodeWithTag("reader-paragraph-spacing-setting")
            .assert(hasText("28dp", substring = false))
        composeRule.onNodeWithTag("reader-horizontal-padding-setting")
            .assert(hasText("48dp", substring = false))
    }

    @Test
    fun textParagraphSpacingUsesItsDpEquivalent() {
        setReader(
            state = textState(
                text = "第一段\n\n第二段",
                settings = TextReaderSettings(
                    fontSizeSp = 28,
                    lineHeight = 1f,
                    paragraphSpacingDp = 28,
                ),
            ),
            fontScale = 2f,
        )

        val first = composeRule.onNodeWithTag(
            testTag = "text-reader-paragraph-0",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithTag(
            testTag = "text-reader-paragraph-1",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val expectedSpacingPixels = with(Density(density = density, fontScale = 2f)) {
            28.dp.toPx()
        }

        assertEquals(expectedSpacingPixels, second.top - first.bottom, 0.5f)
    }

    @Test
    fun centerDoesNotAdjustTextFontSize() {
        var state by mutableStateOf(textState(text = "正文"))
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
        val initialSize = state.settings.fontSizeSp
        composeRule.onNodeWithTag("reader-font-size-setting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(initialSize, state.settings.fontSizeSp)
            assertEquals(0, updates)
        }
    }

    @Test
    fun centerAtMaximumTextFontSizeDoesNotUpdate() {
        var updates = 0
        setReaderWithCallbacks(
            state = textState(
                text = "正文",
                settings = TextReaderSettings(
                    fontSizeSp = ReaderSettingsPolicy.MAX_FONT_SIZE_SP,
                ),
            ),
            onTextSettings = { updates += 1 },
        )

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-font-size-setting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle { assertEquals(0, updates) }
    }

    @Test
    fun textSettingsThemeSwatchesAppearOnlyInChoiceDialog() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag(
            testTag = "reader-current-theme-swatch",
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithTag("reader-text-theme-setting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag(
            testTag = "reader-theme-swatch-white",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun imageSettingsMatchBlackReaderBackground() {
        setReader(imageState())

        composeRule.onNodeWithTag("image-reader-scroll")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }
        composeRule.mainClock.advanceTimeBy(500)

        val panel = composeRule.onNodeWithTag("reader-settings-drawer")
            .captureToImage().asAndroidBitmap()
        assertEquals(Color.Black.toArgb(), panel.getPixel(2, panel.height / 2))
    }

    @Test
    fun imageSettingsEnumOpensDialogAndRestoresRowFocus() {
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
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(
            testTag = "reader-chapter-order-increase",
            useUnmergedTree = true,
        ).assertDoesNotExist()

        row.performKeyInput {
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionRight)
        }.assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, updates) }

        row.performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertExists()
        composeRule.onNodeWithTag(
            testTag = "reader-chapter-order-option-ascending-checkbox-indicator",
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(
            testTag = "reader-chapter-order-option-ascending-radio-indicator",
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithTag("reader-chapter-order-option-ascending")
            .assertIsFocused()
            .performKeyInput {
                pressKey(Key.DirectionDown)
                pressKey(Key.Enter)
            }

        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertDoesNotExist()
        row.assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(ReaderChapterOrder.Descending, state.chapterOrder)
            assertEquals(1, updates)
        }
        composeRule.onNodeWithTag(
            testTag = "reader-session-settings-hint-icon",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun imageSettingChangePublishesTheGlobalPreference() {
        var state by mutableStateOf(
            imageState().copy(
                settings = ImageReaderSettings(pageDirection = ImagePageDirection.Left),
            ),
        )
        var persisted: ImageReaderSettings? = null
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {},
                    onImageSettings = { state = state.copy(settings = it) },
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                    onImagePreferencesChanged = { persisted = it },
                )
            }
        }

        composeRule.onNodeWithTag("image-reader-scroll")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-chapter-order-setting")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithTag("reader-image-zoom-setting")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        val modeRow = composeRule.onNodeWithTag("reader-image-read-mode-setting")
            .assertIsFocused()
        val directionRow = composeRule.onNodeWithTag("reader-page-direction-setting")
            .assertIsNotEnabled()
        modeRow.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionRight)
        }.assertIsFocused()
        modeRow.performKeyInput { pressKey(Key.Enter) }
        composeRule.onNode(
            hasClickAction() and hasTextExactly("滚动") and isFocused(),
        )
            .assertExists()
            .performKeyInput {
                pressKey(Key.DirectionDown)
                pressKey(Key.Enter)
            }

        composeRule.runOnIdle {
            assertEquals(ImageReadMode.Paged, state.settings.readMode)
            assertEquals(ImageReadMode.Paged, persisted?.readMode)
            assertEquals(ImagePageDirection.Left, persisted?.pageDirection)
        }
        modeRow.assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        directionRow.assertIsEnabled()
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNode(hasClickAction() and hasTextExactly("向左") and isFocused())
            .assertExists()
        pressBack()
        directionRow.assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        modeRow.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        composeRule.onNode(hasClickAction() and hasTextExactly("翻页") and isFocused())
            .performKeyInput {
                pressKey(Key.DirectionUp)
                pressKey(Key.Enter)
            }

        directionRow.assertIsNotEnabled()
        modeRow.assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
            .assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(ImageReadMode.Scroll, state.settings.readMode)
            assertEquals(ImageReadMode.Scroll, persisted?.readMode)
            assertEquals(ImagePageDirection.Left, state.settings.pageDirection)
            assertEquals(ImagePageDirection.Left, persisted?.pageDirection)
        }
        pressBack()
        control("阅读设置").assertIsFocused()
    }

    @Test
    fun switchingFromScrollToPagedKeepsTheCurrentImage() {
        assertReadModeSwitchKeepsPosition(ImageReadMode.Scroll)
    }

    @Test
    fun switchingFromPagedToScrollKeepsTheCurrentImage() {
        assertReadModeSwitchKeepsPosition(ImageReadMode.Paged)
    }

    @Test
    fun pagedRetryKeepsThePageChosenAfterLoadingFailed() {
        assertPagedRetryPosition(navigateBack = true, expectedPosition = 1)
    }

    @Test
    fun pagedRetryAdvancesWhenTheReaderStaysOnTheLastPage() {
        assertPagedRetryPosition(navigateBack = false, expectedPosition = 3)
    }

    @Test
    fun scrollingRetryKeepsThePositionChosenAfterLoadingFailed() {
        assertScrollingRetryPosition(navigateBack = true, expectedPosition = 1)
    }

    @Test
    fun scrollingRetryAdvancesWhenTheReaderStaysOnTheLastImage() {
        assertScrollingRetryPosition(navigateBack = false, expectedPosition = 3)
    }

    @Test
    fun textSettingChangePublishesTheGlobalPreference() {
        var state by mutableStateOf(textState(text = "正文"))
        var persisted: TextReaderSettings? = null
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = { state = state.copy(settings = it) },
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                    onTextPreferencesChanged = { persisted = it },
                )
            }
        }

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-font-size-setting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.runOnIdle {
            assertEquals(30, state.settings.fontSizeSp)
            assertEquals(30, persisted?.fontSizeSp)
        }
    }

    @Test
    fun backClosesReaderChoiceDialogBeforeDrawerAndRestoresFocus() {
        setReader(textState(text = "正文"))

        composeRule.onNodeWithTag("text-reader-content")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        control("章节").performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").performKeyInput { pressKey(Key.Enter) }
        val themeRow = composeRule.onNodeWithTag("reader-text-theme-setting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertExists()
        composeRule.onNodeWithTag("reader-theme-option-white").assertIsFocused()

        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-settings-drawer").assertExists()
        themeRow.assertIsFocused()
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

        composeRule.onNodeWithText(
            "在此调整的部分阅读偏好会自动同步为全局默认值。",
        ).assertExists()

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

    private fun assertReaderKeepsScreenOnWhileActive(activeState: ReaderUiState.Active) {
        var state by mutableStateOf<ReaderUiState>(activeState)
        var showReader by mutableStateOf(true)
        lateinit var composeView: View
        composeRule.setContent {
            composeView = LocalView.current
            if (showReader) {
                KaloscopeTheme {
                    ReaderScreen(
                        session = session(),
                        state = state,
                        onBack = { showReader = false },
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

        composeRule.runOnIdle {
            assertTrue(composeView.keepScreenOn)
            state = ReaderUiState.Idle
        }
        composeRule.runOnIdle {
            assertFalse(composeView.keepScreenOn)
            state = activeState
        }
        composeRule.runOnIdle {
            assertTrue(composeView.keepScreenOn)
            state = ReaderUiState.Error(
                requestId = activeState.requestId,
                error = AppError.InvalidData("reader_request"),
            )
        }
        composeRule.runOnIdle {
            assertFalse(composeView.keepScreenOn)
            state = activeState
        }
        composeRule.runOnIdle { assertTrue(composeView.keepScreenOn) }

        pressBack()

        composeRule.onNodeWithTag("reader-screen").assertDoesNotExist()
        composeRule.runOnIdle {
            assertFalse(showReader)
            assertFalse(composeView.keepScreenOn)
        }
    }

    private fun assertReaderConfirmationKeys(state: ReaderUiState.Active, contentTag: String) {
        var exits = 0
        setReader(state, onBack = { exits += 1 })
        val content = composeRule.onNodeWithTag(contentTag)
        for (key in listOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)) {
            content.assertIsFocused().performKeyInput { keyDown(key) }
            composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()

            content.performKeyInput { keyUp(key) }

            composeRule.onNodeWithTag("reader-bottom-controls").assertExists()
            composeRule.onNodeWithTag("reader-chapter-drawer").assertDoesNotExist()
            control("章节").assertIsFocused()
            pressBack()
            composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
            content.assertIsFocused()
        }
        composeRule.runOnIdle { assertEquals(0, exits) }
    }

    private fun assertReadModeSwitchKeepsPosition(initialMode: ImageReadMode) {
        val images = (1..3).map { "https://cdn.example.test/page-$it.jpg" }
        var state by mutableStateOf(imageState(readMode = initialMode, images = images))
        var exits = 0
        val initiallyScrolling = initialMode == ImageReadMode.Scroll
        val initialTag = if (initiallyScrolling) "image-reader-scroll" else "image-reader-paged"
        val switchedTag = if (initiallyScrolling) "image-reader-paged" else "image-reader-scroll"
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = { exits += 1 },
                    onSelectChapter = { index ->
                        state = state.copy(
                            content = state.content.copy(selectedChapterIndex = index),
                            contentRevision = state.contentRevision + 1,
                        )
                    },
                    onLoadMoreImages = {},
                    onImageSettings = { state = state.copy(settings = it) },
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
        val initialContent = composeRule.onNodeWithTag(initialTag)
        initialContent.assertIsFocused().performKeyInput {
            pressKey(if (initiallyScrolling) Key.DirectionDown else Key.DirectionRight)
        }
        composeRule.waitForIdle()
        initialContent.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("第 2 / 3 页").assertExists()
        control("章节").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        control("阅读设置").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        val modeSetting = composeRule.onNodeWithTag("reader-image-read-mode-setting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNode(
            hasClickAction() and
                hasTextExactly(if (initiallyScrolling) "滚动" else "翻页") and
                isFocused(),
        ).performKeyInput {
            pressKey(if (initiallyScrolling) Key.DirectionDown else Key.DirectionUp)
            pressKey(Key.Enter)
        }
        composeRule.onNodeWithTag("kaloscope-choice-dialog-panel").assertDoesNotExist()
        modeSetting.assertIsFocused()
        pressBack()
        control("阅读设置").assertIsFocused()
        composeRule.onNodeWithText("第 2 / 3 页").assertExists()
        pressBack()

        val switchedContent = composeRule.onNodeWithTag(switchedTag).assertIsFocused()
        if (!initiallyScrolling) {
            val viewport = switchedContent.fetchSemanticsNode().boundsInRoot
            val secondImage = composeRule.onNodeWithTag("reader-image-1")
                .fetchSemanticsNode().boundsInRoot
            assertEquals(viewport.top, secondImage.top, 1f)
        }
        switchedContent.performKeyInput {
            pressKey(if (initiallyScrolling) Key.DirectionRight else Key.DirectionDown)
        }
        composeRule.waitForIdle()
        switchedContent.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("第 3 / 3 页").assertExists()
        control("下一章")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("第三章").assertExists()
        composeRule.onNodeWithText("第 1 / 3 页").assertExists()
        if (!initiallyScrolling) {
            val viewport = switchedContent.fetchSemanticsNode().boundsInRoot
            val firstImage = composeRule.onNodeWithTag("reader-image-0")
                .fetchSemanticsNode().boundsInRoot
            assertEquals(viewport.top, firstImage.top, 1f)
        }
        composeRule.runOnIdle { assertEquals(0, exits) }
    }

    private fun assertPageRetryFocus(readMode: ImageReadMode, showControls: Boolean) {
        val images = (1..2).map { "https://cdn.example.test/page-$it.jpg" }
        var state by mutableStateOf(
            imageState(readMode = readMode, images = images.take(1)).let {
                it.copy(
                    content = it.content.copy(imageCount = images.size),
                    imagesExhausted = false,
                    pageError = AppError.Offline,
                )
            },
        )
        var loadMoreRequests = 0
        var exits = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = { exits += 1 },
                    onSelectChapter = {},
                    onLoadMoreImages = {
                        loadMoreRequests += 1
                        state = state.copy(isLoadingMore = true, pageError = null)
                    },
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
        val content = composeRule.onNodeWithTag(
            if (readMode == ImageReadMode.Scroll) "image-reader-scroll" else "image-reader-paged",
        )
        content.assertIsFocused()
        if (showControls) {
            content.performKeyInput { pressKey(Key.DirectionCenter) }
            control("章节").performKeyInput { pressKey(Key.DirectionRight) }
            control("阅读设置").assertIsFocused()
        }
        val focusTarget = if (showControls) control("阅读设置") else content

        repeat(2) { attempt ->
            control("重试")
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .performKeyInput { pressKey(Key.Enter) }

            composeRule.onNodeWithTag("reader-recoverable-error").assertDoesNotExist()
            focusTarget.assertIsFocused()
            if (!showControls) {
                composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
            }
            composeRule.runOnIdle {
                assertEquals(attempt + 1, loadMoreRequests)
                assertTrue(state.isLoadingMore)
                state = if (attempt == 0) {
                    state.copy(isLoadingMore = false, pageError = AppError.Offline)
                } else {
                    state.copy(
                        content = state.content.copy(images = images),
                        isLoadingMore = false,
                        imagesExhausted = true,
                    )
                }
            }
            focusTarget.assertIsFocused()
        }

        if (showControls) {
            focusTarget.performKeyInput { pressKey(Key.DirectionLeft) }
            control("章节").assertIsFocused()
            pressBack()
        }
        content.assertIsFocused().performKeyInput {
            pressKey(if (readMode == ImageReadMode.Scroll) Key.DirectionDown else Key.DirectionRight)
        }
        composeRule.waitForIdle()
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("第 2 / 2 页").assertExists()
        pressBack()
        content.assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(2, loadMoreRequests)
            assertEquals(0, exits)
        }
        pressBack()
        composeRule.runOnIdle { assertEquals(1, exits) }
    }

    private fun assertPagedRetryPosition(navigateBack: Boolean, expectedPosition: Int) {
        val images = (1..4).map { "https://cdn.example.test/page-$it.jpg" }
        var state by mutableStateOf(
            imageState(readMode = ImageReadMode.Paged, images = images.take(2)).let {
                it.copy(
                    content = it.content.copy(imageCount = images.size),
                    imagesExhausted = false,
                )
            },
        )
        var loadMoreRequests = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {
                        loadMoreRequests += 1
                        state = state.copy(isLoadingMore = true, pageError = null)
                    },
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
        val content = composeRule.onNodeWithTag("image-reader-paged")
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeBy(250)
        content.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("reader-image-loading-more-paged").assertExists()
        composeRule.runOnIdle {
            assertEquals(1, loadMoreRequests)
            state = state.copy(isLoadingMore = false, pageError = AppError.Offline)
        }
        composeRule.onNodeWithTag("reader-recoverable-error").assertExists()
        if (navigateBack) {
            content.assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
            composeRule.mainClock.advanceTimeBy(250)
        }
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        val positionBeforeRetry = if (navigateBack) 1 else 2
        composeRule.onNodeWithText("第 $positionBeforeRetry / 4 页").assertExists()
        control("重试")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("reader-image-loading-more-paged").assertExists()
        composeRule.runOnIdle {
            assertEquals(2, loadMoreRequests)
            state = state.copy(
                content = state.content.copy(images = images),
                isLoadingMore = false,
                imagesExhausted = true,
            )
        }
        composeRule.onNodeWithTag("reader-recoverable-error").assertDoesNotExist()
        composeRule.onNodeWithText("第 $expectedPosition / 4 页").assertExists()
        composeRule.mainClock.advanceTimeBy(250)
        pressBack()
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeBy(250)
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("第 ${expectedPosition + 1} / 4 页").assertExists()
        composeRule.runOnIdle { assertEquals(2, loadMoreRequests) }
    }

    private fun assertScrollingRetryPosition(navigateBack: Boolean, expectedPosition: Int) {
        val images = (1..4).map { "https://cdn.example.test/page-$it.jpg" }
        var state by mutableStateOf(
            imageState(images = images.take(2)).let {
                it.copy(
                    content = it.content.copy(imageCount = images.size),
                    imagesExhausted = false,
                )
            },
        )
        var loadMoreRequests = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {
                        loadMoreRequests += 1
                        state = state.copy(isLoadingMore = true, pageError = null)
                    },
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
        val content = composeRule.onNodeWithTag("image-reader-scroll")
        val viewportTop = content.fetchSemanticsNode().boundsInRoot.top
        fun assertImageAtTop(index: Int) {
            val imageTop = composeRule.onNodeWithTag("reader-image-$index")
                .fetchSemanticsNode().boundsInRoot.top
            assertEquals(viewportTop, imageTop, 1f)
        }
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()
        content.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithTag("reader-image-loading-more-scroll").assertExists()
        composeRule.runOnIdle {
            assertEquals(1, loadMoreRequests)
            state = state.copy(isLoadingMore = false, pageError = AppError.Offline)
        }
        composeRule.onNodeWithTag("reader-recoverable-error").assertExists()
        if (navigateBack) {
            content.assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
            composeRule.waitForIdle()
            assertImageAtTop(0)
        }
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        val positionBeforeRetry = if (navigateBack) 1 else 2
        composeRule.onNodeWithText("第 $positionBeforeRetry / 4 页").assertExists()
        control("重试")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(2, loadMoreRequests) }
        if (navigateBack) {
            // Loading must preserve the chosen viewport, not just restore it on success.
            assertImageAtTop(0)
            composeRule.onNodeWithText("第 1 / 4 页").assertExists()
        } else {
            composeRule.onNodeWithTag("reader-image-loading-more-scroll").assertExists()
        }
        composeRule.runOnIdle {
            state = state.copy(
                content = state.content.copy(images = images),
                isLoadingMore = false,
                imagesExhausted = true,
            )
        }
        composeRule.onNodeWithTag("reader-recoverable-error").assertDoesNotExist()
        composeRule.onNodeWithText("第 $expectedPosition / 4 页").assertExists()
        assertImageAtTop(expectedPosition - 1)
        pressBack()
        content.assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()
        content.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.onNodeWithText("第 ${expectedPosition + 1} / 4 页").assertExists()
        composeRule.runOnIdle { assertEquals(2, loadMoreRequests) }
    }

    private fun assertPagedAppendPosition(
        initialImages: List<String>,
        expectedPosition: Int,
    ) {
        val allImages = initialImages + listOf(
            "https://cdn.example.test/appended-1.jpg",
            "https://cdn.example.test/appended-2.jpg",
        )
        var state by mutableStateOf(
            imageState(readMode = ImageReadMode.Paged, images = initialImages).let {
                it.copy(
                    content = it.content.copy(imageCount = allImages.size),
                    imagesExhausted = false,
                )
            },
        )
        var loadMoreRequests = 0
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {
                        loadMoreRequests += 1
                        state = state.copy(isLoadingMore = true)
                    },
                    onImageSettings = {},
                    onTextSettings = {},
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
        composeRule.onNodeWithTag("image-reader-paged")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("reader-image-loading-more-paged").assertExists()
        composeRule.runOnIdle {
            assertEquals(1, loadMoreRequests)
            state = state.copy(
                content = state.content.copy(images = allImages),
                isLoadingMore = false,
                imagesExhausted = true,
            )
        }

        composeRule.onNodeWithTag("image-reader-paged")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        composeRule.onNodeWithText("第 $expectedPosition / ${allImages.size} 页").assertExists()
        control("章节").assertIsFocused()
    }

    private fun textLayoutForText(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(results)
            }
        return results.single()
    }

    private fun setReaderWithCallbacks(
        state: ReaderUiState.Active,
        onTextSettings: (TextReaderSettings) -> Unit,
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                ReaderScreen(
                    session = session(),
                    state = state,
                    onBack = {},
                    onSelectChapter = {},
                    onLoadMoreImages = {},
                    onImageSettings = {},
                    onTextSettings = onTextSettings,
                    onChapterOrder = {},
                    onDismissChapterError = {},
                    onDismissPageError = {},
                )
            }
        }
    }

    private fun setReader(
        state: ReaderUiState.Active,
        onBack: () -> Unit = {},
        fontScale: Float? = null,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = fontScale ?: currentDensity.fontScale,
                ),
            ) {
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
    }

    private fun setTitleOverlayOn(backgroundColor: Color) {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
            ) {
                ReaderTitleOverlay(
                    title = "测试标题",
                    chapter = null,
                    textColor = Color.White,
                    mutedColor = Color.LightGray,
                    scrimColor = Color.Black,
                    status = null,
                )
            }
        }
    }

    private fun setEdgeGradientOn(
        backgroundColor: Color,
        gradientColor: Color,
        edge: ReaderEdge,
    ) {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
            ) {
                ReaderEdgeGradient(
                    color = gradientColor,
                    edge = edge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reader-edge-gradient-test"),
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
    chapterItems: List<ReaderChapter> = chapters,
    selectedChapterIndex: Int = 1,
) = ReaderUiState.Text(
    requestId = "reader-request",
    serverId = "server-id",
    content = ReaderTextContent.network(
        indexerId = 7,
        resourceId = "text-resource",
        chapterId = chapterItems.getOrNull(selectedChapterIndex)?.id,
        title = "测试文本",
        text = text,
        chapters = chapterItems,
        selectedChapterIndex = selectedChapterIndex,
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
