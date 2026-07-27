package org.kaloscope.tv.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
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
    fun historyCarouselMovesRightAndUpdatesSelectedBackdrop() {
        showContentHome()

        composeRule.onAllNodesWithText("最近观看").assertCountEquals(1)
        composeRule.onNodeWithTag("history-selected-title").assertTextEquals("星海纪行")
        composeRule.onNodeWithTag("history-card-301")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("history-card-302").assertIsFocused()
        composeRule.onNodeWithTag("history-selected-title").assertTextEquals("森林来信")
        composeRule.onNodeWithTag("detail-backdrop-/backdrops/forest.webp").assertExists()
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

    private fun showEmptyHome(onRefresh: () -> Unit = {}) {
        composeRule.setContent {
            KaloscopeTheme {
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

    private fun showContentHome(
        restoreMediaId: Long? = null,
        items: List<WatchHistoryItem> = historyItems(),
        onPlayHistory: (WatchHistoryItem) -> Unit = {},
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp),
                ) {
                    HomeScreen(
                        session = testSession(),
                        state = HomeUiState.Content(items),
                        onRefresh = {},
                        restoreMediaId = restoreMediaId,
                        onOpenLibrary = {},
                        onOpenMedia = {},
                        onPlayHistory = onPlayHistory,
                    )
                }
            }
        }
        composeRule.waitForIdle()
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
