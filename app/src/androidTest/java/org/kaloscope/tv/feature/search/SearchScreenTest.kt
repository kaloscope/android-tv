package org.kaloscope.tv.feature.search

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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.SavedServer
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
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onResultFocused = {},
                    onPlay = {},
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
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onResultFocused = {},
                    onPlay = { selectedId = it },
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
    fun webAuthenticationPromptOffersRetry() {
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                SearchScreen(
                    session = session(),
                    state = (state() as SearchUiState.Content).copy(
                        source = SearchSourceState.WebAuthRequired,
                        results = SearchResultsState.AwaitingQuery,
                    ),
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = { retries += 1 },
                    onLoadMore = {},
                    onResultFocused = {},
                    onPlay = {},
                )
            }
        }

        composeRule.onNodeWithTag("indexer-auth-retry")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, retries)
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
                    onSelectIndexer = {},
                    onQueryChange = {},
                    onSearch = { searches += 1 },
                    onRetry = {},
                    onLoadMore = {},
                    onResultFocused = {},
                    onPlay = {},
                )
            }
        }

        composeRule.onNodeWithText("星际").performImeAction()

        composeRule.runOnIdle {
            assertEquals(1, searches)
        }
    }
}

private fun state(): SearchUiState = SearchUiState.Content(
    indexers = listOf(indexer()),
    selectedIndexerId = 11,
    source = SearchSourceState.Ready(
        IndexerSourceProfile(
            indexer = indexer(),
            pageSize = 20,
            keywordRequired = true,
            webAuthRequired = false,
        ),
    ),
    query = "星际",
    submittedKeyword = "星际",
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

private fun indexer() = NetworkIndexer(11, "星海站", null)

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
