package org.kaloscope.tv.feature.reader

import android.graphics.Color as AndroidColor
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
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
import org.kaloscope.tv.core.designsystem.OnBackground
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
        composeRule.onNodeWithTag("reader-title-overlay").assertExists()

        pressBack()
        composeRule.mainClock.advanceTimeBy(500)

        composeRule.onNodeWithTag("reader-bottom-controls").assertDoesNotExist()
        composeRule.onNodeWithTag("reader-title-overlay").assertDoesNotExist()
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
    fun textSettingsUseReadableRestingTextOnLightTheme() {
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
        assertEquals(Color(0xFFFDFDFD).toArgb(), panel.getPixel(2, panel.height / 2))
        assertEquals(
            OnBackground,
            textLayoutForText("章节显示顺序").layoutInput.style.color,
        )
    }

    @Test
    fun textChapterDrawerUsesReadableRestingTextOnLightTheme() {
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

        assertEquals(
            OnBackground,
            textLayoutForText("第一章").layoutInput.style.color,
        )
    }

    @Test
    fun textSettingsKeepWebUiTextColorOnDarkTheme() {
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
            Color(0xFFCCCCCC),
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
