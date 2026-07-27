package org.kaloscope.tv.feature.home

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.TimeZone
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.WatchHistoryItem

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyHomeShowsRecentWatchingWithoutExtraDescriptions() {
        showEmptyHome()

        composeRule.onNodeWithText("最近观看").assertExists()
        composeRule.onNodeWithText("首页").assertDoesNotExist()
        composeRule.onNodeWithText("来自当前服务器的真实观看历史").assertDoesNotExist()
        composeRule.onNodeWithText("暂无观看记录").assertExists()
        composeRule.onNodeWithText("播放媒体库内容后，真实进度会显示在这里。")
            .assertDoesNotExist()
        composeRule.onNodeWithText("进入媒体库").assertHasClickAction()
    }

    @Test
    fun refreshIconInvokesRefreshAction() {
        var refreshed = false
        showEmptyHome(onRefresh = { refreshed = true })

        composeRule.onNodeWithContentDescription("刷新").assertExists()
        composeRule.onNodeWithTag("home-refresh")
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("home-refresh")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertTrue(refreshed)
        }
    }

    @Test
    fun refreshButtonHasSubtleRestingOutline() {
        showEmptyHome()
        composeRule.onNodeWithText("进入媒体库")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        val bitmap = composeRule.onNodeWithTag("home-refresh")
            .captureToImage()
            .asAndroidBitmap()
        val sampleDepth = with(composeRule.density) {
            2.dp.roundToPx().coerceAtLeast(1)
        }
        val sampleHalfWidth = with(composeRule.density) {
            4.dp.roundToPx().coerceAtLeast(1)
        }
        val centerX = bitmap.width / 2
        var redTotal = 0L
        var sampleCount = 0L
        for (x in centerX - sampleHalfWidth until centerX + sampleHalfWidth) {
            for (y in 0 until sampleDepth) {
                redTotal += AndroidColor.red(bitmap.getPixel(x, y))
                sampleCount += 1
            }
        }
        val averageRed = redTotal.toDouble() / sampleCount

        assertTrue(
            "Expected a subtle light outline at the top edge, average red=$averageRed",
            averageRed in 35.0..60.0,
        )
    }

    @Test
    fun historyCarouselMovesRightAndUpdatesSelectedBackdrop() {
        var selectedBackdrop: HomeBackdropPresentation? = null
        showContentHome(
            onBackdropChanged = { selectedBackdrop = it },
        )

        composeRule.onAllNodesWithText("最近观看").assertCountEquals(1)
        composeRule.onNodeWithTag("history-selected-title").assertTextEquals("星海纪行")
        composeRule.onNodeWithTag("history-card-301")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("history-card-302").assertIsFocused()
        composeRule.onNodeWithTag("history-selected-title").assertTextEquals("森林来信")
        composeRule.runOnIdle {
            assertEquals("/backdrops/forest.webp", selectedBackdrop?.path)
        }
    }

    @Test
    fun recentWatchingLabelIsSecondaryToSelectedTitle() {
        showContentHome()

        val label = textLayoutFor("最近观看")
        val title = textLayoutForTag("history-selected-title")

        assertTrue(
            label.layoutInput.style.fontSize.value <
                title.layoutInput.style.fontSize.value,
        )
        assertEquals(Muted, label.layoutInput.style.color)
    }

    @Test
    fun cardShowsLocalTimestampAndOnlyOneProgressIndicator() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"))
            showContentHome()

            composeRule.onAllNodesWithText("2026/07/27 16:00:00")
                .assertCountEquals(2)
            composeRule.onAllNodesWithTag("history-progress")
                .assertCountEquals(1)
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun unselectedCardKeepsSelectedCardContentStrength() {
        val duplicatedVisuals = historyItems().mapIndexed { index, item ->
            item.copy(
                mediaId = 301L + index,
                parentTitle = "同一海报",
                title = "同一集",
                posterPath = null,
                backdropPath = null,
            )
        }
        showContentHome(items = duplicatedVisuals)

        composeRule.onNodeWithTag("history-card-301").assertIsSelected()
        composeRule.onNodeWithTag("history-card-302").assertIsNotSelected()
        val selected = composeRule.onNodeWithTag(
            "history-card-poster-301",
            useUnmergedTree = true,
        )
            .captureToImage()
            .asAndroidBitmap()
        val unselected = composeRule.onNodeWithTag(
            "history-card-poster-302",
            useUnmergedTree = true,
        )
            .captureToImage()
            .asAndroidBitmap()
        val selectedLuminance = averagePosterLuminance(selected)
        val unselectedLuminance = averagePosterLuminance(unselected)

        assertTrue(
            "Expected selected ($selectedLuminance) and unselected " +
                "($unselectedLuminance) posters to have matching visual strength",
            abs(unselectedLuminance - selectedLuminance) <=
                selectedLuminance * 0.08,
        )
    }

    @Test
    fun restingCardUsesFaintNeutralBorderBeforeBrightWhiteFocusBorder() {
        showContentHome()
        composeRule.onNodeWithTag("home-refresh")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        val resting = composeRule.onNodeWithTag("history-card-301")
            .captureToImage()
            .asAndroidBitmap()

        composeRule.onNodeWithTag("history-card-301")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
        val focused = composeRule.onNodeWithTag("history-card-301")
            .captureToImage()
            .asAndroidBitmap()

        val restingBorder = averageTopCenterColor(resting)
        val focusedBorder = averageTopCenterColor(focused)

        assertTrue(
            "Resting border must be neutral rather than purple: $restingBorder",
            restingBorder.channelSpread <= 45.0,
        )
        assertTrue(
            "Resting border must remain faintly visible: $restingBorder",
            restingBorder.luminance >= 35.0,
        )
        assertTrue(
            "Focused white border must be clearly brighter: " +
                "resting=$restingBorder, focused=$focusedBorder",
            focusedBorder.luminance >= restingBorder.luminance * 1.5,
        )
    }

    @Test
    fun centerOnCarouselCardResumesFocusedEpisode() {
        var playedMediaId: Long? = null
        showContentHome(
            onPlayHistory = { playedMediaId = it.mediaId },
        )

        composeRule.onNodeWithTag("history-card-302")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertTrue(playedMediaId == 302L)
        }
    }

    @Test
    fun requestedMediaIsSelectedAndFocusedWhenHomeReturns() {
        showContentHome(restoreMediaId = 302L)

        composeRule.onNodeWithTag("history-card-302").assertIsFocused()
        composeRule.onNodeWithTag("history-selected-title").assertTextEquals("森林来信")
    }

    @Test
    fun carouselSupportsTwoDimensionalDpadNavigation() {
        showContentHome()

        composeRule.onNodeWithTag("history-card-301")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("history-card-302")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNodeWithTag("history-card-301")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithText("继续播放")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithTag("history-card-301").assertIsFocused()
    }

    @Test
    fun selectedHistoryActionsRemainVisibleAboveCarousel() {
        val items = historyItems().toMutableList()
        items[0] = items[0].copy(
            parentTitle = "无职转生～到了异世界就拿出真本事～ 第二季",
        )
        showContentHome(items = items)

        composeRule.onNodeWithText("继续播放").assertIsDisplayed()
        composeRule.onNodeWithText("查看详情").assertIsDisplayed()
        composeRule.onNodeWithTag("history-carousel").assertIsDisplayed()
        val actionBounds = composeRule.onNodeWithText("继续播放")
            .fetchSemanticsNode()
            .boundsInRoot
        val carouselBounds = composeRule.onNodeWithTag("history-carousel")
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            "Selected history actions must not overlap the carousel",
            actionBounds.bottom <= carouselBounds.top,
        )
    }

    @Test
    fun longSelectedTitleKeepsActionButtonsAtFullHeight() {
        val items = historyItems().toMutableList()
        items[0] = items[0].copy(
            parentTitle = "无职转生～到了异世界就拿出真本事～ 第二季",
        )
        showContentHome(
            items = items,
            viewportHeight = 416.dp,
        )

        val minimumActionHeight = with(composeRule.density) { 42.dp.toPx() }
        val resumeBounds = composeRule.onNodeWithText("继续播放")
            .fetchSemanticsNode()
            .boundsInRoot
        val detailBounds = composeRule.onNodeWithText("查看详情")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "Resume action must keep its full height when the title wraps",
            resumeBounds.height >= minimumActionHeight,
        )
        assertTrue(
            "Detail action must keep its full height when the title wraps",
            detailBounds.height >= minimumActionHeight,
        )
    }

    @Test
    fun longSelectedTitleUsesNonOverlappingLines() {
        val items = historyItems().toMutableList()
        items[0] = items[0].copy(
            parentTitle = "无职转生～到了异世界就拿出真本事～ 第二季",
        )
        showContentHome(items = items)

        val layoutResults = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag("history-selected-title")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(layoutResults)
            }

        val layout = layoutResults.single()
        assertEquals(2, layout.lineCount)
        val lineAdvance = layout.getLineTop(1) - layout.getLineTop(0)
        val minimumAdvance = with(layout.layoutInput.density) { 37.sp.toPx() }
        assertTrue(
            "Selected title lines must have enough vertical separation",
            lineAdvance >= minimumAdvance,
        )
    }

    @Test
    fun firstFocusedCardHasSafeCarouselInsets() {
        showContentHome()

        composeRule.onNodeWithTag("history-card-301")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.waitForIdle()

        val carouselBounds = composeRule.onNodeWithTag("history-carousel")
            .fetchSemanticsNode()
            .boundsInRoot
        val firstCardBounds = composeRule.onNodeWithTag("history-card-301")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(firstCardBounds.left > carouselBounds.left)
        assertTrue(firstCardBounds.top > carouselBounds.top)
    }

    @Test
    fun carouselShowsFadesOnlyForScrollableDirections() {
        val items = historyItems() + listOf(
            historyItem(
                historyId = 403,
                mediaId = 303,
                title = "潮声",
                parentTitle = "海岸来信",
                posterPath = "/posters/coast.webp",
                backdropPath = "/backdrops/coast.webp",
                episode = 5,
            ),
            historyItem(
                historyId = 404,
                mediaId = 304,
                title = "雪夜",
                parentTitle = "北境纪行",
                posterPath = "/posters/snow.webp",
                backdropPath = "/backdrops/snow.webp",
                episode = 6,
            ),
        )
        showContentHome(items = items)

        composeRule.onNodeWithTag("history-carousel-end-fade").assertExists()
        composeRule.onNodeWithTag("history-carousel-start-fade").assertDoesNotExist()

        composeRule.onNodeWithTag("history-card-301")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("history-card-302").assertIsFocused()
        composeRule.onNodeWithTag("history-carousel-start-fade").assertExists()
    }

    @Test
    fun endFadeCoversEnoughOfContinuationCardToAvoidHardCut() {
        val items = historyItems() + listOf(
            historyItem(
                historyId = 403,
                mediaId = 303,
                title = "潮声",
                parentTitle = "海岸来信",
                posterPath = "/posters/coast.webp",
                backdropPath = "/backdrops/coast.webp",
                episode = 5,
            ),
            historyItem(
                historyId = 404,
                mediaId = 304,
                title = "雪夜",
                parentTitle = "北境纪行",
                posterPath = "/posters/snow.webp",
                backdropPath = "/backdrops/snow.webp",
                episode = 6,
            ),
        )
        showContentHome(items = items)

        val minimumFadeWidth = with(composeRule.density) { 96.dp.toPx() }
        val fadeBounds = composeRule.onNodeWithTag("history-carousel-end-fade")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "End fade must cover a meaningful portion of the continuation card",
            fadeBounds.width >= minimumFadeWidth,
        )
    }

    private fun showEmptyHome(onRefresh: () -> Unit = {}) {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background),
                ) {
                    HomeScreen(
                        session = testSession(),
                        state = HomeUiState.Empty,
                        onRefresh = onRefresh,
                        restoreMediaId = null,
                        onOpenLibrary = {},
                        onOpenMedia = {},
                        onPlayHistory = {},
                    )
                }
            }
        }
    }

    private fun showContentHome(
        restoreMediaId: Long? = null,
        items: List<WatchHistoryItem> = historyItems(),
        onPlayHistory: (WatchHistoryItem) -> Unit = {},
        onBackdropChanged: (HomeBackdropPresentation?) -> Unit = {},
        viewportHeight: Dp = 440.dp,
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(viewportHeight)
                        .background(Background),
                ) {
                    HomeScreen(
                        session = testSession(),
                        state = HomeUiState.Content(items),
                        onRefresh = {},
                        restoreMediaId = restoreMediaId,
                        onOpenLibrary = {},
                        onOpenMedia = {},
                        onPlayHistory = onPlayHistory,
                        onBackdropChanged = onBackdropChanged,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun textLayoutFor(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(results)
            }
        return results.single()
    }

    private fun textLayoutForTag(tag: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(results)
            }
        return results.single()
    }

    private fun averagePosterLuminance(bitmap: Bitmap): Double {
        val density = composeRule.density
        val inset = with(density) { 4.dp.roundToPx() }
        val left = inset
        val right = bitmap.width - inset
        val top = inset
        val bottom = bitmap.height - top
        var total = 0L
        var count = 0L
        for (x in left until right) {
            for (y in top until bottom) {
                val pixel = bitmap.getPixel(x, y)
                total += AndroidColor.red(pixel)
                total += AndroidColor.green(pixel)
                total += AndroidColor.blue(pixel)
                count += 3
            }
        }
        return total.toDouble() / count
    }

    private fun averageTopCenterColor(bitmap: Bitmap): AverageColor {
        val density = composeRule.density
        val halfWidth = with(density) { 8.dp.roundToPx().coerceAtLeast(1) }
        val depth = with(density) { 2.dp.roundToPx().coerceAtLeast(1) }
        val centerX = bitmap.width / 2
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L
        for (x in centerX - halfWidth until centerX + halfWidth) {
            for (y in 0 until depth) {
                val pixel = bitmap.getPixel(x, y)
                red += AndroidColor.red(pixel)
                green += AndroidColor.green(pixel)
                blue += AndroidColor.blue(pixel)
                count += 1
            }
        }
        return AverageColor(
            red = red.toDouble() / count,
            green = green.toDouble() / count,
            blue = blue.toDouble() / count,
        )
    }

    private data class AverageColor(
        val red: Double,
        val green: Double,
        val blue: Double,
    ) {
        val luminance: Double
            get() = (red + green + blue) / 3.0

        val channelSpread: Double
            get() = maxOf(red, green, blue) - minOf(red, green, blue)
    }
}

private fun testSession() = Session(
    server = SavedServer("demo", "Demo", "https://demo.example"),
    token = "fixture",
    user = SessionUser(1, "viewer", "user"),
)

private fun historyItems() = listOf(
    historyItem(
        historyId = 401,
        mediaId = 301,
        title = "抵达",
        parentTitle = "星海纪行",
        posterPath = "/posters/stars.webp",
        backdropPath = "/backdrops/stars.webp",
        episode = 3,
    ),
    historyItem(
        historyId = 402,
        mediaId = 302,
        title = "微风",
        parentTitle = "森林来信",
        posterPath = "/posters/forest.webp",
        backdropPath = "/backdrops/forest.webp",
        episode = 4,
    ),
)

private fun historyItem(
    historyId: Long,
    mediaId: Long,
    title: String,
    parentTitle: String,
    posterPath: String,
    backdropPath: String,
    episode: Int,
) = WatchHistoryItem(
    historyId = historyId,
    mediaId = mediaId,
    title = title,
    fileName = "S01E0$episode.mkv",
    path = "/media/S01E0$episode.mkv",
    positionSeconds = 900,
    percentage = 45,
    year = 2026,
    season = 1,
    episode = episode,
    posterPath = posterPath,
    backdropPath = backdropPath,
    rating = 8.6,
    updatedAt = "2026-07-27T08:00:00Z",
    parentTitle = parentTitle,
)
