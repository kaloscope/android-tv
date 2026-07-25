package org.kaloscope.tv.feature.library

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deepViewportRestoresFocusedMediaFromSessionState() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = mediaItems(30),
                        focusedMediaId = 25,
                        gridViewport = GridViewportSnapshot(24, 0),
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("media-card-25").assertIsFocused()
    }

    @Test
    fun missingRestoreTargetFallsBackNearSavedViewport() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = mediaItems(20),
                        focusedMediaId = 999,
                        gridViewport = GridViewportSnapshot(18, 0),
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("media-card-19").assertIsFocused()
    }

    @Test
    fun prefetchZoneRequestsOneNextPage() {
        var loads = 0
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(media = mediaItems(20), hasNext = true),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = { loads += 1 },
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-results-grid").performScrollToIndex(19)
        composeRule.onNodeWithTag("media-card-20")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.runOnIdle {
            assertEquals(1, loads)
        }
    }

    @Test
    fun finalPageDoesNotPrefetchOrRenderPagingFooter() {
        var loads = 0
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(media = mediaItems(20), hasNext = false),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = { loads += 1 },
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-results-grid").performScrollToIndex(19)
        composeRule.onNodeWithTag("media-card-20")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("library-load-more-loading").assertDoesNotExist()
        composeRule.onNodeWithTag("library-load-more-retry").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, loads)
        }
    }

    @Test
    fun loadMoreFailureKeepsMediaAndOffersFocusableRetry() {
        var loads = 0
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = mediaItems(20),
                        hasNext = true,
                        loadMoreError = AppError.Offline,
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = { loads += 1 },
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-results-grid").performScrollToIndex(20)
        composeRule.onNodeWithTag("media-card-20").assertExists()
        composeRule.onNodeWithTag("library-load-more-retry")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, loads)
        }
    }
}

private fun state(
    media: List<MediaSummary> = mediaItems(1),
    focusedMediaId: Long? = null,
    gridViewport: GridViewportSnapshot = GridViewportSnapshot.Top,
    hasNext: Boolean = false,
    isLoadingMore: Boolean = false,
    loadMoreError: AppError? = null,
) = LibraryUiState.Content(
    libraries = listOf(MediaLibrary(21, "剧集库", MediaLibraryType.TvShow)),
    selectedLibraryId = 21,
    items = LibraryItemsState.Content(
        items = media,
        total = media.size,
        pageNumber = 1,
        hasNext = hasNext,
        isLoadingMore = isLoadingMore,
        loadMoreError = loadMoreError,
    ),
    focusedMediaId = focusedMediaId,
    gridViewport = gridViewport,
)

private fun mediaItems(count: Int) = (1..count).map { id ->
    MediaSummary(
        id = id.toLong(),
        title = "媒体$id",
        path = "/media/$id",
        posterPath = null,
        backdropPath = null,
        year = null,
        rating = null,
        season = null,
        episode = null,
    )
}

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
