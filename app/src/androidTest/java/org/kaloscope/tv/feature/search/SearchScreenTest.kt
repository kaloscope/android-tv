package org.kaloscope.tv.feature.search

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
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

class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

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
    fun firstRealIndexerReceivesInitialFocus() {
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

        composeRule.onNodeWithTag("indexer-11").assertIsFocused()
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
    fun missingGraphIconKeepsIndexerInitial() {
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(results = emptyList()),
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
            text = "星",
            useUnmergedTree = true,
        ).assertExists()
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
    fun resolvingResultKeepsMetadataCardBounds() {
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
            text = "正在获取播放地址…",
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithText(
            text = "UP: Admin · 10 Hours Ago",
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun landscapeGridFitsThreeResultsPerRowAt1080p() {
        val width = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.widthPixels
        if (width != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = state(
                        results = listOf(
                            result("v1"),
                            result("v2"),
                            result("v3"),
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

        val firstTop = composeRule.onNodeWithTag("network-result-v1")
            .fetchSemanticsNode()
            .boundsInRoot.top
        val secondTop = composeRule.onNodeWithTag("network-result-v2")
            .fetchSemanticsNode()
            .boundsInRoot.top
        val thirdTop = composeRule.onNodeWithTag("network-result-v3")
            .fetchSemanticsNode()
            .boundsInRoot.top

        assertEquals(firstTop, secondTop, 0.5f)
        assertEquals(firstTop, thirdTop, 0.5f)
    }

    @Test
    fun emptyIndexerStateOffersRefresh() {
        var refreshes = 0
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
                )
            }
        }

        composeRule.onNodeWithTag("refresh-indexers")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, refreshes)
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
    fun searchActionsMatchSearchFieldHeightInWebUiOrder() {
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
            assertEquals(52f * density, bounds.width, 1f)
            assertEquals(52f * density, bounds.height, 1f)
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
        composeRule.onNodeWithTag("filter-option-region-cn").assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithTag("search-filter-button").assertIsFocused()
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
