package org.kaloscope.tv.app

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.Composable
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
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
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
import org.kaloscope.tv.feature.search.SearchResultsState
import org.kaloscope.tv.feature.search.SearchUiState
import org.kaloscope.tv.feature.settings.SettingsSection
import org.kaloscope.tv.feature.settings.SettingsUiState

class MainShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeNavigationReceivesInitialFocus() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNode(hasText("首页") and hasClickAction()).assertIsFocused()
    }

    @Test
    fun homeHistoryUsesBrandedBackgroundAndCinematicHero() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Content(listOf(history())),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
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
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    settingsState = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.ServerAccount,
                    ),
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
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithText("网络搜索").assertIsEnabled()
        composeRule.onNodeWithText("媒体库").assertIsEnabled()
    }

    @Test
    fun mediaCardOpensDetailAndBackRestoresCardFocus() {
        val libraryItems = listOf(
            summary(),
            summary().copy(
                id = 202,
                title = "下一项",
                path = "/media/next.mkv",
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(items = libraryItems),
                    detailState = MediaDetailUiState.Content(detail()),
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
        composeRule.onNodeWithTag("media-card-201")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("media-card-202").assertIsFocused()
    }

    @Test
    fun topLevelRoundTripRestoresDeepLibraryCard() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = deepLibraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("media-card-25").assertIsFocused()

        composeRule.onNode(hasText("首页") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("media-card-25").assertIsFocused()
    }

    @Test
    fun topLevelRoundTripRestoresDeepSearchResult() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = deepSearchState(),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNode(hasText("网络搜索") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("network-result-v25").assertIsFocused()

        composeRule.onNode(hasText("首页") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNode(hasText("网络搜索") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("network-result-v25").assertIsFocused()
    }
}

@Composable
private fun TestMainShell(
    session: Session,
    homeState: HomeUiState,
    searchState: SearchUiState = SearchUiState.Loading,
    libraryState: LibraryUiState,
    detailState: MediaDetailUiState,
    settingsState: SettingsUiState = SettingsUiState.Content(TvSettings()),
) {
    MainShell(
        session = session,
        homeState = homeState,
        searchState = searchState,
        libraryState = libraryState,
        detailState = detailState,
        homeActions = HomeActions(),
        searchActions = SearchActions(),
        libraryActions = LibraryActions(),
        detailActions = DetailActions(),
        settingsState = settingsState,
        settingsActions = SettingsActions(),
        playerActions = PlayerActions(),
    )
}

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)

private fun libraryState(
    items: List<MediaSummary> = listOf(summary()),
) = LibraryUiState.Content(
    libraries = listOf(MediaLibrary(21, "剧集库", MediaLibraryType.TvShow)),
    selectedLibraryId = 21,
    items = LibraryItemsState.Content(
        items = items,
        total = items.size,
        pageNumber = 1,
        hasNext = false,
    ),
    focusedMediaId = null,
)

private fun deepLibraryState() = LibraryUiState.Content(
    libraries = listOf(MediaLibrary(21, "剧集库", MediaLibraryType.TvShow)),
    selectedLibraryId = 21,
    items = LibraryItemsState.Content(
        items = (1..30).map { id ->
            summary().copy(
                id = id.toLong(),
                title = "媒体$id",
                path = "/media/$id",
            )
        },
        total = 30,
        pageNumber = 2,
        hasNext = false,
    ),
    focusedMediaId = 25,
    gridViewport = GridViewportSnapshot(24, 0),
)

private fun deepSearchState() = SearchUiState.Content(
    profiles = listOf(
        IndexerSourceProfile(
            indexer = NetworkIndexer(11, "星海站", null),
            pageSize = 20,
            keywordRequired = true,
        ),
    ),
    selectedIndexerId = 11,
    query = "星际",
    submittedKeyword = "星际",
    results = SearchResultsState.Content(
        items = (1..30).map { id ->
            NetworkSearchResult(
                id = "v$id",
                title = "视频$id",
                coverPath = null,
                rating = null,
                category = null,
                uploader = null,
                uploadedAt = null,
            )
        },
        total = 30,
        pageNumber = 2,
        hasNext = false,
    ),
    focusedResultId = "v25",
    gridViewport = GridViewportSnapshot(24, 0),
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
