package org.kaloscope.tv.test.golden

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.app.RootFullscreenBackdropFrame
import org.kaloscope.tv.app.ServerSetupScreen
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.ServerImagePlaceholder
import org.kaloscope.tv.core.designsystem.ServerImageVisualState
import org.kaloscope.tv.core.designsystem.TvSearchField
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.SearchFilterDefinition
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.feature.library.LibraryItemsState
import org.kaloscope.tv.feature.library.LibraryScreen
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.player.PlayerActionUiState
import org.kaloscope.tv.feature.player.PlayerControls
import org.kaloscope.tv.feature.player.PlayerControlsUiState
import org.kaloscope.tv.feature.search.SearchResultsState
import org.kaloscope.tv.feature.search.SearchScreen
import org.kaloscope.tv.feature.search.SearchUiState
import org.kaloscope.tv.feature.server.ServerSetupState

class P2GoldenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun libraryGridMatchesCurrentResolution() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                GoldenLibrary()
            }
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        val width = Resources.getSystem().displayMetrics.widthPixels
        assertGolden("library-$width", composeRule.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun librarySidebarRowMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                GoldenLibrary()
            }
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        assertGolden(
            "library-sidebar-row-1920",
            composeRule.onNodeWithTag("library-sidebar-item-1")
                .captureToImage()
                .asAndroidBitmap(),
        )
    }

    @Test
    fun localNavigationIconsMatch1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeBackground {
                    LocalNavigationIconGoldenSheet()
                }
            }
        }

        assertGolden(
            "local-navigation-icons-1920",
            composeRule.onRoot().captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun localActionIconsMatch1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeBackground {
                    LocalActionIconGoldenSheet()
                }
            }
        }

        assertGolden(
            "local-action-icons-1920",
            composeRule.onRoot().captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun searchResultsMatch1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeBackground {
                    SearchScreen(
                        session = session(),
                        state = searchState(),
                        requestInitialFocus = false,
                        onRefreshIndexers = {},
                        onSelectIndexer = {},
                        onQueryChange = {},
                        onSearch = {},
                        onRetry = {},
                        onLoadMore = {},
                        onResultFocused = {},
                        onPlay = {},
                        onOpenFilters = {},
                        onDismissFilters = {},
                        onApplyFilters = {},
                        onClearFilters = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag("network-result-ranked")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(1_000)

        assertGolden(
            "search-results-1920",
            composeRule.onRoot().captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun searchCursorMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.mainClock.autoAdvance = false
        var query by mutableStateOf("Kaloscope")
        composeRule.setContent {
            KaloscopeTheme {
                TvSearchField(
                    value = query,
                    hint = "搜索",
                    onValueChange = { query = it },
                    onSearch = {},
                    modifier = Modifier.testTag("golden-search"),
                )
            }
        }
        composeRule.onNodeWithTag("golden-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.mainClock.advanceTimeBy(100)
        assertGolden(
            "search-cursor-1920",
            composeRule.onNodeWithTag("golden-search").captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun searchSelectionMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        var query by mutableStateOf("Kaloscope")
        composeRule.setContent {
            KaloscopeTheme {
                TvSearchField(
                    value = query,
                    hint = "搜索",
                    onValueChange = { query = it },
                    onSearch = {},
                    modifier = Modifier.testTag("golden-search-selection"),
                )
            }
        }
        composeRule.onNodeWithTag("golden-search-selection")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .performSemanticsAction(SemanticsActions.SetSelection) {
                it(1, 5, false)
            }
        assertGolden(
            "search-selection-1920",
            composeRule.onNodeWithTag("golden-search-selection")
                .captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun imageStatesMatch1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                Column(Modifier.fillMaxSize()) {
                    ServerImagePlaceholder(ServerImageVisualState.Loading, "L")
                    ServerImagePlaceholder(ServerImageVisualState.Missing, "无")
                    ServerImagePlaceholder(ServerImageVisualState.Failed, "F")
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(600)
        assertGolden("image-states-1920", composeRule.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun controlStatesMatch1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.mainClock.autoAdvance = false
        var spec by mutableStateOf(controlStateGoldenSpecs().first())
        composeRule.setContent {
            KaloscopeTheme {
                ControlStateGoldenCell(
                    spec = spec,
                    modifier = Modifier.testTag("control-state-cell"),
                )
            }
        }

        val cells = controlStateGoldenSpecs().map { next ->
            composeRule.runOnIdle { spec = next }
            composeRule.mainClock.advanceTimeBy(220)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("control-state-cell")
                .captureToImage()
                .asAndroidBitmap()
        }
        assertGolden("control-states-1920", stitchControlStateCells(cells))
    }

    @Test
    fun playerControlsMatch1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                GoldenPlayerControls()
            }
        }
        composeRule.onNodeWithContentDescription("暂停")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(220)

        assertGolden(
            "player-controls-1920",
            composeRule.onRoot().captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun playerAuxiliaryFocusMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                GoldenPlayerControls()
            }
        }
        composeRule.onNodeWithTag("player-danmaku")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(220)

        assertGolden(
            "player-controls-auxiliary-focus-1920",
            composeRule.onRoot().captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun serverDeleteFocusMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                GoldenServerSetup()
            }
        }

        composeRule.onNodeWithTag("saved-server-golden-server")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("delete-server-golden-server").assertIsFocused()

        assertGolden(
            "server-delete-focus-1920",
            composeRule.onRoot().captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun serverDeletionDialogMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                GoldenServerSetup()
            }
        }

        composeRule.onNodeWithTag("saved-server-golden-server")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("delete-server-golden-server")
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("confirm-dialog-cancel").assertIsFocused()

        assertGolden(
            "server-deletion-dialog-1920",
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(),
        )
    }
}

@Composable
private fun GoldenPlayerControls() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6F8797),
                        Color(0xFF385064),
                        Color(0xFF17283B),
                    ),
                ),
            ),
    ) {
        PlayerControls(
            state = PlayerControlsUiState(
                title = "星海纪行",
                secondaryTitle = "第 4 集 · 穿越静默海",
                isPlaying = true,
                positionMillis = 2_758_000,
                durationMillis = 2_758_000,
                playbackModeLabel = "自动 · 1080P",
                playbackSpeed = 1f,
                fallbackInProgress = false,
                progressSaveFailed = false,
                previousEnabled = true,
                nextEnabled = true,
                subtitles = PlayerActionUiState(enabled = true, active = true),
                danmakus = PlayerActionUiState(enabled = true, active = true),
                danmakuSettings = PlayerActionUiState(enabled = true),
                quality = PlayerActionUiState(enabled = true),
                subtitleLabel = "简体中文",
            ),
            playFocus = remember { FocusRequester() },
            definitionFocus = remember { FocusRequester() },
            danmakuSettingsFocus = remember { FocusRequester() },
            subtitleFocus = remember { FocusRequester() },
            speedFocus = remember { FocusRequester() },
            onPrevious = {},
            onRewind = {},
            onPlayPause = {},
            onForward = {},
            onNext = {},
            onOpenSubtitles = {},
            onOpenSpeed = {},
            onToggleDanmakus = {},
            onOpenDanmakuSettings = {},
            onOpenDefinitions = {},
            onSeekTo = {},
            onHideControls = {},
            onInteraction = {},
        )
    }
}

@Composable
private fun GoldenLibrary() {
    KaloscopeBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            RootFullscreenBackdropFrame(
                testTag = "golden-library-backdrop",
            ) { imageModifier ->
                Box(
                    modifier = imageModifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF21445D),
                                Color(0xFF77545B),
                                Color(0xFF263954),
                            ),
                        ),
                    ),
                )
            }
            LibraryScreen(
                session = session(),
                state = libraryState(),
                restoreMediaId = null,
                onSelectLibrary = {},
                onQueryChange = {},
                onSearch = {},
                onRetry = {},
                onLoadMore = {},
                onMediaFocused = {},
                onOpenMedia = {},
            )
        }
    }
}

@Composable
private fun GoldenServerSetup() {
    ServerSetupScreen(
        savedServers = listOf(
            SavedServer(
                id = "golden-server",
                name = "家庭服务器",
                origin = "https://home.example",
            ),
        ),
        state = ServerSetupState(),
        onNameChange = {},
        onUrlChange = {},
        onTest = {},
        onSave = {},
        onSelectServer = {},
    )
}

private fun libraryState(): LibraryUiState.Content {
    val items = (1..30).map { id ->
        MediaSummary(
            id = id.toLong(),
            title = "固定媒体标题 $id",
            path = "/media/$id",
            posterPath = null,
            backdropPath = "/unused/$id",
            year = 2026.takeUnless { id % 4 == 0 },
            rating = if (id % 3 == 0) 8.6 else null,
            season = null,
            episode = null,
        )
    }
    return LibraryUiState.Content(
        libraries = listOf(MediaLibrary(1, "固定媒体库", MediaLibraryType.Movie)),
        selectedLibraryId = 1,
        items = LibraryItemsState.Content(
            items = items,
            total = items.size,
            pageNumber = 1,
            hasNext = false,
        ),
        focusedMediaId = 1,
        gridViewport = GridViewportSnapshot.Top,
    )
}

private fun searchState(): SearchUiState.Content {
    val results = listOf(
        NetworkSearchResult(
            id = "ranked",
            title = "完整信息的网络搜索结果",
            coverPath = null,
            rating = 9.5,
            category = "电影",
            uploader = "Admin",
            uploadedAt = "10 Hours Ago",
            ranking = 1,
            misc = "1:30:00",
            size = "1GB",
        ),
        NetworkSearchResult(
            id = "rated",
            title = "没有排名时显示评分的较长网络搜索结果标题",
            coverPath = null,
            rating = 8.6,
            category = "剧集",
            uploader = "Uploader",
            uploadedAt = "Yesterday",
            misc = "1080P",
            size = "12.4GB",
        ),
        NetworkSearchResult(
            id = "sparse",
            title = "缺少可选字段的结果",
            coverPath = null,
            rating = null,
            category = null,
            uploader = null,
            uploadedAt = null,
        ),
    )
    return SearchUiState.Content(
        profiles = listOf(
            IndexerSourceProfile(
                indexer = NetworkIndexer(11, "固定测试站点", null),
                pageSize = 20,
                keywordRequired = true,
                filters = listOf(
                    SearchFilterDefinition(
                        key = "region",
                        label = "地区",
                        type = SearchFilterType.Text,
                    ),
                ),
            ),
        ),
        selectedIndexerId = 11,
        query = "Kaloscope",
        submittedKeyword = "Kaloscope",
        appliedFilters = mapOf(
            "region" to SearchFilterValue.Scalar("cn"),
        ),
        focusedResultId = "ranked",
        gridViewport = GridViewportSnapshot.Top,
        results = SearchResultsState.Content(
            items = results,
            total = results.size,
            pageNumber = 1,
            hasNext = false,
        ),
    )
}

private fun session() = Session(
    server = SavedServer("golden", "Golden", "http://127.0.0.1:8000"),
    token = "fixture",
    user = SessionUser(1, "golden", "user"),
)
