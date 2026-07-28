package org.kaloscope.tv.app

import android.graphics.Color as AndroidColor
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation3.runtime.NavKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.navigation.HomeRoute
import org.kaloscope.tv.app.navigation.SearchRoute
import org.kaloscope.tv.app.navigation.SettingsRoute
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
import org.kaloscope.tv.feature.settings.SettingsConnection
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
    fun homeHistoryBackdropFillsAuthenticatedShellWithoutHero() {
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

        val shellBounds = composeRule.onNodeWithTag("kaloscope-background")
            .fetchSemanticsNode()
            .boundsInRoot
        val backdropBounds = composeRule.onNodeWithTag("home-fullscreen-backdrop")
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(shellBounds, backdropBounds)
        composeRule.onNodeWithTag("home-hero").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-backdrop-/backdrop.jpg").assertExists()
    }

    @Test
    fun rootRoutesUseTheSameTransparentTopBar() {
        var route by mutableStateOf<NavKey>(HomeRoute)
        composeRule.setContent {
            KaloscopeTheme {
                val homeFocus = remember { FocusRequester() }
                val searchFocus = remember { FocusRequester() }
                val libraryFocus = remember { FocusRequester() }
                val settingsFocus = remember { FocusRequester() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor.Magenta),
                ) {
                    MainTopBar(
                        currentRoute = route,
                        onHome = {},
                        onSearch = {},
                        onLibrary = {},
                        onSettings = {},
                        onDestinationFocused = {},
                        homeFocus = homeFocus,
                        searchFocus = searchFocus,
                        libraryFocus = libraryFocus,
                        settingsFocus = settingsFocus,
                    )
                }
            }
        }

        val homeBitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val sampleX = homeBitmap.width * 3 / 4
        val sampleY = 20
        val homePixel = homeBitmap.getPixel(sampleX, sampleY)

        composeRule.runOnIdle {
            route = SearchRoute
        }
        val searchBitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val searchPixel = searchBitmap.getPixel(sampleX, sampleY)

        assertEquals(
            "Root routes must use the same top-bar background",
            homePixel,
            searchPixel,
        )
    }

    @Test
    fun homeBackdropFadesTowardEveryEdge() {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeColor.Black),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .homeBackdropEdgeFade()
                        .background(ComposeColor.Magenta),
                )
            }
        }

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val edgeInset = 2
        val centerRed = AndroidColor.red(bitmap.getPixel(centerX, centerY))
        val leftRed = AndroidColor.red(bitmap.getPixel(edgeInset, centerY))
        val rightRed = AndroidColor.red(
            bitmap.getPixel(bitmap.width - edgeInset - 1, centerY),
        )
        val topRed = AndroidColor.red(bitmap.getPixel(centerX, edgeInset))
        val bottomRed = AndroidColor.red(
            bitmap.getPixel(centerX, bitmap.height - edgeInset - 1),
        )

        assertTrue(centerRed > leftRed + 100)
        assertTrue(centerRed > rightRed + 100)
        assertTrue(centerRed > topRed + 100)
        assertTrue(centerRed > bottomRed + 100)
    }

    @Test
    fun movingCarouselFocusUpdatesFullscreenBackdrop() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Content(
                        listOf(
                            history(),
                            history(
                                historyId = 2,
                                mediaId = 202,
                                title = "森林来信",
                                posterPath = "/forest-poster.jpg",
                                backdropPath = "/forest-backdrop.jpg",
                            ),
                        ),
                    ),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithTag("home-fullscreen-backdrop").assertExists()
        composeRule.onNodeWithTag("history-card-201")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("history-card-202").assertIsFocused()
        composeRule.onNodeWithTag("detail-backdrop-/forest-backdrop.jpg").assertExists()
    }

    @Test
    fun emptyHomeClearsFullscreenBackdrop() {
        var homeState by mutableStateOf<HomeUiState>(
            HomeUiState.Content(listOf(history())),
        )
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = homeState,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithTag("home-fullscreen-backdrop").assertExists()

        composeRule.runOnIdle {
            homeState = HomeUiState.Empty
        }

        composeRule.onNodeWithTag("home-fullscreen-backdrop").assertDoesNotExist()
    }

    @Test
    fun settingsGearOpensOnFocusAndKeepsFocus() {
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

        composeRule.onNodeWithText("家庭服务器").assertExists()
        composeRule.onNodeWithText("tv_user").assertExists()
        composeRule.onNodeWithContentDescription("设置")
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun openingSettingChoiceKeepsSettingsRouteAndFocusesDialog() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = SettingsRoute,
                )
            }
        }

        composeRule.onNodeWithText("默认播放模式")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithContentDescription("设置").assertIsSelected()
        composeRule.onNodeWithText("首页").assertIsNotSelected()
        composeRule.onNodeWithText("自动")
            .assertIsSelected()
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
        composeRule.onNode(hasText("默认播放模式") and hasClickAction()).assertIsFocused()
        composeRule.onNodeWithContentDescription("设置").assertIsSelected()
    }

    @Test
    fun savingToggleKeepsSettingsFocusAndIgnoresRepeatCenter() {
        var settingsState by mutableStateOf(
            SettingsUiState.Content(TvSettings()),
        )
        var saves = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    settingsState = settingsState,
                    initialRoute = SettingsRoute,
                    settingsActions = SettingsActions(
                        setAutoplayNext = {
                            saves += 1
                            settingsState = settingsState.copy(isSaving = true)
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithText("自动播放下一集")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithContentDescription("设置").assertIsSelected()
        composeRule.onNodeWithText("首页").assertIsNotSelected()
        composeRule.onNodeWithText("自动播放下一集")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(1, saves)
        }
    }

    @Test
    fun testingConnectionKeepsSettingsFocusAndIgnoresRepeatCenter() {
        var settingsState by mutableStateOf(
            SettingsUiState.Content(
                settings = TvSettings(),
                section = SettingsSection.ServerAccount,
            ),
        )
        var tests = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    settingsState = settingsState,
                    initialRoute = SettingsRoute,
                    settingsActions = SettingsActions(
                        testConnection = {
                            tests += 1
                            settingsState = settingsState.copy(
                                connection = SettingsConnection.Testing,
                            )
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithText("测试连接")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithContentDescription("设置").assertIsSelected()
        composeRule.onNodeWithText("首页").assertIsNotSelected()
        composeRule.onNodeWithText("测试连接")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle {
            assertEquals(1, tests)
        }
    }

    @Test
    fun focusingSearchSelectsRouteWithoutCenterAndKeepsFocus() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = deepSearchState().copy(focusedResultId = null),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithText("首页").assertIsSelected()
        composeRule.onNodeWithText("网络搜索")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .assertIsSelected()
        composeRule.onNodeWithTag("search-content").assertExists()
        composeRule.onNodeWithText("首页")
            .assertIsNotSelected()
            .assertIsNotFocused()
    }

    @Test
    fun recomposingFocusedSearchDoesNotOpenAgain() {
        var homeState by mutableStateOf<HomeUiState>(HomeUiState.Empty)
        var searchOpens = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = homeState,
                    searchState = deepSearchState().copy(focusedResultId = null),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    searchActions = SearchActions(open = { searchOpens += 1 }),
                )
            }
        }

        composeRule.onNodeWithText("网络搜索")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.runOnIdle {
            assertEquals(1, searchOpens)
            homeState = HomeUiState.Loading
        }
        composeRule.runOnIdle {
            assertEquals(1, searchOpens)
        }
    }

    @Test
    fun movingAcrossTopNavigationActivatesEachFocusedRoute() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = deepSearchState().copy(focusedResultId = null),
                    libraryState = deepLibraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithText("首页")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithText("网络搜索")
            .assertIsFocused()
            .assertIsSelected()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithText("媒体库")
            .assertIsFocused()
            .assertIsSelected()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithContentDescription("设置")
            .assertIsFocused()
            .assertIsSelected()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNodeWithText("媒体库")
            .assertIsFocused()
            .assertIsSelected()
    }

    @Test
    fun directionDownEntersTheActiveSearchContent() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = deepSearchState().copy(focusedResultId = null),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithText("网络搜索")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithText("网络搜索").assertIsNotFocused()
        composeRule.onNodeWithTag("network-search-input").assertIsFocused()
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
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithTag("library-search-input").assertIsFocused()
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
    fun topLevelRoundTripKeepsLibraryFocusAndDeepViewport() {
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
            .assertIsFocused()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("media-card-25"))
                .fetchSemanticsNodes().size == 1
        }

        composeRule.onNode(hasText("首页") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.onNodeWithText("媒体库").assertIsFocused()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("media-card-25"))
                .fetchSemanticsNodes().size == 1
        }
    }

    @Test
    fun topLevelRoundTripKeepsSearchFocusAndDeepViewport() {
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
            .assertIsFocused()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("network-result-v25"))
                .fetchSemanticsNodes().size == 1
        }

        composeRule.onNode(hasText("首页") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNode(hasText("网络搜索") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.onNodeWithText("网络搜索").assertIsFocused()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("network-result-v25"))
                .fetchSemanticsNodes().size == 1
        }
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
    initialRoute: NavKey = HomeRoute,
    searchActions: SearchActions = SearchActions(),
    libraryActions: LibraryActions = LibraryActions(),
    settingsActions: SettingsActions = SettingsActions(),
) {
    MainShell(
        session = session,
        homeState = homeState,
        searchState = searchState,
        libraryState = libraryState,
        detailState = detailState,
        homeActions = HomeActions(),
        searchActions = searchActions,
        libraryActions = libraryActions,
        detailActions = DetailActions(),
        settingsState = settingsState,
        initialRoute = initialRoute,
        settingsActions = settingsActions,
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

private fun history(
    historyId: Long = 1,
    mediaId: Long = 201,
    title: String = "群星档案",
    posterPath: String = "/poster.jpg",
    backdropPath: String = "/backdrop.jpg",
) = WatchHistoryItem(
    historyId = historyId,
    mediaId = mediaId,
    title = title,
    fileName = "episode-1.mkv",
    path = "/media/episode-1.mkv",
    positionSeconds = 1_200,
    percentage = 42,
    year = 2026,
    season = 1,
    episode = 1,
    posterPath = posterPath,
    backdropPath = backdropPath,
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
