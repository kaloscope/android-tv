package org.kaloscope.tv.feature.search

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import org.kaloscope.tv.core.model.SearchFilterDefinition
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.player.PlaybackOrigin
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.core.model.TvSettings
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
        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `initial load consumes complete available profile catalog`() = runTest {
        val repository = FakeSearchRepository(
            availableProfiles = AppResult.Success(
                listOf(profile(indexerId = 11), profile(indexerId = 12)),
            ),
        )
        val coordinator = coordinator(repository)

        coordinator.load(session())

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals(listOf(11L, 12L), state.profiles.map { it.indexer.id })
        assertEquals(11L, state.selectedIndexerId)
    }

    @Test
    fun `switching site clears query filters results and focus`() = runTest {
        val repository = FakeSearchRepository(
            availableProfiles = AppResult.Success(
                listOf(
                    profile(indexerId = 11, filters = listOf(regionFilter())),
                    profile(indexerId = 12),
                ),
            ),
            pages = mutableListOf(AppResult.Success(page("v1"))),
        )
        val coordinator = coordinator(repository)
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.applyFilters(
            session(),
            mapOf("region" to SearchFilterValue.Scalar("cn")),
        )
        coordinator.rememberFocusedResult("v1")

        coordinator.selectIndexer(session(), 12)

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals("", state.query)
        assertTrue(state.appliedFilters.isEmpty())
        assertEquals(SearchResultsState.AwaitingQuery, state.results)
        assertNull(state.focusedResultId)
    }

    @Test
    fun `paging reuses committed filters`() = runTest {
        val repository = FakeSearchRepository(
            availableProfiles = AppResult.Success(
                listOf(profile(filters = listOf(regionFilter()))),
            ),
            pages = mutableListOf(
                AppResult.Success(page("v1", pageNumber = 1, hasNext = true)),
                AppResult.Success(page("v2", pageNumber = 2, hasNext = false)),
            ),
        )
        val coordinator = coordinator(repository)
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.applyFilters(
            session(),
            mapOf("region" to SearchFilterValue.Scalar("cn")),
        )

        coordinator.loadNext(session())

        assertEquals(2, repository.searchFilters.size)
        assertEquals(repository.searchFilters[0], repository.searchFilters[1])
    }

    @Test
    fun `dismissing filters closes drawer without searching`() = runTest {
        val repository = FakeSearchRepository(
            availableProfiles = AppResult.Success(listOf(profile())),
        )
        val coordinator = coordinator(repository)
        coordinator.load(session())

        coordinator.openFilters()
        coordinator.dismissFilters()

        val state = coordinator.state.value as SearchUiState.Content
        assertFalse(state.filterDrawerOpen)
        assertTrue(repository.searchCalls.isEmpty())
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

    @Test
    fun `playback resolution follows persisted TV settings`() = runTest {
        val store = PlaybackRequestStore()
        val repository = FakeSearchRepository(
            pages = mutableListOf(AppResult.Success(page("v1"))),
            playback = AppResult.Success(playback()),
        )
        val coordinator = SearchCoordinator(repository, store) { "settings-request" }
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.search(session())

        coordinator.play(
            session = session(),
            resultId = "v1",
            settings = TvSettings(transcodeResolution = TranscodeResolution.P720),
        )

        assertEquals(TranscodeResolution.P720, repository.preferredDefinition)
        val request = store.get("settings-request") as PlaybackRequest.NetworkVideo
        assertEquals(TranscodeResolution.P720, request.preferredDefinition)
    }
}

private class FakeSearchRepository(
    private val indexers: AppResult<List<NetworkIndexer>> =
        AppResult.Success(listOf(indexer())),
    private val profile: AppResult<IndexerSourceProfile> =
        AppResult.Success(profile()),
    private val pages: MutableList<AppResult<NetworkSearchPage>> = mutableListOf(),
    private val playback: AppResult<NetworkPlaybackSource> =
        AppResult.Failure(AppError.NotFound),
    private val availableProfiles: AppResult<List<IndexerSourceProfile>>? = null,
) : SearchRepository {
    val searchCalls = mutableListOf<SearchCall>()
    val searchFilters = mutableListOf<Map<String, SearchFilterValue>>()
    var preferredDefinition: TranscodeResolution? = null

    override suspend fun getAvailableProfiles(
        session: Session,
    ): AppResult<List<IndexerSourceProfile>> {
        availableProfiles?.let { return it }
        val loadedIndexers = when (indexers) {
            is AppResult.Failure -> return indexers
            is AppResult.Success -> indexers.value
        }
        val loadedProfile = when (profile) {
            is AppResult.Failure -> return profile
            is AppResult.Success -> profile.value
        }
        return AppResult.Success(
            loadedIndexers.map { indexer ->
                loadedProfile.copy(indexer = indexer)
            },
        )
    }

    override suspend fun search(
        session: Session,
        profile: IndexerSourceProfile,
        keyword: String,
        filters: Map<String, SearchFilterValue>,
        pageNumber: Int,
    ): AppResult<NetworkSearchPage> {
        searchCalls += SearchCall(keyword, filters, pageNumber)
        searchFilters += filters
        return pages.removeAt(0)
    }

    override suspend fun resolvePlayback(
        session: Session,
        indexerId: Long,
        result: NetworkSearchResult,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource> {
        this.preferredDefinition = preferredDefinition
        return playback
    }

    override suspend fun resolveChapter(
        session: Session,
        source: NetworkPlaybackSource,
        chapterIndex: Int,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource> = error("Not used")
}

private data class SearchCall(
    val keyword: String,
    val filters: Map<String, SearchFilterValue>,
    val pageNumber: Int,
)

private fun coordinator(repository: SearchRepository) =
    SearchCoordinator(repository, PlaybackRequestStore()) { "request-id" }

private fun indexer() = NetworkIndexer(11, "星海站", null)

private fun profile(
    indexerId: Long = 11,
    filters: List<SearchFilterDefinition> = emptyList(),
) = IndexerSourceProfile(
    indexer = NetworkIndexer(indexerId, "站点$indexerId", null),
    pageSize = 20,
    keywordRequired = true,
    filters = filters,
)

private fun regionFilter() = SearchFilterDefinition(
    key = "region",
    label = "地区",
    type = SearchFilterType.Select,
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
