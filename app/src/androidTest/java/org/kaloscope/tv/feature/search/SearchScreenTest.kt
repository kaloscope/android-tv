package org.kaloscope.tv.feature.search

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
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

        composeRule.onNodeWithText("星际").performImeAction()

        composeRule.runOnIdle {
            assertEquals(1, searches)
        }
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
    fun rightFromSearchFieldMovesThroughSearchAndFilterActions() {
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
        composeRule.onNodeWithTag("search-action-button").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("search-filter-button").assertIsFocused()
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
}

private fun state(
    filters: List<SearchFilterDefinition> = emptyList(),
    filterDrawerOpen: Boolean = false,
): SearchUiState {
    val profile = IndexerSourceProfile(
        indexer = indexer(),
        pageSize = 20,
        keywordRequired = true,
        filters = filters,
    )
    return SearchUiState.Content(
        profiles = listOf(profile),
        selectedIndexerId = 11,
        query = "星际",
        submittedKeyword = "星际",
        filterDrawerOpen = filterDrawerOpen,
        results = SearchResultsState.Content(
            items = listOf(
                NetworkSearchResult(
                    id = "v1",
                    title = "星际回声",
                    coverPath = null,
                    rating = 8.6,
                    category = "科幻",
                    uploader = null,
                    uploadedAt = null,
                ),
            ),
            total = 1,
            pageNumber = 1,
            hasNext = false,
        ),
    )
}

private fun regionFilter() = SearchFilterDefinition(
    key = "region",
    label = "地区",
    type = SearchFilterType.Select,
    options = listOf(
        SearchFilterOption("cn", "中国"),
        SearchFilterOption("jp", "日本"),
    ),
)

private fun indexer() = NetworkIndexer(11, "星海站", null)

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
