package org.kaloscope.tv.feature.search

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.ImagePageDirection
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ImageZoomMode
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.ResolvedNetworkResource
import org.kaloscope.tv.core.model.TextReaderFont
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.TextReaderTheme
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.SearchFilterDefinition
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.player.PlaybackOrigin
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.core.player.TranscodeQuality
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.core.reader.ReaderRequestStore
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.data.search.SearchRepository
import org.kaloscope.tv.data.search.NetworkResourceRepository

class SearchCoordinatorTest {
    @Test
    fun `viewport is remembered for the current search dataset`() = runTest {
        val coordinator = coordinator(
            FakeSearchRepository(
                pages = mutableListOf(AppResult.Success(page("v1"))),
            ),
        )
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.search(session())

        coordinator.rememberGridViewport(GridViewportSnapshot(18, 24))

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals(GridViewportSnapshot(18, 24), state.gridViewport)
    }

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
        coordinator.rememberGridViewport(GridViewportSnapshot(8, 12))

        coordinator.selectIndexer(session(), 12)

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals("", state.query)
        assertTrue(state.appliedFilters.isEmpty())
        assertEquals(SearchResultsState.AwaitingQuery, state.results)
        assertNull(state.focusedResultId)
        assertEquals(GridViewportSnapshot.Top, state.gridViewport)
    }

    @Test
    fun `submitting a new search clears viewport and focus`() = runTest {
        val coordinator = coordinator(
            FakeSearchRepository(
                pages = mutableListOf(
                    AppResult.Success(page("v1")),
                    AppResult.Success(page("v2")),
                ),
            ),
        )
        coordinator.load(session())
        coordinator.updateQuery("旧关键词")
        coordinator.search(session())
        coordinator.rememberFocusedResult("v1")
        coordinator.rememberGridViewport(GridViewportSnapshot(9, 16))

        coordinator.updateQuery("新关键词")
        coordinator.search(session())

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals(GridViewportSnapshot.Top, state.gridViewport)
        assertNull(state.focusedResultId)
        assertEquals(listOf("v2"), state.results.items.map { it.id })
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
    fun `load more failure preserves content and retries the same page`() = runTest {
        val repository = FakeSearchRepository(
            pages = mutableListOf(
                AppResult.Success(page("v1", pageNumber = 1, hasNext = true)),
                AppResult.Failure(AppError.Offline),
                AppResult.Success(page("v2", pageNumber = 2, hasNext = false)),
            ),
        )
        val coordinator = coordinator(repository)
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.search(session())

        coordinator.loadNext(session())

        val failed = coordinator.state.value as SearchUiState.Content
        val failedResults = failed.results as SearchResultsState.Content
        assertEquals(listOf("v1"), failedResults.items.map { it.id })
        assertEquals(1, failedResults.pageNumber)
        assertEquals(AppError.Offline, failedResults.loadMoreError)

        coordinator.loadNext(session())

        val recovered = coordinator.state.value as SearchUiState.Content
        val recoveredResults = recovered.results as SearchResultsState.Content
        assertEquals(listOf("v1", "v2"), recoveredResults.items.map { it.id })
        assertEquals(listOf(2, 2), repository.searchCalls.drop(1).map { it.pageNumber })
    }

    @Test
    fun `final search page ignores load more`() = runTest {
        val repository = FakeSearchRepository(
            pages = mutableListOf(AppResult.Success(page("v1", hasNext = false))),
        )
        val coordinator = coordinator(repository)
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.search(session())

        coordinator.loadNext(session())

        assertEquals(listOf(1), repository.searchCalls.map { it.pageNumber })
    }

    @Test
    fun `cancelled load more clears transient loading state`() = runTest {
        val pagingStarted = CompletableDeferred<Unit>()
        val pagingResult = CompletableDeferred<AppResult<NetworkSearchPage>>()
        val repository = FakeSearchRepository(
            pages = mutableListOf(
                AppResult.Success(page("v1", pageNumber = 1, hasNext = true)),
            ),
            pagingStarted = pagingStarted,
            pagingResult = pagingResult,
        )
        val coordinator = coordinator(repository)
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.search(session())

        val pagingJob = launch { coordinator.loadNext(session()) }
        pagingStarted.await()
        val loading = coordinator.state.value as SearchUiState.Content
        assertTrue((loading.results as SearchResultsState.Content).isLoadingMore)

        pagingJob.cancelAndJoin()

        val cancelled = coordinator.state.value as SearchUiState.Content
        assertFalse((cancelled.results as SearchResultsState.Content).isLoadingMore)
    }

    @Test
    fun `result click resolves details and creates direct network request`() = runTest {
        val store = PlaybackRequestStore()
        val repository = FakeSearchRepository(
            pages = mutableListOf(AppResult.Success(page("v1"))),
        )
        val resourceRepository = FakeNetworkResourceRepository(
            resolution = AppResult.Success(ResolvedNetworkResource.Video(playback())),
        )
        val coordinator = SearchCoordinator(
            repository = repository,
            requestStore = store,
            requestIdFactory = { "network-request" },
            networkResourceRepository = resourceRepository,
        )
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
        )
        val coordinator = coordinator(
            repository = repository,
            resourceRepository = FakeNetworkResourceRepository(
                resolution = AppResult.Failure(AppError.Offline),
            ),
        )
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.search(session())

        coordinator.play(session(), "v1")

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals(listOf("v1"), state.results.items.map { it.id })
        assertEquals(AppError.Offline, state.playbackError)
    }

    @Test
    fun `network definition stays independent from local transcode quality`() = runTest {
        val store = PlaybackRequestStore()
        val repository = FakeSearchRepository(
            pages = mutableListOf(AppResult.Success(page("v1"))),
        )
        val resourceRepository = FakeNetworkResourceRepository(
            resolution = AppResult.Success(ResolvedNetworkResource.Video(playback())),
        )
        val coordinator = SearchCoordinator(
            repository = repository,
            requestStore = store,
            requestIdFactory = { "settings-request" },
            networkResourceRepository = resourceRepository,
        )
        coordinator.load(session())
        coordinator.updateQuery("星际")
        coordinator.search(session())

        coordinator.play(
            session = session(),
            resultId = "v1",
            settings = TvSettings(transcodeQuality = TranscodeQuality.Low),
        )

        assertEquals(TranscodeResolution.P1080, resourceRepository.preferredDefinition)
        val request = store.get("settings-request") as PlaybackRequest.NetworkVideo
        assertEquals(TranscodeResolution.P1080, request.preferredDefinition)
    }

    @Test
    fun `image result creates reader request with a settings snapshot`() = runTest {
        val playbackStore = PlaybackRequestStore()
        val readerStore = ReaderRequestStore()
        val imageSettings = ImageReaderSettings(
            readMode = ImageReadMode.Paged,
            zoomMode = ImageZoomMode.FitHeight,
            pageDirection = ImagePageDirection.Left,
        )
        val resourceRepository = FakeNetworkResourceRepository(
            resolution = AppResult.Success(
                ResolvedNetworkResource.Image(
                    ReaderImageContent.network(
                        indexerId = 11,
                        resourceId = "v1",
                        title = "Comic",
                        images = listOf("one.jpg"),
                        imageCount = 1,
                    ),
                ),
            ),
        )
        val repository = FakeSearchRepository(
            pages = mutableListOf(AppResult.Success(page("v1"))),
        )
        val coordinator = SearchCoordinator(
            repository = repository,
            requestStore = playbackStore,
            requestIdFactory = { "reader-request" },
            networkResourceRepository = resourceRepository,
            readerRequestStore = readerStore,
        )
        coordinator.load(session())
        coordinator.updateQuery("comic")
        coordinator.search(session())

        coordinator.play(
            session(),
            "v1",
            TvSettings(
                imageReader = imageSettings,
                readerChapterOrder = ReaderChapterOrder.Descending,
            ),
        )

        val state = coordinator.state.value as SearchUiState.Content
        assertEquals(
            SearchPendingDestination.Reader("reader-request"),
            state.pendingDestination,
        )
        val request = readerStore.get("reader-request") as ReaderRequest.Image
        assertEquals(imageSettings, request.settings)
        assertEquals(ReaderChapterOrder.Descending, request.chapterOrder)
        assertNull(playbackStore.get("reader-request"))
    }

    @Test
    fun `text destination is consumed exactly once`() = runTest {
        val readerStore = ReaderRequestStore()
        val textSettings = TextReaderSettings(
            theme = TextReaderTheme.Black,
            font = TextReaderFont.Serif,
            fontSizeSp = 34,
        )
        val repository = FakeSearchRepository(
            pages = mutableListOf(AppResult.Success(page("v1"))),
        )
        val coordinator = SearchCoordinator(
            repository = repository,
            requestStore = PlaybackRequestStore(),
            requestIdFactory = { "text-request" },
            networkResourceRepository = FakeNetworkResourceRepository(
                resolution = AppResult.Success(
                    ResolvedNetworkResource.Text(
                        ReaderTextContent.network(
                            indexerId = 11,
                            resourceId = "v1",
                            title = "Book",
                            text = "Body",
                        ),
                    ),
                ),
            ),
            readerRequestStore = readerStore,
        )
        coordinator.load(session())
        coordinator.updateQuery("book")
        coordinator.search(session())
        coordinator.play(
            session(),
            "v1",
            TvSettings(textReader = textSettings),
        )

        val request = readerStore.get("text-request") as ReaderRequest.Text
        assertEquals(textSettings, request.settings)

        coordinator.consumeDestination("text-request")
        coordinator.consumeDestination("text-request")

        val state = coordinator.state.value as SearchUiState.Content
        assertNull(state.pendingDestination)
        assertTrue(readerStore.get("text-request") is ReaderRequest.Text)
    }

    @Test
    fun `cancelled resource resolution clears the resolving card`() = runTest {
        val resolutionStarted = CompletableDeferred<Unit>()
        val resolutionResult = CompletableDeferred<AppResult<ResolvedNetworkResource>>()
        val coordinator = SearchCoordinator(
            repository = FakeSearchRepository(
                pages = mutableListOf(AppResult.Success(page("v1"))),
            ),
            requestStore = PlaybackRequestStore(),
            networkResourceRepository = FakeNetworkResourceRepository(
                resolutionStarted = resolutionStarted,
                deferredResolution = resolutionResult,
            ),
        )
        coordinator.load(session())
        coordinator.updateQuery("video")
        coordinator.search(session())

        val job = launch { coordinator.play(session(), "v1") }
        resolutionStarted.await()
        assertEquals(
            "v1",
            (coordinator.state.value as SearchUiState.Content).resolvingResultId,
        )

        job.cancelAndJoin()

        assertNull((coordinator.state.value as SearchUiState.Content).resolvingResultId)
    }

    @Test
    fun `explicit cancellation prevents a delayed playback destination`() = runTest {
        val resolutionStarted = CompletableDeferred<Unit>()
        val resolutionResult = CompletableDeferred<AppResult<ResolvedNetworkResource>>()
        val requestStore = PlaybackRequestStore()
        val coordinator = SearchCoordinator(
            repository = FakeSearchRepository(
                pages = mutableListOf(AppResult.Success(page("v1"))),
            ),
            requestStore = requestStore,
            networkResourceRepository = FakeNetworkResourceRepository(
                resolutionStarted = resolutionStarted,
                deferredResolution = resolutionResult,
            ),
            requestIdFactory = { "cancelled-request" },
        )
        coordinator.load(session())
        coordinator.updateQuery("video")
        coordinator.search(session())

        val job = launch { coordinator.play(session(), "v1") }
        resolutionStarted.await()

        assertTrue(coordinator.cancelResolution())
        assertNull((coordinator.state.value as SearchUiState.Content).resolvingResultId)

        resolutionResult.complete(
            AppResult.Success(ResolvedNetworkResource.Video(playback())),
        )
        job.join()

        val state = coordinator.state.value as SearchUiState.Content
        assertNull(state.pendingDestination)
        assertNull(requestStore.get("cancelled-request"))
    }
}

private class FakeNetworkResourceRepository(
    private val resolution: AppResult<ResolvedNetworkResource> =
        AppResult.Failure(AppError.NotFound),
    private val resolutionStarted: CompletableDeferred<Unit>? = null,
    private val deferredResolution: CompletableDeferred<AppResult<ResolvedNetworkResource>>? = null,
) : NetworkResourceRepository {
    var preferredDefinition: TranscodeResolution? = null
        private set

    override suspend fun resolveResource(
        session: Session,
        indexerId: Long,
        result: NetworkSearchResult,
        preferredDefinition: TranscodeResolution,
    ): AppResult<ResolvedNetworkResource> {
        this.preferredDefinition = preferredDefinition
        resolutionStarted?.complete(Unit)
        return deferredResolution?.await() ?: resolution
    }

    override suspend fun resolveVideoChapter(
        session: Session,
        source: NetworkPlaybackSource,
        chapterIndex: Int,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource> = error("Not used")

    override suspend fun resolveReaderChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent> = error("Not used")

    override suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
    ): AppResult<ReaderImagePage> = error("Not used")
}

private class FakeSearchRepository(
    private val indexers: AppResult<List<NetworkIndexer>> =
        AppResult.Success(listOf(indexer())),
    private val profile: AppResult<IndexerSourceProfile> =
        AppResult.Success(profile()),
    private val pages: MutableList<AppResult<NetworkSearchPage>> = mutableListOf(),
    private val availableProfiles: AppResult<List<IndexerSourceProfile>>? = null,
    private val pagingStarted: CompletableDeferred<Unit>? = null,
    private val pagingResult: CompletableDeferred<AppResult<NetworkSearchPage>>? = null,
) : SearchRepository {
    val searchCalls = mutableListOf<SearchCall>()
    val searchFilters = mutableListOf<Map<String, SearchFilterValue>>()

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
        if (pageNumber > 1 && pagingResult != null) {
            pagingStarted?.complete(Unit)
            return pagingResult.await()
        }
        return pages.removeAt(0)
    }

}

private data class SearchCall(
    val keyword: String,
    val filters: Map<String, SearchFilterValue>,
    val pageNumber: Int,
)

private fun coordinator(
    repository: SearchRepository,
    resourceRepository: NetworkResourceRepository = FakeNetworkResourceRepository(),
) =
    SearchCoordinator(
        repository = repository,
        requestStore = PlaybackRequestStore(),
        requestIdFactory = { "request-id" },
        networkResourceRepository = resourceRepository,
    )

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
