package org.kaloscope.tv.app

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
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
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.navigation.HomeRoute
import org.kaloscope.tv.app.navigation.LibraryRoute
import org.kaloscope.tv.app.navigation.SearchRoute
import org.kaloscope.tv.app.navigation.SettingsRoute
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.KaloscopeMotion
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.SearchFilterDefinition
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.model.MediaActor
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryItemsState
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.player.PlayerUiState
import org.kaloscope.tv.feature.reader.ReaderUiState
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
    fun directionUpThroughHomeContentFocusesActiveHomeNavigation() {
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

        composeRule.onNodeWithTag("history-card-201")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithText("继续播放")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithTag("home-refresh")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNode(hasText("首页") and hasClickAction())
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun directionUpFromEmptyHomeMovesThroughRefreshToNavigation() {
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

        composeRule.onNodeWithText("进入媒体库")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithTag("home-refresh")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNode(hasText("首页") and hasClickAction())
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun emptyHomeSearchShortcutOpensNetworkSearch() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = SearchUiState.EmptyIndexers,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithTag("home-open-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("home-open-search"))
                .fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNode(hasText("网络搜索") and hasClickAction())
            .assertIsSelected()
        composeRule.onNode(hasText("首页") and hasClickAction())
            .assertIsNotSelected()
        composeRule.onNodeWithText("当前服务器没有可用的网络搜索数据源。")
            .assertExists()
        composeRule.onNodeWithTag("refresh-indexers").assertIsFocused()
    }

    @Test
    fun directionUpFromHomeErrorMovesThroughRefreshToNavigation() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Error(AppError.Offline),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithText("重试")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithTag("home-refresh")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNode(hasText("首页") and hasClickAction())
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun homeNavigationKeepsFocusAtLeftBoundary() {
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

        composeRule.onNode(hasText("首页") and hasClickAction())
            .assertIsSelected()
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun settingsNavigationKeepsFocusAtRightBoundary() {
        composeRule.setContent {
            KaloscopeTheme {
                val homeFocus = remember { FocusRequester() }
                val searchFocus = remember { FocusRequester() }
                val libraryFocus = remember { FocusRequester() }
                val settingsFocus = remember { FocusRequester() }
                val searchMenuFocus = remember { FocusRequester() }
                val libraryMenuFocus = remember { FocusRequester() }
                val settingsMenuFocus = remember { FocusRequester() }
                Box(modifier = Modifier.fillMaxSize()) {
                    MainTopBar(
                        currentRoute = SettingsRoute,
                        onHome = {},
                        onSearch = {},
                        onLibrary = {},
                        onSettings = {},
                        onDestinationFocused = {},
                        homeFocus = homeFocus,
                        searchFocus = searchFocus,
                        libraryFocus = libraryFocus,
                        settingsFocus = settingsFocus,
                        searchMenuFocus = searchMenuFocus,
                        libraryMenuFocus = libraryMenuFocus,
                        settingsMenuFocus = settingsMenuFocus,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = 18.dp)
                            .size(40.dp)
                            .focusable()
                            .testTag("right-boundary-decoy"),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("设置")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsSelected()
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
            .assertIsSelected()
            .assertIsFocused()
        composeRule.onNodeWithTag("right-boundary-decoy").assertIsNotFocused()
    }

    @Test
    fun topNavigationRejectsRepeatedUpAtUpperBoundary() {
        composeRule.setContent {
            KaloscopeTheme {
                val homeFocus = remember { FocusRequester() }
                val searchFocus = remember { FocusRequester() }
                val libraryFocus = remember { FocusRequester() }
                val settingsFocus = remember { FocusRequester() }
                val searchMenuFocus = remember { FocusRequester() }
                val libraryMenuFocus = remember { FocusRequester() }
                val settingsMenuFocus = remember { FocusRequester() }
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .focusable()
                            .testTag("upper-boundary-decoy"),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = 80.dp),
                    ) {
                        MainTopBar(
                            currentRoute = SearchRoute,
                            onHome = {},
                            onSearch = {},
                            onLibrary = {},
                            onSettings = {},
                            onDestinationFocused = {},
                            homeFocus = homeFocus,
                            searchFocus = searchFocus,
                            libraryFocus = libraryFocus,
                            settingsFocus = settingsFocus,
                            searchMenuFocus = searchMenuFocus,
                            libraryMenuFocus = libraryMenuFocus,
                            settingsMenuFocus = settingsMenuFocus,
                        )
                    }
                }
            }
        }

        listOf(
            composeRule.onNodeWithText("首页"),
            composeRule.onNodeWithTag("main-nav-search"),
            composeRule.onNodeWithText("媒体库"),
            composeRule.onNodeWithContentDescription("设置"),
        ).forEach { navigation ->
            navigation
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsFocused()
                .performKeyInput {
                    repeat(8) { pressKey(Key.DirectionUp) }
                }
                .assertIsFocused()
            composeRule.onNodeWithTag("upper-boundary-decoy")
                .assertIsNotFocused()
        }
    }

    @Test
    fun topNavigationUsesLocalVectorIcons() {
        var route by mutableStateOf<NavKey>(HomeRoute)
        composeRule.setContent {
            KaloscopeTheme {
                val homeFocus = remember { FocusRequester() }
                val searchFocus = remember { FocusRequester() }
                val libraryFocus = remember { FocusRequester() }
                val settingsFocus = remember { FocusRequester() }
                val searchMenuFocus = remember { FocusRequester() }
                val libraryMenuFocus = remember { FocusRequester() }
                val settingsMenuFocus = remember { FocusRequester() }
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
                    searchMenuFocus = searchMenuFocus,
                    libraryMenuFocus = libraryMenuFocus,
                    settingsMenuFocus = settingsMenuFocus,
                )
            }
        }

        composeRule.onNodeWithTag(
            "main-nav-icon-home-filled",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            "main-nav-icon-search-regular",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            "main-nav-icon-library-regular",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            "main-nav-icon-settings-regular",
            useUnmergedTree = true,
        ).assertExists()

        composeRule.runOnIdle { route = SettingsRoute }

        composeRule.onNodeWithTag(
            "main-nav-icon-home-regular",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            "main-nav-icon-settings-filled",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithContentDescription("设置").assertExists()
    }

    @Test
    fun topNavigationIconIsStillCrossfadingHalfwayThroughFocusMotion() {
        var route by mutableStateOf<NavKey>(HomeRoute)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                val homeFocus = remember { FocusRequester() }
                val searchFocus = remember { FocusRequester() }
                val libraryFocus = remember { FocusRequester() }
                val settingsFocus = remember { FocusRequester() }
                val searchMenuFocus = remember { FocusRequester() }
                val libraryMenuFocus = remember { FocusRequester() }
                val settingsMenuFocus = remember { FocusRequester() }
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
                    searchMenuFocus = searchMenuFocus,
                    libraryMenuFocus = libraryMenuFocus,
                    settingsMenuFocus = settingsMenuFocus,
                )
            }
        }

        val start = composeRule.onNodeWithTag(
            "main-nav-icon-search-regular",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()

        composeRule.runOnIdle { route = SearchRoute }
        composeRule.mainClock.advanceTimeBy(
            KaloscopeMotion.FocusMillis.toLong() / 2,
        )
        val inProgress = composeRule.onNodeWithTag(
            "main-nav-icon-search-filled",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()

        composeRule.mainClock.advanceTimeBy(
            KaloscopeMotion.FocusMillis.toLong() / 2 + 20,
        )
        val settled = composeRule.onNodeWithTag(
            "main-nav-icon-search-filled",
            useUnmergedTree = true,
        ).captureToImage().asAndroidBitmap()

        val foreground = OnBackground.toArgb()
        var selectedOnlyPixels = 0
        var unfinishedPixels = 0
        settled.forEachPixel { x, y, settledPixel ->
            val startPixel = start.getPixel(x, y)
            if (
                settledPixel.isNearColor(foreground, tolerance = 8) &&
                !startPixel.isNearColor(foreground, tolerance = 24)
            ) {
                selectedOnlyPixels += 1
                if (
                    !inProgress.getPixel(x, y)
                        .isNearColor(settledPixel, tolerance = 8)
                ) {
                    unfinishedPixels += 1
                }
            }
        }

        assertTrue(
            "The fixture did not isolate enough selected-icon pixels",
            selectedOnlyPixels >= 12,
        )
        assertTrue(
            "The icon switched before the focus motion completed: " +
                "$unfinishedPixels/$selectedOnlyPixels pixels still animating",
            unfinishedPixels >= selectedOnlyPixels / 3,
        )
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
                val searchMenuFocus = remember { FocusRequester() }
                val libraryMenuFocus = remember { FocusRequester() }
                val settingsMenuFocus = remember { FocusRequester() }
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
                        searchMenuFocus = searchMenuFocus,
                        libraryMenuFocus = libraryMenuFocus,
                        settingsMenuFocus = settingsMenuFocus,
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
    fun rootBackdropFadesTowardEveryEdge() {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeColor.Black),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rootBackdropEdgeFade()
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
    fun libraryBackdropFillsAuthenticatedShell() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(
                        items = listOf(
                            summary().copy(backdropPath = "/library-backdrop.jpg"),
                        ),
                    ),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = LibraryRoute,
                )
            }
        }

        val shellBounds = composeRule.onNodeWithTag("kaloscope-background")
            .fetchSemanticsNode()
            .boundsInRoot
        val backdropBounds = composeRule.onNodeWithTag("library-fullscreen-backdrop")
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(shellBounds, backdropBounds)
        composeRule.onNodeWithTag("home-fullscreen-backdrop").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-backdrop-/library-backdrop.jpg").assertExists()
    }

    @Test
    fun movingLibraryFocusUpdatesFullscreenBackdrop() {
        val first = summary().copy(backdropPath = "/library-first.jpg")
        val second = summary().copy(
            id = 202,
            title = "下一项",
            path = "/media/next",
            backdropPath = "/library-second.jpg",
        )
        var currentState by mutableStateOf(libraryState(items = listOf(first, second)))
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = currentState,
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = LibraryRoute,
                    libraryActions = LibraryActions(
                        rememberFocusedMedia = { mediaId ->
                            currentState = currentState.copy(focusedMediaId = mediaId)
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("detail-backdrop-/library-first.jpg").assertExists()
        composeRule.onNodeWithTag("media-card-201")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("media-card-202").assertIsFocused()
        composeRule.onNodeWithTag("detail-backdrop-/library-second.jpg").assertExists()
    }

    @Test
    fun libraryLoadingRetainsLastValidBackdrop() {
        var currentState by mutableStateOf<LibraryUiState>(
            libraryState(
                items = listOf(
                    summary().copy(backdropPath = "/retained-library.jpg"),
                ),
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = currentState,
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = LibraryRoute,
                )
            }
        }

        composeRule.onNodeWithTag("detail-backdrop-/retained-library.jpg").assertExists()
        composeRule.runOnIdle {
            currentState = libraryState().copy(items = LibraryItemsState.Loading)
        }

        composeRule.onNodeWithTag("library-fullscreen-backdrop").assertExists()
        composeRule.onNodeWithTag("detail-backdrop-/retained-library.jpg").assertExists()
    }

    @Test
    fun libraryBackdropIsScopedToLibraryRoute() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(
                        items = listOf(
                            summary().copy(backdropPath = "/scoped-library.jpg"),
                        ),
                    ),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = LibraryRoute,
                )
            }
        }

        composeRule.onNodeWithTag("library-fullscreen-backdrop").assertExists()
        composeRule.onNode(hasText("首页") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.onNodeWithText("首页").assertIsSelected()
        composeRule.onNodeWithTag("library-fullscreen-backdrop").assertDoesNotExist()
    }

    @Test
    fun serverChangeClearsRetainedLibraryBackdrop() {
        var currentSession by mutableStateOf(session(serverId = "server-one"))
        var currentState by mutableStateOf<LibraryUiState>(
            libraryState(
                items = listOf(
                    summary().copy(backdropPath = "/server-one-library.jpg"),
                ),
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = currentSession,
                    homeState = HomeUiState.Empty,
                    libraryState = currentState,
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = LibraryRoute,
                )
            }
        }

        composeRule.onNodeWithTag("library-fullscreen-backdrop").assertExists()
        composeRule.runOnIdle {
            currentSession = session(serverId = "server-two")
            currentState = libraryState().copy(items = LibraryItemsState.Loading)
        }

        composeRule.onNodeWithTag("library-fullscreen-backdrop").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-backdrop-/server-one-library.jpg")
            .assertDoesNotExist()
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
    fun directionDownFromSettingsGearFocusesSelectedMenuSection() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    settingsState = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Danmaku,
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("设置")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNode(hasClickAction() and hasText("弹幕设置")).assertIsFocused()
        composeRule.onNodeWithText("默认开启弹幕").assertIsNotFocused()
    }

    @Test
    fun directionUpFromSettingsMenuFocusesActiveNavigation() {
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

        composeRule.onNode(hasClickAction() and hasText("播放设置"))
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNodeWithContentDescription("设置")
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun directionUpFromSettingsPanelFocusesActiveNavigation() {
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

        composeRule.onNode(hasText("默认播放模式") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

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
        composeRule.onNodeWithTag("playback-mode-option-auto")
            .assertIsSelected()
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
        composeRule.onNode(hasText("默认播放模式") and hasClickAction()).assertIsFocused()
        composeRule.onNodeWithContentDescription("设置").assertIsSelected()
    }

    @Test
    fun rapidSettingToggleKeepsFocusAndAcceptsLatestValue() {
        var settingsState by mutableStateOf(
            SettingsUiState.Content(TvSettings()),
        )
        val requestedValues = mutableListOf<Boolean>()
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
                        setAutoplayNext = { value ->
                            requestedValues += value
                            settingsState = settingsState.copy(
                                settings = settingsState.settings.copy(
                                    autoplayNext = value,
                                ),
                                isSaving = true,
                            )
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
            assertEquals(listOf(false, true), requestedValues)
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
        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .assertIsSelected()
        composeRule.onNodeWithTag("search-content").assertExists()
        composeRule.onNodeWithText("首页")
            .assertIsNotSelected()
            .assertIsNotFocused()
    }

    @Test
    fun rootDestinationsShareCompactContentTop() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Content(listOf(history())),
                    searchState = deepSearchState().copy(focusedResultId = null),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val expectedTop = 84f * density
        val expectedHomeControlTop = 86f * density

        assertEquals(
            expectedHomeControlTop,
            composeRule.onNodeWithTag("home-refresh")
                .fetchSemanticsNode()
                .boundsInRoot.top,
            1f,
        )

        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        assertEquals(
            expectedTop,
            composeRule.onNodeWithTag("search-content")
                .fetchSemanticsNode()
                .boundsInRoot.top,
            1f,
        )

        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
        assertEquals(
            expectedTop,
            composeRule.onNodeWithTag("library-content")
                .fetchSemanticsNode()
                .boundsInRoot.top,
            1f,
        )

        composeRule.onNodeWithContentDescription("设置")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        assertEquals(
            expectedTop,
            composeRule.onNodeWithTag("settings-panel")
                .fetchSemanticsNode()
                .boundsInRoot.top,
            1f,
        )
    }

    @Test
    fun browseAndSettingsPanesShareLeadingEdge() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = deepSearchState().copy(focusedResultId = null),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = SearchRoute,
                )
            }
        }

        val searchLeft = composeRule.onNodeWithTag("search-content")
            .fetchSemanticsNode()
            .boundsInRoot.left

        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
        val libraryLeft = composeRule.onNodeWithTag("library-content")
            .fetchSemanticsNode()
            .boundsInRoot.left

        composeRule.onNodeWithContentDescription("设置")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        val settingsLeft = composeRule.onNodeWithTag("settings-panel")
            .fetchSemanticsNode()
            .boundsInRoot.left

        assertEquals(searchLeft, libraryLeft, 1f)
        assertEquals(searchLeft, settingsLeft, 1f)
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

        composeRule.onNodeWithTag("main-nav-search")
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
    fun backCancelsSearchResolutionBeforeLeavingSearch() {
        var searchState by mutableStateOf(
            deepSearchState().copy(resolvingResultId = "v25"),
        )
        var cancellations = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = searchState,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = SearchRoute,
                    searchActions = SearchActions(
                        cancelResolution = {
                            if (searchState.resolvingResultId == null) {
                                false
                            } else {
                                cancellations += 1
                                searchState = searchState.copy(resolvingResultId = null)
                                true
                            }
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("search-playback-loading").assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(1, cancellations) }
        composeRule.onNodeWithText("网络搜索").assertIsSelected()
        composeRule.onNodeWithText("首页").assertIsNotSelected()

        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(1, cancellations) }
        composeRule.onNodeWithText("首页").assertIsSelected()
    }

    @Test
    fun searchResolutionReplacesTheShellWithBlockingFullscreenLoading() {
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = deepSearchState().copy(resolvingResultId = "v25"),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = SearchRoute,
                )
            }
        }

        val loading = composeRule.onNodeWithTag("search-playback-loading")
            .assertIsFocused()
        composeRule.onNodeWithTag("search-playback-loading-indicator").assertExists()
        composeRule.onNodeWithText("正在获取资源…").assertExists()
        composeRule.onNodeWithTag("search-results-grid").assertDoesNotExist()
        composeRule.onNodeWithTag("main-nav-search").assertDoesNotExist()

        loading.performKeyInput {
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionUp)
            pressKey(Key.DirectionDown)
        }

        loading.assertIsFocused()
    }

    @Test
    fun failedSearchResolutionRestoresCardFocusAndShowsTheExistingError() {
        var searchState by mutableStateOf(
            deepSearchState().copy(focusedResultId = null),
        )
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = searchState,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = SearchRoute,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("network-result-v25"))
                .fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag("network-result-v25")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()

        composeRule.runOnIdle {
            searchState = searchState.copy(
                focusedResultId = "v25",
                resolvingResultId = "v25",
            )
        }
        composeRule.onNodeWithTag("search-playback-loading").assertIsFocused()

        composeRule.runOnIdle {
            searchState = searchState.copy(
                resolvingResultId = null,
                playbackError = AppError.Offline,
            )
        }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("network-result-v25"))
                .fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag("network-result-v25").assertIsFocused()
        composeRule.onNodeWithText("无法打开资源", substring = true).assertExists()
    }

    @Test
    fun backClosesSearchFiltersWithoutLeavingSearchOrApplying() {
        val baseSearchState = deepSearchState()
        var searchState by mutableStateOf(
            baseSearchState.copy(
                profiles = baseSearchState.profiles.map { profile ->
                    profile.copy(
                        filters = listOf(
                            SearchFilterDefinition(
                                key = "title",
                                label = "标题",
                                type = SearchFilterType.Text,
                            ),
                        ),
                    )
                },
            ),
        )
        var dismissals = 0
        var applications = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = searchState,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = SearchRoute,
                    searchActions = SearchActions(
                        openFilters = {
                            searchState = searchState.copy(filterDrawerOpen = true)
                        },
                        dismissFilters = {
                            dismissals += 1
                            searchState = searchState.copy(filterDrawerOpen = false)
                        },
                        applyFilters = { applications += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("search-filter-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("search-filter-drawer").assertExists()
        composeRule.onNodeWithTag("filter-clear")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(0, applications)
        }
        composeRule.onNodeWithTag("search-filter-drawer").assertDoesNotExist()
        composeRule.onNodeWithTag("search-filter-button").assertIsFocused()
        composeRule.onNodeWithText("网络搜索").assertIsSelected()
        composeRule.onNodeWithText("首页").assertIsNotSelected()
    }

    @Test
    fun applyingSearchFiltersClosesDrawerWithoutLeavingSearch() {
        val baseSearchState = deepSearchState()
        var searchState by mutableStateOf(
            baseSearchState.copy(
                profiles = baseSearchState.profiles.map { profile ->
                    profile.copy(
                        filters = listOf(
                            SearchFilterDefinition(
                                key = "title",
                                label = "标题",
                                type = SearchFilterType.Text,
                            ),
                        ),
                    )
                },
            ),
        )
        var applications = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = searchState,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = SearchRoute,
                    searchActions = SearchActions(
                        openFilters = {
                            searchState = searchState.copy(filterDrawerOpen = true)
                        },
                        applyFilters = { values ->
                            applications += 1
                            searchState = searchState.copy(
                                appliedFilters = values,
                                filterDrawerOpen = false,
                            )
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("search-filter-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("filter-apply")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, applications)
        }
        composeRule.onNodeWithTag("search-filter-drawer").assertDoesNotExist()
        composeRule.onNodeWithText("网络搜索").assertIsSelected()
        composeRule.onNodeWithText("首页").assertIsNotSelected()
        composeRule.onNodeWithTag("search-filter-button").assertIsFocused()
    }

    @Test
    fun clearingSearchFiltersClosesDrawerWithoutLeavingSearch() {
        val baseSearchState = deepSearchState()
        var searchState by mutableStateOf(
            baseSearchState.copy(
                profiles = baseSearchState.profiles.map { profile ->
                    profile.copy(
                        filters = listOf(
                            SearchFilterDefinition(
                                key = "title",
                                label = "标题",
                                type = SearchFilterType.Text,
                            ),
                        ),
                    )
                },
                appliedFilters = mapOf(
                    "title" to SearchFilterValue.Scalar("星际"),
                ),
            ),
        )
        var clearings = 0
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = searchState,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = SearchRoute,
                    searchActions = SearchActions(
                        openFilters = {
                            searchState = searchState.copy(filterDrawerOpen = true)
                        },
                        clearFilters = {
                            clearings += 1
                            searchState = searchState.copy(
                                appliedFilters = emptyMap(),
                                filterDrawerOpen = false,
                            )
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("search-filter-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("filter-clear")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, clearings)
        }
        composeRule.onNodeWithTag("search-filter-drawer").assertDoesNotExist()
        composeRule.onNodeWithText("网络搜索").assertIsSelected()
        composeRule.onNodeWithText("首页").assertIsNotSelected()
        composeRule.onNodeWithTag("search-filter-button").assertIsFocused()
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
    fun directionDownFromSearchNavigationSkipsSingleIndexer() {
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

        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("network-search-input") and isFocused(),
            ).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag("network-search-input").assertIsFocused()
        composeRule.onNodeWithTag("indexer-11")
            .assertIsNotFocused()
            .assertIsSelected()
    }

    @Test
    fun directionDownFromSearchNavigationFocusesOffscreenSelectedIndexer() {
        val baseProfile = deepSearchState().profiles.single()
        val searchState = deepSearchState().copy(
            profiles = (1L..30L).map { id ->
                baseProfile.copy(
                    indexer = NetworkIndexer(id, "站点$id", null),
                )
            },
            selectedIndexerId = 30,
            focusedResultId = null,
        )
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = searchState,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.onNodeWithTag("indexer-1").assertExists()
        composeRule.onNodeWithTag("indexer-30").assertDoesNotExist()

        composeRule.onNodeWithTag("main-nav-search")
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("indexer-30") and isFocused(),
            ).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag("indexer-30").assertIsFocused()
        composeRule.onNodeWithTag("main-nav-search").assertIsNotFocused()
        composeRule.onNodeWithTag("network-search-input").assertIsNotFocused()
    }

    @Test
    fun directionUpFromFirstIndexerFocusesActiveSearchNavigation() {
        val firstProfile = deepSearchState().profiles.single()
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = deepSearchState().copy(
                        profiles = listOf(
                            firstProfile,
                            firstProfile.copy(
                                indexer = NetworkIndexer(22, "云端站", null),
                            ),
                        ),
                        focusedResultId = null,
                    ),
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("indexer-11")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNodeWithTag("main-nav-search")
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun directionUpFromSearchInputFocusesActiveSearchNavigation() {
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

        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("network-search-input")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNodeWithTag("main-nav-search")
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun enteringSearchEditorKeepsSearchRouteAndFocus() {
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

        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("network-search-input")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNode(
            hasTestTag("network-search-input") and hasSetTextAction(),
        ).assertIsFocused()
        composeRule.onNodeWithTag("main-nav-search").assertIsSelected()
        composeRule.onNodeWithText("首页").assertIsNotSelected()
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

        composeRule.onNodeWithTag("main-nav-search").assertIsEnabled()
        composeRule.onNodeWithText("媒体库").assertIsEnabled()
    }

    @Test
    fun directionDownFromLibraryNavigationSkipsSingleLibrary() {
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

        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("library-search-input") and isFocused(),
            ).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag("library-search-input").assertIsFocused()
        composeRule.onNodeWithTag("library-sidebar-item-21")
            .assertIsNotFocused()
            .assertIsSelected()
    }

    @Test
    fun directionDownFromLibraryNavigationFocusesOffscreenSelectedLibrary() {
        val selectedLibraryState = libraryState().copy(
            libraries = (1L..30L).map { id ->
                MediaLibrary(id, "媒体库$id", MediaLibraryType.TvShow)
            },
            selectedLibraryId = 30,
        )
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = selectedLibraryState,
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.onNodeWithTag("library-sidebar-item-1").assertExists()
        composeRule.onNodeWithTag("library-sidebar-item-30").assertDoesNotExist()

        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("library-sidebar-item-30") and isFocused(),
            ).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag("library-sidebar-item-30").assertIsFocused()
        composeRule.onNodeWithText("媒体库").assertIsNotFocused()
        composeRule.onNodeWithTag("library-search-input").assertIsNotFocused()
    }

    @Test
    fun directionUpFromFirstLibraryFocusesActiveLibraryNavigation() {
        val multipleLibraryState = libraryState().copy(
            libraries = listOf(
                MediaLibrary(21, "剧集库", MediaLibraryType.TvShow),
                MediaLibrary(22, "电影库", MediaLibraryType.Movie),
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = multipleLibraryState,
                    detailState = MediaDetailUiState.Content(detail()),
                )
            }
        }

        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithText("剧集库")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNodeWithText("媒体库")
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun directionUpFromLibraryInputFocusesActiveLibraryNavigation() {
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

        composeRule.onNode(hasText("媒体库") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("library-search-input")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNodeWithText("媒体库")
            .assertIsSelected()
            .assertIsFocused()
    }

    @Test
    fun openingDetailKeepsOutgoingLibraryFrameStableDuringFade() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    libraryState = libraryState(
                        items = listOf(
                            summary().copy(backdropPath = "/stable-library.jpg"),
                        ),
                    ),
                    detailState = MediaDetailUiState.Content(detail()),
                    initialRoute = LibraryRoute,
                )
            }
        }

        val contentBoundsBefore = composeRule.onNodeWithTag("library-content")
            .fetchSemanticsNode()
            .boundsInRoot

        composeRule.onNodeWithTag("media-card-201")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.mainClock.advanceTimeBy(
            KaloscopeMotion.ContentMillis.toLong() / 2,
        )

        val contentBoundsDuring = composeRule.onNodeWithTag("library-content")
            .fetchSemanticsNode()
            .boundsInRoot
        assertEquals(contentBoundsBefore, contentBoundsDuring)
        composeRule.onNodeWithTag("library-fullscreen-backdrop").assertExists()
        composeRule.onNode(hasText("媒体库") and hasClickAction()).assertExists()
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
    fun playerOpenedFromSearchReturnsToFocusedSearchResult() {
        var preparationEnds = 0
        var searchState by mutableStateOf(
            deepSearchState().copy(
                results = SearchResultsState.Content(
                    items = listOf(
                        NetworkSearchResult(
                            id = "v1",
                            title = "视频1",
                            coverPath = null,
                            rating = null,
                            category = null,
                            uploader = null,
                            uploadedAt = null,
                        ),
                    ),
                    total = 1,
                    pageNumber = 1,
                    hasNext = false,
                ),
                focusedResultId = "v1",
                gridViewport = GridViewportSnapshot.Top,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                val context = LocalContext.current
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = searchState,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    searchActions = SearchActions(
                        cancelResolution = {
                            if (searchState.resolvingResultId != null) {
                                preparationEnds += 1
                                searchState = searchState.copy(resolvingResultId = null)
                                true
                            } else {
                                false
                            }
                        },
                        consumeDestination = { requestId ->
                            if (searchState.pendingPlaybackRequestId == requestId) {
                                searchState = searchState.copy(
                                    pendingPlaybackRequestId = null,
                                )
                            }
                        },
                    ),
                    playerState = PlayerUiState.Loading(),
                    playbackControllerFactory = remember(context) {
                        PlaybackControllerFactory(context.applicationContext)
                    },
                )
            }
        }

        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("network-result-v1")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.runOnIdle {
            searchState = searchState.copy(
                pendingPlaybackRequestId = "network-request",
                resolvingResultId = "v1",
            )
        }
        composeRule.onNodeWithTag("player-loading-indicator").assertExists()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("network-result-v1"))
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasText("进入媒体库"))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("进入媒体库").assertDoesNotExist()
        composeRule.onNodeWithTag("main-nav-search")
            .assertIsSelected()
        composeRule.onNode(hasText("首页") and hasClickAction())
            .assertIsNotSelected()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("network-result-v1") and isFocused(),
            ).fetchSemanticsNodes().size == 1
        }
        composeRule.runOnIdle {
            assertEquals(1, preparationEnds)
        }
    }

    @Test
    fun readerOpenedFromSearchClosesRequestAndRestoresResultFocus() {
        var closedRequestId: String? = null
        var preparationEnds = 0
        var searchState by mutableStateOf(
            deepSearchState().copy(
                results = SearchResultsState.Content(
                    items = listOf(
                        NetworkSearchResult(
                            id = "t1",
                            title = "文本1",
                            coverPath = null,
                            rating = null,
                            category = null,
                            uploader = null,
                            uploadedAt = null,
                        ),
                    ),
                    total = 1,
                    pageNumber = 1,
                    hasNext = false,
                ),
                focusedResultId = "t1",
                gridViewport = GridViewportSnapshot.Top,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                TestMainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    searchState = searchState,
                    libraryState = libraryState(),
                    detailState = MediaDetailUiState.Content(detail()),
                    searchActions = SearchActions(
                        cancelResolution = {
                            if (searchState.resolvingResultId != null) {
                                preparationEnds += 1
                                searchState = searchState.copy(resolvingResultId = null)
                                true
                            } else {
                                false
                            }
                        },
                        consumeDestination = { requestId ->
                            if (searchState.pendingReaderRequestId == requestId) {
                                searchState = searchState.copy(
                                    pendingReaderRequestId = null,
                                )
                            }
                        },
                    ),
                    readerState = ReaderUiState.Text(
                        requestId = "reader-request",
                        serverId = "server-id",
                        content = ReaderTextContent.network(
                            indexerId = 11,
                            resourceId = "t1",
                            title = "文本1",
                            text = "正文",
                        ),
                        settings = TextReaderSettings(),
                        chapterOrder = ReaderChapterOrder.Ascending,
                    ),
                    readerActions = ReaderActions(
                        close = { closedRequestId = it },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("network-result-t1")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.runOnIdle {
            searchState = searchState.copy(
                pendingReaderRequestId = "reader-request",
                resolvingResultId = "t1",
            )
        }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("text-reader-content") and isFocused(),
            ).fetchSemanticsNodes().size == 1
        }

        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("text-reader-content"))
                .fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("network-result-t1") and isFocused(),
            ).fetchSemanticsNodes().size == 1
        }
        composeRule.runOnIdle {
            assertEquals("reader-request", closedRequestId)
            assertEquals(1, preparationEnds)
        }
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

        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(hasTestTag("network-result-v25"))
                .fetchSemanticsNodes().size == 1
        }

        composeRule.onNode(hasText("首页") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("main-nav-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.onNodeWithTag("main-nav-search").assertIsFocused()
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
    playerState: PlayerUiState = PlayerUiState.Loading(),
    playbackControllerFactory: PlaybackControllerFactory? = null,
    readerState: ReaderUiState = ReaderUiState.Idle,
    readerActions: ReaderActions = ReaderActions(),
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
        playerState = playerState,
        playbackControllerFactory = playbackControllerFactory,
        playerActions = PlayerActions(),
        readerState = readerState,
        readerActions = readerActions,
    )
}

private inline fun Bitmap.forEachPixel(
    block: (x: Int, y: Int, color: Int) -> Unit,
) {
    for (y in 0 until height) {
        for (x in 0 until width) {
            block(x, y, getPixel(x, y))
        }
    }
}

private fun Int.isNearColor(
    expected: Int,
    tolerance: Int,
): Boolean {
    return AndroidColor.red(this).isWithin(
        AndroidColor.red(expected),
        tolerance,
    ) &&
        AndroidColor.green(this).isWithin(
            AndroidColor.green(expected),
            tolerance,
        ) &&
        AndroidColor.blue(this).isWithin(
            AndroidColor.blue(expected),
            tolerance,
        )
}

private fun Int.isWithin(
    expected: Int,
    tolerance: Int,
): Boolean = this in (expected - tolerance)..(expected + tolerance)

private fun session(serverId: String = "server-id") = Session(
    server = SavedServer(serverId, "家庭服务器", "http://127.0.0.1:8000"),
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
