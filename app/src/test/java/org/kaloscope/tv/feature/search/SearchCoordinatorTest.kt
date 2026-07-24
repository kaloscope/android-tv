package org.kaloscope.tv.feature.search

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.player.PlaybackOrigin
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.data.search.SearchRepository

class SearchCoordinatorTest {
    @Test
    fun `initial load selects first real indexer and awaits required keyword`() = runTest {
        val repository = FakeSearchRepository()
        val coordinator = coordinator(repository)

        coordinator.load(session())

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals(11L, state.selectedIndexerId)
        assertEquals(SearchResultsState.AwaitingQuery, state.results)
        assertTrue(state.source is SearchSourceState.Ready)
        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `required web authentication blocks search`() = runTest {
        val repository = FakeSearchRepository(
            profile = AppResult.Success(profile(webAuthRequired = true)),
        )
        val coordinator = coordinator(repository)

        coordinator.load(session())

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals(SearchSourceState.WebAuthRequired, state.source)
        assertEquals(SearchResultsState.AwaitingQuery, state.results)
    }

    @Test
    fun `retry after web authentication reloads source profile`() = runTest {
        val repository = FakeSearchRepository(
            profiles = mutableListOf(
                AppResult.Success(profile(webAuthRequired = true)),
                AppResult.Success(profile(webAuthRequired = false)),
            ),
        )
        val coordinator = coordinator(repository)
        coordinator.load(session())

        coordinator.retry(session())

        val state = coordinator.state.value as SearchUiState.Content
        assertTrue(state.source is SearchSourceState.Ready)
    }

    @Test
    fun `confirmed query loads first page and paging appends unique results`() = runTest {
        val repository = FakeSearchRepository(
            pages = mutableListOf(
                AppResult.Success(page("v1", pageNumber = 1, hasNext = true)),
                AppResult.Success(
                    NetworkSearchPage(
                        items = listOf(result("v1"), result("v2")),
                        total = 2,
                        pageNumber = 2,
                        pageSize = 20,
                        hasNext = false,
                    ),
                ),
            ),
        )
        val coordinator = coordinator(repository)
        coordinator.load(session())
        coordinator.updateQuery(" 星际 ")

        coordinator.search(session())
        coordinator.loadNext(session())

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals("星际", state.submittedKeyword)
        assertEquals(listOf("v1", "v2"), state.results.items.map { it.id })
        assertEquals(listOf("星际", "星际"), repository.searchCalls.map { it.keyword })
    }

    @Test
    fun `result click resolves details and creates direct network request`() = runTest {
        val store = PlaybackRequestStore()
        val repository = FakeSearchRepository(
            pages = mutableListOf(AppResult.Success(page("v1"))),
            playback = AppResult.Success(playback()),
        )
        val coordinator = SearchCoordinator(repository, store) { "network-request" }
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.search(session())

        coordinator.play(session(), "v1")

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals("network-request", state.pendingPlaybackRequestId)
        assertNull(state.resolvingResultId)
        val request = store.get("network-request") as PlaybackRequest.NetworkVideo
        assertEquals(PlaybackOrigin.NetworkSearch, request.origin)
        assertEquals("/_api/media/proxy?id=1", request.source.url)
    }

    @Test
    fun `details failure keeps results available for retry`() = runTest {
        val repository = FakeSearchRepository(
            pages = mutableListOf(AppResult.Success(page("v1"))),
            playback = AppResult.Failure(AppError.Offline),
        )
        val coordinator = coordinator(repository)
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.search(session())

        coordinator.play(session(), "v1")

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals(listOf("v1"), state.results.items.map { it.id })
        assertEquals(AppError.Offline, state.playbackError)
    }
}

private class FakeSearchRepository(
    private val indexers: AppResult<List<NetworkIndexer>> =
        AppResult.Success(listOf(indexer())),
    private val profile: AppResult<IndexerSourceProfile> =
        AppResult.Success(profile()),
    private val profiles: MutableList<AppResult<IndexerSourceProfile>> = mutableListOf(),
    private val pages: MutableList<AppResult<NetworkSearchPage>> = mutableListOf(),
    private val playback: AppResult<NetworkPlaybackSource> =
        AppResult.Failure(AppError.NotFound),
) : SearchRepository {
    val searchCalls = mutableListOf<SearchCall>()

    override suspend fun getIndexers(session: Session): AppResult<List<NetworkIndexer>> =
        indexers

    override suspend fun getProfile(
        session: Session,
        indexer: NetworkIndexer,
    ): AppResult<IndexerSourceProfile> =
        if (profiles.isEmpty()) profile else profiles.removeAt(0)

    override suspend fun search(
        session: Session,
        profile: IndexerSourceProfile,
        keyword: String,
        pageNumber: Int,
    ): AppResult<NetworkSearchPage> {
        searchCalls += SearchCall(keyword, pageNumber)
        return pages.removeAt(0)
    }

    override suspend fun resolvePlayback(
        session: Session,
        indexerId: Long,
        result: NetworkSearchResult,
    ): AppResult<NetworkPlaybackSource> = playback
}

private data class SearchCall(
    val keyword: String,
    val pageNumber: Int,
)

private fun coordinator(repository: SearchRepository) =
    SearchCoordinator(repository, PlaybackRequestStore()) { "request-id" }

private fun indexer() = NetworkIndexer(11, "星海站", null)

private fun profile(webAuthRequired: Boolean = false) = IndexerSourceProfile(
    indexer = indexer(),
    pageSize = 20,
    keywordRequired = true,
    webAuthRequired = webAuthRequired,
)

private fun page(
    id: String,
    pageNumber: Int = 1,
    hasNext: Boolean = false,
) = NetworkSearchPage(
    items = listOf(result(id)),
    total = 1,
    pageNumber = pageNumber,
    pageSize = 20,
    hasNext = hasNext,
)

private fun result(id: String) = NetworkSearchResult(
    id = id,
    title = "视频$id",
    coverPath = null,
    rating = null,
    category = null,
    uploader = null,
    uploadedAt = null,
)

private fun playback() = NetworkPlaybackSource(
    indexerId = 11,
    resourceId = "v1",
    title = "视频v1",
    url = "/_api/media/proxy?id=1",
    videoType = NetworkVideoType.Hls,
    danmakus = listOf(DanmakuComment("d1", "Ready", "scroll", null, 12_500)),
)

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
