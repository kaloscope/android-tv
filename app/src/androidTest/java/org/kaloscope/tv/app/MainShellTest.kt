package org.kaloscope.tv.app

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.model.MediaActor
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryItemsState
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.settings.SettingsSection
import org.kaloscope.tv.feature.settings.SettingsUiState

class MainShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeNavigationReceivesInitialFocus() {
        composeRule.setContent {
            KaloscopeTheme {
                MainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    onRefresh = {},
                    onOpenLibrary = {},
                    onSelectLibrary = {},
                    onLibraryQueryChange = {},
                    onSearchLibrary = {},
                    onRetryLibrary = {},
                    onLoadMoreMedia = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                    onRetryDetail = {},
                    onSelectMediaChild = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(hasText("首页") and hasClickAction()).assertIsFocused()
    }

    @Test
    fun homeHistoryUsesBrandedBackgroundAndCinematicHero() {
        composeRule.setContent {
            KaloscopeTheme {
                MainShell(
                    session = session(),
                    homeState = HomeUiState.Content(listOf(history())),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    onRefresh = {},
                    onOpenLibrary = {},
                    onSelectLibrary = {},
                    onLibraryQueryChange = {},
                    onSearchLibrary = {},
                    onRetryLibrary = {},
                    onLoadMoreMedia = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                    onRetryDetail = {},
                    onSelectMediaChild = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithTag("kaloscope-background").assertExists()
        composeRule.onNodeWithTag("home-hero").assertExists()
    }

    @Test
    fun settingsGearStaysSelectedAndShowsCurrentAccount() {
        composeRule.setContent {
            KaloscopeTheme {
                MainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    settingsState = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.ServerAccount,
                    ),
                    onRefresh = {},
                    onOpenLibrary = {},
                    onSelectLibrary = {},
                    onLibraryQueryChange = {},
                    onSearchLibrary = {},
                    onRetryLibrary = {},
                    onLoadMoreMedia = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                    onRetryDetail = {},
                    onSelectMediaChild = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(hasText("首页") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionRight)
            }

        composeRule.onNodeWithContentDescription("设置")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithText("家庭服务器").assertExists()
        composeRule.onNodeWithText("tv_user").assertExists()
        composeRule.onNodeWithContentDescription("设置").assertIsSelected()
    }

    @Test
    fun searchAndLibraryAreEnabled() {
        composeRule.setContent {
            KaloscopeTheme {
                MainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    onRefresh = {},
                    onOpenLibrary = {},
                    onSelectLibrary = {},
                    onLibraryQueryChange = {},
                    onSearchLibrary = {},
                    onRetryLibrary = {},
                    onLoadMoreMedia = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                    onRetryDetail = {},
                    onSelectMediaChild = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("网络搜索").assertIsEnabled()
        composeRule.onNodeWithText("媒体库").assertIsEnabled()
    }

    @Test
    fun mediaCardOpensDetailAndBackRestoresCardFocus() {
        composeRule.setContent {
            KaloscopeTheme {
                MainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    onRefresh = {},
                    onOpenLibrary = {},
                    onSelectLibrary = {},
                    onLibraryQueryChange = {},
                    onSearchLibrary = {},
                    onRetryLibrary = {},
                    onLoadMoreMedia = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                    onRetryDetail = {},
                    onSelectMediaChild = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithText("剧集库").assertIsFocused()
        composeRule.onNodeWithTag("media-card-201")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithText("来自服务器的简介").assertExists()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("media-card-201") and isFocused(),
            ).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag("media-card-201").assertIsFocused()
    }
}

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)

private fun libraryState() = LibraryUiState.Content(
    libraries = listOf(MediaLibrary(21, "剧集库", MediaLibraryType.TvShow)),
    selectedLibraryId = 21,
    items = LibraryItemsState.Content(
        items = listOf(summary()),
        total = 1,
        pageNumber = 1,
        hasNext = false,
    ),
    focusedMediaId = 201,
)

private fun summary() = MediaSummary(
    id = 201,
    title = "群星档案",
    path = "/media/series",
    posterPath = null,
    backdropPath = null,
    year = 2026,
    rating = 8.8,
    season = null,
    episode = null,
)

private fun history() = WatchHistoryItem(
    historyId = 1,
    mediaId = 201,
    title = "群星档案",
    fileName = "episode-1.mkv",
    path = "/media/episode-1.mkv",
    positionSeconds = 1_200,
    percentage = 42,
    year = 2026,
    season = 1,
    episode = 1,
    posterPath = "/poster.jpg",
    backdropPath = "/backdrop.jpg",
    rating = 8.8,
    updatedAt = "2026-07-25T00:00:00Z",
)

private fun detail() = MediaDetail(
    id = 201,
    library = MediaLibrary(21, "剧集库", MediaLibraryType.TvShow),
    title = "群星档案",
    path = "/media/series",
    posterPath = null,
    backdropPath = null,
    year = 2026,
    rating = 8.8,
    season = null,
    episode = null,
    aired = null,
    plot = "来自服务器的简介",
    genres = listOf("剧情", "科幻"),
    directors = listOf("林屿"),
    writers = emptyList(),
    studios = emptyList(),
    actors = listOf(MediaActor("沈川", "队长", null)),
    children = listOf(
        MediaSummary(
            id = 301,
            title = "启程",
            path = "/media/episode-1",
            posterPath = null,
            backdropPath = null,
            year = 2026,
            rating = 8.5,
            season = 1,
            episode = 1,
        ),
    ),
)
