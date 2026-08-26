package org.kaloscope.tv.feature.search

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.R
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.SearchFilterDefinition
import org.kaloscope.tv.core.model.SearchFilterOption
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.test.assertFocusedContentCardCornerRadius
import org.kaloscope.tv.test.assertFocusedContentCardBottomInsideViewport
import org.kaloscope.tv.test.assertFocusedContentCardScale
import org.kaloscope.tv.test.assertFocusedContentCardSurface
import org.kaloscope.tv.test.assertSidebarNavigationSurfaces

class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun indexerSidebarUsesApprovedRestingSurfaces() {
        val firstProfile = state().profiles.single()
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background),
                ) {
                    SearchScreen(
                        session = session(),
                        state = state().copy(
                            profiles = listOf(
                                firstProfile,
                                firstProfile.copy(
                                    indexer = NetworkIndexer(22, "云端站", null),
                                ),
                            ),
                        ),
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

        val selected = composeRule.onNodeWithTag("indexer-11")
            .captureToImage()
            .asAndroidBitmap()
        val unselected = composeRule.onNodeWithTag("indexer-22")
            .captureToImage()
            .asAndroidBitmap()
        val sampleInset = with(composeRule.density) { 16.dp.roundToPx() }

        assertSidebarNavigationSurfaces(
            label = "Indexer sidebar",
            selected = selected,
            unselected = unselected,
            sampleInset = sampleInset,
        )
    }

    @Test
    fun longIndexerNameUsesSingleLineEllipsis() {
        val longName = "轻小说翻译资源站点名称长到无法在菜单中完整显示"
        val firstProfile = state().profiles.single()
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state().copy(
                        profiles = listOf(
                            firstProfile.copy(
                                indexer = NetworkIndexer(11, longName, null),
                            ),
                        ),
                    ),
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

        val layoutResults = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(longName, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(layoutResults)
            }

        val layout = layoutResults.single()
        assertEquals(1, layout.lineCount)
        assertTrue("Long indexer name should end with an ellipsis", layout.isLineEllipsized(0))
    }

    @Test
    fun missingIndexerIconUsesSharedBrokenImagePlaceholder() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(indexerIconPath = null),
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

        composeRule.onNodeWithTag("indexer-11", useUnmergedTree = true)
            .assert(hasAnyDescendant(hasTestTag("server-image-broken-icon")))
    }

    @Test
    fun focusedNetworkResultUsesLighterBlueSurface() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(),
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

        composeRule.onNodeWithTag("network-result-v1")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(1_000)
        val focused = composeRule.onNodeWithTag("network-result-v1")
            .captureToImage()
            .asAndroidBitmap()
        val sampleInset = with(composeRule.density) { 12.dp.roundToPx() }

        assertFocusedContentCardSurface(
            label = "Network result card",
            bitmap = focused,
            sampleX = focused.width / 2,
            sampleY = focused.height - sampleInset,
        )
    }

    @Test
    fun resolvingNetworkResultBorderMatchesCardCornerRadius() {
        var screenState by mutableStateOf(state())
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background),
                ) {
                    SearchScreen(
                        session = session(),
                        state = screenState,
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

        composeRule.onNodeWithTag("network-result-v1")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.runOnIdle {
            screenState = screenState.copy(resolvingResultId = "v1")
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        val resolving = composeRule.onNodeWithTag("network-result-v1")
            .assertIsFocused()
            .captureToImage()
            .asAndroidBitmap()

        assertFocusedContentCardCornerRadius(
            label = "Resolving network result card",
            bitmap = resolving,
            density = composeRule.density.density,
        )
    }

    @Test
    fun focusedNetworkResultUsesThreePercentScale() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(),
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

        composeRule.onNodeWithTag("search-action-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(1_000)
        val cardBounds = composeRule.onNodeWithTag("network-result-v1")
            .fetchSemanticsNode()
            .boundsInRoot
        val scaleSearchPadding = with(composeRule.density) { 5.dp.roundToPx() }
        val resting = composeRule.onRoot()
            .captureToImage()
            .asAndroidBitmap()
        composeRule.onNodeWithTag("network-result-v1")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(1_000)
        val focused = composeRule.onRoot()
            .captureToImage()
            .asAndroidBitmap()

        assertFocusedContentCardScale(
            label = "Network result card",
            resting = resting,
            focused = focused,
            searchBounds = cardBounds,
            searchPadding = scaleSearchPadding,
        )
    }

    @Test
    fun initialLoadingUsesCenteredIndicator() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = SearchUiState.Loading,
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

        composeRule.onNodeWithTag("search-loading-indicator").assertExists()
        composeRule.onNodeWithTag("search-loading-skeleton").assertDoesNotExist()
    }

    @Test
    fun resultLoadingKeepsKnownControlsAndCentersIndicatorInContentRegion() {
        val content = state()
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = content.copy(results = SearchResultsState.Loading),
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

        composeRule.onNodeWithTag("indexer-11").assertExists()
        composeRule.onNodeWithTag("network-search-input").assertExists()
        composeRule.onNodeWithTag("search-results-loading-indicator").assertExists()
        composeRule.onNodeWithTag("search-results-loading-skeleton").assertDoesNotExist()
    }

    @Test
    fun searchFailureUsesCompactRetryPresentation() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state().copy(
                        results = SearchResultsState.Error(AppError.Offline),
                    ),
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

        composeRule.onNodeWithText("加载失败").assertExists()
        composeRule.onNodeWithText("无法加载网络搜索").assertDoesNotExist()
        composeRule.onNodeWithTag(
            testTag = "search-retry-refresh-icon",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun retryingSearchFailureFocusesSelectedIndexer() {
        val firstProfile = state().profiles.single()
        val errorState = state().copy(
            profiles = listOf(
                firstProfile,
                firstProfile.copy(
                    indexer = NetworkIndexer(22, "云端站", null),
                ),
            ),
            selectedIndexerId = 22,
            results = SearchResultsState.Error(AppError.Offline),
        )
        var currentState by mutableStateOf<SearchUiState>(errorState)
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = currentState,
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {
                        retries += 1
                        currentState = errorState.copy(results = SearchResultsState.Loading)
                    },
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

        composeRule.onNodeWithText("重试")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("indexer-22").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }

    @Test
    fun retryingSearchFailureWithSingleIndexerFocusesSearchInput() {
        val errorState = state().copy(
            results = SearchResultsState.Error(AppError.Offline),
        )
        var currentState by mutableStateOf<SearchUiState>(errorState)
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = currentState,
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {
                        retries += 1
                        currentState = errorState.copy(results = SearchResultsState.Loading)
                    },
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

        composeRule.onNodeWithText("重试")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("network-search-input").assertIsFocused()
        composeRule.onNodeWithTag("indexer-11")
            .assertIsNotFocused()
            .assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }

    @Test
    fun singleIndexerInitialFocusesSearchInput() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(),
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

        composeRule.onNodeWithTag("network-search-input").assertIsFocused()
        composeRule.onNodeWithTag("indexer-11")
            .assertIsNotFocused()
            .assertIsSelected()
    }

    @Test
    fun movingLeftFromSearchInputSkipsSingleIndexer() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(),
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

        composeRule.onNodeWithTag("network-search-input")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }

        composeRule.onNodeWithTag("network-search-input").assertIsFocused()
        composeRule.onNodeWithTag("indexer-11").assertIsNotFocused()
    }

    @Test
    fun multipleIndexersInitialFocusesFirstIndexer() {
        val firstProfile = state().profiles.single()
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state().copy(
                        profiles = listOf(
                            firstProfile,
                            firstProfile.copy(
                                indexer = NetworkIndexer(22, "云端站", null),
                            ),
                        ),
                    ),
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

        composeRule.onNodeWithTag("indexer-11").assertIsFocused()
        composeRule.onNodeWithTag("network-search-input").assertIsNotFocused()
    }

    @Test
    fun rightFromLowerIndexerFocusesFirstVisibleResult() {
        val firstProfile = state().profiles.single()
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        results = (1..12).map { result("v$it") },
                    ).copy(
                        profiles = (11L..17L).map { indexerId ->
                            firstProfile.copy(
                                indexer = NetworkIndexer(
                                    indexerId,
                                    "站点$indexerId",
                                    null,
                                ),
                            )
                        },
                    ),
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

        composeRule.onNodeWithTag("indexer-17")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("network-result-v1").assertIsFocused()
    }

    @Test
    fun rightFromIndexerAtDeepViewportFocusesFirstVisibleResult() {
        val firstProfile = state().profiles.single()
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        results = (1..30).map { result("v$it") },
                    ).copy(
                        profiles = (11L..17L).map { indexerId ->
                            firstProfile.copy(
                                indexer = NetworkIndexer(
                                    indexerId,
                                    "站点$indexerId",
                                    null,
                                ),
                            )
                        },
                    ),
                    requestInitialFocus = false,
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onResultFocused = {},
                    onGridViewportChanged = {},
                    onPlay = {},
                    onOpenFilters = {},
                    onDismissFilters = {},
                    onApplyFilters = {},
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("search-results-grid").performScrollToIndex(12)
        val gridBounds = composeRule.onNodeWithTag("search-results-grid")
            .fetchSemanticsNode()
            .boundsInRoot
        val visibleResultIds = (1..30).filter { resultId ->
            composeRule.onAllNodes(hasTestTag("network-result-v$resultId"))
                .fetchSemanticsNodes()
                .any { node ->
                    node.boundsInRoot.bottom > gridBounds.top &&
                        node.boundsInRoot.top < gridBounds.bottom
                }
        }
        assertTrue("Scrolled grid must contain visible results", visibleResultIds.isNotEmpty())
        val firstVisibleResultId = visibleResultIds.first()
        assertTrue("Scrolled grid must leave the first result behind", firstVisibleResultId > 1)
        composeRule.onNodeWithTag("indexer-17")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("network-result-v$firstVisibleResultId").assertIsFocused()
        composeRule.onNodeWithTag("network-result-v1").assertDoesNotExist()
    }

    @Test
    fun leftFromLeftmostResultFocusesSelectedIndexer() {
        val firstProfile = state().profiles.single()
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        results = (1..6).map { result("v$it") },
                    ).copy(
                        profiles = (11L..30L).map { indexerId ->
                            firstProfile.copy(
                                indexer = NetworkIndexer(
                                    indexerId,
                                    "站点$indexerId",
                                    null,
                                ),
                            )
                        },
                        selectedIndexerId = 30L,
                    ),
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

        composeRule.onNodeWithTag("network-result-v4")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }

        composeRule.onNodeWithTag("indexer-30").assertIsFocused()
    }

    @Test
    fun leftFromSecondResultMovesToAdjacentResult() {
        val firstProfile = state().profiles.single()
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        results = (1..6).map { result("v$it") },
                    ).copy(
                        profiles = (11L..13L).map { indexerId ->
                            firstProfile.copy(
                                indexer = NetworkIndexer(
                                    indexerId,
                                    "站点$indexerId",
                                    null,
                                ),
                            )
                        },
                        selectedIndexerId = 13L,
                    ),
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

        composeRule.onNodeWithTag("network-result-v2")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }

        composeRule.onNodeWithTag("network-result-v1").assertIsFocused()
        composeRule.onNodeWithTag("indexer-13").assertIsNotFocused()
    }

    @Test
    fun graphIconReplacesIndexerInitial() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        indexerIconPath = "icons/indexer.webp",
                        results = emptyList(),
                    ),
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

        composeRule.onNodeWithTag(
            testTag = "server-image-success",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithText(
            text = "星",
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun resultCenterClickRequestsDirectPlayback() {
        var selectedId: String? = null
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(),
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onResultFocused = {},
                    onPlay = { selectedId = it },
                    onOpenFilters = {},
                    onDismissFilters = {},
                    onApplyFilters = {},
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("network-result-v1")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals("v1", selectedId)
        }
    }

    @Test
    fun completeResultRendersWebGridMetadata() {
        val completeResult = result("v1").copy(
            misc = "1:30:00",
            uploader = "Admin",
            uploadedAt = "10 Hours Ago",
            size = "1GB",
        )
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(results = listOf(completeResult)),
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

        composeRule.onNodeWithText(
            text = "1:30:00",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithText(
            text = "UP: Admin · 10 Hours Ago",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithText(
            text = "1GB",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithText(
            text = "科幻",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun shortSizeLeavesEnoughWidthForSingleLineSource() {
        val width = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.widthPixels
        if (width != 1920) return
        val source = "UP: 就叫阿路8 · 2026-07-30"
        val shortSize = "255.1万"
        val completeResult = result("v1").copy(
            uploader = "就叫阿路8",
            uploadedAt = "2026-07-30",
            size = shortSize,
        )
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(results = listOf(completeResult)),
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

        val sourceHeight = composeRule.onNodeWithText(
            text = source,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.height
        val sizeHeight = composeRule.onNodeWithText(
            text = shortSize,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.height

        assertTrue(
            "Source metadata should remain one line when the short size fits",
            sourceHeight <= sizeHeight * 1.25f,
        )
    }

    @Test
    fun rankingReplacesRatingBadge() {
        val rankedResult = result("v1").copy(ranking = 2)
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(results = listOf(rankedResult)),
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

        composeRule.onNodeWithTag(
            testTag = "search-result-ranking-v1",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "search-result-rating-v1",
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun unrankedResultUsesRatingBadge() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(),
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

        composeRule.onNodeWithTag(
            testTag = "search-result-rating-v1",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "search-result-ranking-v1",
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun resolvingStateDoesNotReplaceCardMetadataInline() {
        val completeResult = result("v1").copy(
            misc = "1:30:00",
            uploader = "Admin",
            uploadedAt = "10 Hours Ago",
            size = "1GB",
        )
        var screenState by mutableStateOf(
            state(results = listOf(completeResult)),
        )
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = screenState,
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

        val initialBounds = composeRule.onNodeWithTag("network-result-v1")
            .fetchSemanticsNode()
            .boundsInRoot
        composeRule.runOnIdle {
            screenState = screenState.copy(resolvingResultId = "v1")
        }
        val resolvingBounds = composeRule.onNodeWithTag("network-result-v1")
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(initialBounds, resolvingBounds)
        composeRule.onNodeWithText(
            text = InstrumentationRegistry.getInstrumentation()
                .targetContext.getString(R.string.playback_preparation_resource),
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithText(
            text = "UP: Admin · 10 Hours Ago",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun landscapeGridFitsExactlyThreeResultsPerRowInAuthenticatedFrameAt1080p() {
        val width = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.widthPixels
        if (width != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 44.dp),
                ) {
                    SearchScreen(
                        session = session(),
                        state = state(
                            results = (1..4).map { result("v$it") },
                        ),
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

        val resultTops = (1..4).map { id ->
            composeRule.onNodeWithTag("network-result-v$id")
                .fetchSemanticsNode()
                .boundsInRoot.top
        }

        resultTops.take(3).forEach { top ->
            assertEquals(resultTops.first(), top, 0.5f)
        }
        assertTrue(
            "The fourth landscape result should start the second row",
            resultTops[3] > resultTops.first(),
        )
    }

    @Test
    fun portraitGridFitsExactlyFourResultsPerRowWithSharedSpacingAt1080p() {
        val width = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.widthPixels
        if (width != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 44.dp),
                ) {
                    SearchScreen(
                        session = session(),
                        state = state(
                            coverRatio = 2f / 3f,
                            results = (1..5).map { result("v$it") },
                        ),
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

        val resultBounds = (1..5).map { id ->
            composeRule.onNodeWithTag("network-result-v$id")
                .fetchSemanticsNode()
                .boundsInRoot
        }
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        resultBounds.take(4).forEach { bounds ->
            assertEquals(resultBounds.first().top, bounds.top, 0.5f)
        }
        assertTrue(
            "The fifth portrait result should start the second row",
            resultBounds[4].top > resultBounds.first().top,
        )
        assertEquals(
            10f * density,
            resultBounds[1].left - resultBounds[0].right,
            1f,
        )
        assertEquals(
            14f * density,
            resultBounds[4].top - resultBounds[0].bottom,
            1f,
        )
    }

    @Test
    fun focusedSecondRowResultKeepsScaledBottomInsideGridAt1080p() {
        val width = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.widthPixels
        if (width != 1920) return
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(
                            start = 44.dp,
                            top = 84.dp,
                            end = 44.dp,
                            bottom = 24.dp,
                        ),
                ) {
                    SearchScreen(
                        session = session(),
                        state = state(
                            coverRatio = 2f / 3f,
                            results = (1..9).map { result("v$it") },
                        ),
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

        composeRule.onNodeWithTag("network-result-v5")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.mainClock.advanceTimeBy(1_000)
        val cardBounds = composeRule.onNodeWithTag("network-result-v5")
            .fetchSemanticsNode()
            .boundsInRoot
        val gridBounds = composeRule.onNodeWithTag("search-results-grid")
            .fetchSemanticsNode()
            .boundsInRoot
        val screenshot = composeRule.onRoot()
            .captureToImage()
            .asAndroidBitmap()

        assertFocusedContentCardBottomInsideViewport(
            label = "Second-row network result",
            bitmap = screenshot,
            cardBounds = cardBounds,
            viewportBounds = gridBounds,
            density = composeRule.density.density,
        )
    }

    @Test
    fun emptyIndexerStateOffersRecoveryActions() {
        var refreshes = 0
        var serverSwitches = 0
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = SearchUiState.EmptyIndexers,
                    onRefreshIndexers = { refreshes += 1 },
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
                    onManageServers = { serverSwitches += 1 },
                )
            }
        }

        composeRule.onNodeWithTag("refresh-indexers")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("search-manage-servers")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, refreshes)
            assertEquals(1, serverSwitches)
        }
    }

    @Test
    fun searchImeActionSubmitsTheCurrentQuery() {
        var searches = 0
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(),
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = { searches += 1 },
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

        composeRule.onNodeWithTag("network-search-input")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .performImeAction()

        composeRule.runOnIdle {
            assertEquals(1, searches)
        }
        composeRule.onNodeWithTag("network-search-input")
            .assertIsFocused()
            .assertHasClickAction()
    }

    @Test
    fun filterButtonIsHiddenWithoutDefinitionsAndVisibleWithDefinitions() {
        var filters by mutableStateOf(emptyList<SearchFilterDefinition>())
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(filters = filters),
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
        composeRule.onNodeWithTag("search-filter-button").assertDoesNotExist()

        composeRule.runOnIdle {
            filters = listOf(regionFilter())
        }
        composeRule.onNodeWithTag("search-filter-button").assertExists()
    }

    @Test
    fun searchActionsUseCompactSharedControlHeightInWebUiOrder() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(filters = listOf(regionFilter())),
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

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val filterBounds = composeRule.onNodeWithTag("search-filter-button")
            .fetchSemanticsNode()
            .boundsInRoot
        val searchBounds = composeRule.onNodeWithTag("search-action-button")
            .fetchSemanticsNode()
            .boundsInRoot
        val filterIconBounds = composeRule.onNodeWithTag(
            testTag = "search-filter-icon",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val searchIconBounds = composeRule.onNodeWithTag(
            testTag = "search-action-icon",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        listOf(filterBounds, searchBounds).forEach { bounds ->
            assertEquals(48f * density, bounds.width, 1f)
            assertEquals(48f * density, bounds.height, 1f)
        }
        listOf(filterIconBounds, searchIconBounds).forEach { bounds ->
            assertEquals(24f * density, bounds.width, 1f)
            assertEquals(24f * density, bounds.height, 1f)
        }
        assertTrue(filterBounds.right < searchBounds.left)
        composeRule.onNodeWithText("筛选", useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("搜索", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun resultsStartTwentySixDpBelowSearchField() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        coverRatio = 2f / 3f,
                        results = (1..5).map { result("v$it") },
                    ),
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

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val inputBounds = composeRule.onNodeWithTag("network-search-input")
            .fetchSemanticsNode()
            .boundsInRoot
        val firstResultBounds = composeRule.onNodeWithTag("network-result-v1")
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(
            26f * density,
            firstResultBounds.top - inputBounds.bottom,
            1f,
        )
    }

    @Test
    fun rightFromSearchFieldMovesThroughFilterAndSearchActions() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(filters = listOf(regionFilter())),
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

        composeRule.onNodeWithTag("network-search-input")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("search-filter-button").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("search-action-button").assertIsFocused()
    }

    @Test
    fun iconSearchAndFilterActionsInvokeExistingCallbacks() {
        var searches = 0
        var filterOpens = 0
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(filters = listOf(regionFilter())),
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = { searches += 1 },
                    onRetry = {},
                    onLoadMore = {},
                    onResultFocused = {},
                    onPlay = {},
                    onOpenFilters = { filterOpens += 1 },
                    onDismissFilters = {},
                    onApplyFilters = {},
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("search-filter-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("search-action-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, filterOpens)
            assertEquals(1, searches)
        }
    }

    @Test
    fun selectedIndexerRemainsSelectedWhileSearchActionOwnsFocus() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(),
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

        composeRule.onNodeWithTag("search-action-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.onNodeWithTag("indexer-11").assertIsSelected()
    }

    @Test
    fun selectingLowerIndexerKeepsFocusOnSelectedItem() {
        val firstProfile = state().profiles.single()
        var currentState by mutableStateOf(
            state().copy(
                profiles = listOf(
                    firstProfile,
                    firstProfile.copy(
                        indexer = NetworkIndexer(22, "云端站", null),
                    ),
                ),
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = currentState,
                    onRefreshIndexers = {},
                    onSelectIndexer = { indexerId ->
                        currentState = currentState.copy(
                            selectedIndexerId = indexerId,
                            results = SearchResultsState.AwaitingQuery,
                            focusedResultId = null,
                        )
                    },
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

        composeRule.onNodeWithTag("indexer-22")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("indexer-22")
            .assertIsSelected()
            .assertIsFocused()
        composeRule.onNodeWithTag("indexer-11").assertIsNotFocused()
    }

    @Test
    fun appliedFiltersMarkFilterActionSelected() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(regionFilter()),
                        appliedFilters = mapOf(
                            "region" to SearchFilterValue.Scalar("cn"),
                        ),
                    ),
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

        composeRule.onNodeWithTag("search-filter-button").assertIsSelected()
    }

    @Test
    fun textFilterWaitsForCenterAndUsesDoneImeAction() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(textFilter()),
                        filterDrawerOpen = true,
                    ),
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

        composeRule.onNodeWithTag("filter-input-title")
            .assertIsFocused()
            .assertHasClickAction()
            .performKeyInput { pressKey(Key.Enter) }
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ImeAction,
                    ImeAction.Done,
                ),
            )
            .performImeAction()
        composeRule.onNodeWithTag("filter-input-title")
            .assertIsFocused()
            .assertHasClickAction()
    }

    @Test
    fun selectFilterDefaultsToAll() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(regionFilter()),
                        filterDrawerOpen = true,
                    ),
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

        val all = composeRule.onNodeWithTag("filter-option-region-all")
            .assertIsSelected()
            .assertIsFocused()
        composeRule.onNodeWithText("全部").assertExists()
        val firstOption = composeRule.onNodeWithTag("filter-option-region-cn")
            .assertIsNotSelected()

        assertTrue(
            all.fetchSemanticsNode().boundsInRoot.top <
                firstOption.fetchSemanticsNode().boundsInRoot.top,
        )
    }

    @Test
    fun selectingAllClearsSelectValue() {
        var applied: Map<String, SearchFilterValue>? = null
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(regionFilter()),
                        appliedFilters = mapOf(
                            "region" to SearchFilterValue.Scalar("cn"),
                        ),
                        filterDrawerOpen = true,
                    ),
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
                    onApplyFilters = { applied = it },
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("filter-option-region-cn").assertIsSelected()
        composeRule.onNodeWithTag("filter-option-region-all")
            .assertIsNotSelected()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsSelected()
        composeRule.onNodeWithTag("filter-option-region-cn").assertIsNotSelected()
        composeRule.onNodeWithTag("filter-apply")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(emptyMap<String, SearchFilterValue>(), applied)
        }
    }

    @Test
    fun filterChoiceAppliesSelectedValue() {
        var applied: Map<String, SearchFilterValue>? = null
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(regionFilter()),
                        filterDrawerOpen = true,
                    ),
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
                    onApplyFilters = { applied = it },
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("filter-option-region-cn")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsSelected()
        composeRule.onNodeWithTag("filter-apply")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(SearchFilterValue.Scalar("cn"), applied?.get("region"))
        }
    }

    @Test
    fun filterIndicatorsMatchTheServerSelectionTypes() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(sortFilter(), genreFilter(), regionFilter()),
                        appliedFilters = mapOf(
                            "sort" to SearchFilterValue.Scalar("clicks"),
                            "genre" to SearchFilterValue.Multiple(listOf("fantasy")),
                            "region" to SearchFilterValue.Scalar("cn"),
                        ),
                        filterDrawerOpen = true,
                    ),
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

        composeRule.onNodeWithTag("filter-option-sort-clicks")
            .assertIsSelected()
        composeRule.onNodeWithTag(
            testTag = "filter-option-sort-clicks-radio-indicator",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "filter-option-sort-clicks-radio-indicator-mark",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "filter-option-sort-updated-radio-indicator",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "filter-option-sort-updated-radio-indicator-mark",
            useUnmergedTree = true,
        ).assertDoesNotExist()

        composeRule.onNodeWithTag("filter-option-genre-fantasy")
            .assertIsSelected()
        composeRule.onNodeWithTag(
            testTag = "filter-option-genre-fantasy-checkbox-indicator",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "filter-option-genre-fantasy-checkbox-indicator-mark",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "filter-option-genre-scifi-checkbox-indicator",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(
            testTag = "filter-option-genre-scifi-checkbox-indicator-mark",
            useUnmergedTree = true,
        ).assertDoesNotExist()

        composeRule.onNodeWithTag("filter-option-region-cn")
            .assertIsSelected()
        composeRule.onNodeWithTag(
            testTag = "filter-option-region-cn-radio-indicator",
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(
            testTag = "filter-option-region-cn-checkbox-indicator",
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun clickingTheSelectedRadioClearsItBeforeApply() {
        var applied: Map<String, SearchFilterValue>? = null
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(sortFilter()),
                        appliedFilters = mapOf(
                            "sort" to SearchFilterValue.Scalar("clicks"),
                        ),
                        filterDrawerOpen = true,
                    ),
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
                    onApplyFilters = { applied = it },
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("filter-option-sort-clicks")
            .assertIsSelected()
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .assertIsNotSelected()
        composeRule.onNodeWithTag(
            testTag = "filter-option-sort-clicks-radio-indicator-mark",
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithTag("filter-apply")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle { assertEquals(emptyMap<String, SearchFilterValue>(), applied) }
    }

    @Test
    fun filterActionsShowIconsAtStandardSize() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(regionFilter()),
                        filterDrawerOpen = true,
                    ),
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

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val clearIcon = composeRule.onNodeWithTag(
            testTag = "filter-clear-icon",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val applyIcon = composeRule.onNodeWithTag(
            testTag = "filter-apply-icon",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        listOf(clearIcon, applyIcon).forEach { bounds ->
            assertEquals(22f * density, bounds.width, 1f)
            assertEquals(22f * density, bounds.height, 1f)
        }
        composeRule.onNodeWithText("清除", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("应用", useUnmergedTree = true).assertExists()
    }

    @Test
    fun filterActionsFollowHorizontalDpadOrder() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(regionFilter()),
                        filterDrawerOpen = true,
                    ),
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

        val clear = composeRule.onNodeWithTag("filter-clear")
        val apply = composeRule.onNodeWithTag("filter-apply")

        clear.performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        apply.assertIsFocused()

        apply.performKeyInput { pressKey(Key.DirectionLeft) }
        clear.assertIsFocused()
    }

    @Test
    fun closingFilterDrawerRestoresFilterButtonFocus() {
        var drawerOpen by mutableStateOf(false)
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(regionFilter()),
                        filterDrawerOpen = drawerOpen,
                    ),
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onResultFocused = {},
                    onPlay = {},
                    onOpenFilters = { drawerOpen = true },
                    onDismissFilters = { drawerOpen = false },
                    onApplyFilters = {},
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("search-filter-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("filter-option-region-all").assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithTag("search-filter-button").assertIsFocused()
    }

    @Test
    fun filterDrawerUsesStandardEndPanelGeometry() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(regionFilter()),
                        filterDrawerOpen = true,
                    ),
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

        val displayMetrics = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics
        val drawer = composeRule.onNodeWithTag("search-filter-drawer")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(500f * displayMetrics.density, drawer.width, displayMetrics.density)
        assertEquals(displayMetrics.widthPixels.toFloat(), drawer.right, 1f)
    }

    @Test
    fun filterDrawerCoversFullViewportOutsidePaddedSearchContent() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("filter-test-app-root"),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 44.dp,
                                top = 100.dp,
                                end = 44.dp,
                                bottom = 24.dp,
                            )
                            .testTag("filter-test-padded-content"),
                    ) {
                        SearchScreen(
                            session = session(),
                            state = state(
                                filters = listOf(regionFilter()),
                                filterDrawerOpen = true,
                            ),
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
        }

        val appRoot = composeRule.onNodeWithTag("filter-test-app-root")
            .fetchSemanticsNode()
        val paddedContent = composeRule.onNodeWithTag("filter-test-padded-content")
            .fetchSemanticsNode()
        val drawer = composeRule.onNodeWithTag("search-filter-drawer")
            .fetchSemanticsNode()
        val appRootPosition = appRoot.positionOnScreen
        val drawerPosition = drawer.positionOnScreen

        assertTrue(
            "Test content must retain the destination top inset",
            paddedContent.boundsInRoot.top > appRoot.boundsInRoot.top,
        )
        assertEquals(appRootPosition.y, drawerPosition.y, 1f)
        assertEquals(
            appRootPosition.y + appRoot.boundsInRoot.height,
            drawerPosition.y + drawer.boundsInRoot.height,
            1f,
        )
        assertEquals(
            appRootPosition.x + appRoot.boundsInRoot.width,
            drawerPosition.x + drawer.boundsInRoot.width,
            1f,
        )
    }

    @Test
    fun filterDrawerBackInvokesDismissExactlyOnce() {
        var dismissCount = 0
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = listOf(regionFilter()),
                        filterDrawerOpen = true,
                    ),
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onResultFocused = {},
                    onPlay = {},
                    onOpenFilters = {},
                    onDismissFilters = { dismissCount += 1 },
                    onApplyFilters = {},
                    onClearFilters = {},
                )
            }
        }

        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(1, dismissCount) }
    }

    @Test
    fun filterDrawerScrollsToFirstFocusableDynamicField() {
        val emptyFields = List(18) { index ->
            SearchFilterDefinition(
                key = "empty-$index",
                label = "空字段 $index",
                type = SearchFilterType.Select,
            )
        }
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        filters = emptyFields + textFilter(),
                        filterDrawerOpen = true,
                    ),
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

        composeRule.onNodeWithTag("filter-input-title").assertIsFocused()
    }

    @Test
    fun deepViewportRestoresFocusedResult() {
        val results = (1..30).map { result("v$it") }
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        results = results,
                        focusedResultId = "v25",
                        gridViewport = GridViewportSnapshot(24, 0),
                    ),
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onResultFocused = {},
                    onGridViewportChanged = {},
                    onPlay = {},
                    onOpenFilters = {},
                    onDismissFilters = {},
                    onApplyFilters = {},
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("network-result-v25").assertIsFocused()
    }

    @Test
    fun prefetchZoneRequestsOneNextPage() {
        var loads = 0
        val results = (1..20).map { result("v$it") }
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(results = results, hasNext = true),
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = { loads += 1 },
                    onResultFocused = {},
                    onGridViewportChanged = {},
                    onPlay = {},
                    onOpenFilters = {},
                    onDismissFilters = {},
                    onApplyFilters = {},
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("search-results-grid").performScrollToIndex(19)
        composeRule.onNodeWithTag("network-result-v20")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.runOnIdle {
            assertEquals(1, loads)
        }
    }

    @Test
    fun finalPageDoesNotPrefetchOrRenderPagingFooter() {
        var loads = 0
        val results = (1..20).map { result("v$it") }
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(results = results, hasNext = false),
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = { loads += 1 },
                    onResultFocused = {},
                    onGridViewportChanged = {},
                    onPlay = {},
                    onOpenFilters = {},
                    onDismissFilters = {},
                    onApplyFilters = {},
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("search-results-grid").performScrollToIndex(19)
        composeRule.onNodeWithTag("network-result-v20")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("search-load-more-loading").assertDoesNotExist()
        composeRule.onNodeWithTag("search-load-more-retry").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, loads)
        }
    }

    @Test
    fun loadMoreFailureKeepsResultsAndOffersFocusableRetry() {
        var loads = 0
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        results = (1..20).map { result("v$it") },
                        hasNext = true,
                        loadMoreError = AppError.Offline,
                    ),
                    onRefreshIndexers = {},
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = { loads += 1 },
                    onResultFocused = {},
                    onGridViewportChanged = {},
                    onPlay = {},
                    onOpenFilters = {},
                    onDismissFilters = {},
                    onApplyFilters = {},
                    onClearFilters = {},
                )
            }
        }

        composeRule.onNodeWithTag("search-results-grid").performScrollToIndex(20)
        composeRule.onNodeWithTag("network-result-v20").assertExists()
        composeRule.onNodeWithTag("search-load-more-retry")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, loads)
        }
    }
}

private fun state(
    indexerIconPath: String? = null,
    coverRatio: Float = 16f / 9f,
    filters: List<SearchFilterDefinition> = emptyList(),
    appliedFilters: Map<String, SearchFilterValue> = emptyMap(),
    filterDrawerOpen: Boolean = false,
    results: List<NetworkSearchResult> = listOf(result("v1")),
    focusedResultId: String? = null,
    gridViewport: GridViewportSnapshot = GridViewportSnapshot.Top,
    hasNext: Boolean = false,
    isLoadingMore: Boolean = false,
    loadMoreError: AppError? = null,
): SearchUiState.Content {
    val profile = IndexerSourceProfile(
        indexer = indexer(indexerIconPath),
        pageSize = 20,
        keywordRequired = true,
        coverRatio = coverRatio,
        filters = filters,
    )
    return SearchUiState.Content(
        profiles = listOf(profile),
        selectedIndexerId = 11,
        query = "星际",
        submittedKeyword = "星际",
        appliedFilters = appliedFilters,
        filterDrawerOpen = filterDrawerOpen,
        focusedResultId = focusedResultId,
        gridViewport = gridViewport,
        results = SearchResultsState.Content(
            items = results,
            total = results.size,
            pageNumber = 1,
            hasNext = hasNext,
            isLoadingMore = isLoadingMore,
            loadMoreError = loadMoreError,
        ),
    )
}

private fun result(id: String) = NetworkSearchResult(
    id = id,
    title = "视频$id",
    coverPath = null,
    rating = 8.6,
    category = "科幻",
    uploader = null,
    uploadedAt = null,
)

private fun regionFilter() = SearchFilterDefinition(
    key = "region",
    label = "地区",
    type = SearchFilterType.Select,
    options = listOf(
        SearchFilterOption("cn", "中国"),
        SearchFilterOption("jp", "日本"),
    ),
)

private fun sortFilter() = SearchFilterDefinition(
    key = "sort",
    label = "排序",
    type = SearchFilterType.Radio,
    options = listOf(
        SearchFilterOption("updated", "更新"),
        SearchFilterOption("clicks", "点击"),
    ),
)

private fun genreFilter() = SearchFilterDefinition(
    key = "genre",
    label = "类型",
    type = SearchFilterType.Checkbox,
    options = listOf(
        SearchFilterOption("fantasy", "奇幻"),
        SearchFilterOption("scifi", "科幻"),
    ),
)

private fun textFilter() = SearchFilterDefinition(
    key = "title",
    label = "标题",
    type = SearchFilterType.Text,
)

private fun indexer(iconPath: String? = null) = NetworkIndexer(11, "星海站", iconPath)

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
