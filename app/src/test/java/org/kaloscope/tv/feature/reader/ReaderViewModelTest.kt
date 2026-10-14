package org.kaloscope.tv.feature.reader

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.core.reader.ReaderRequestStore
import org.kaloscope.tv.test.StubReaderContentLoader

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val request = ReaderRequest.Image(
        requestId = "reader-1",
        serverId = "server-id",
        content = imageContent(0),
        settings = ImageReaderSettings(),
        chapterOrder = ReaderChapterOrder.Ascending,
    )
    private lateinit var loader: PendingReaderContentLoader
    private lateinit var requestStore: ReaderRequestStore
    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        loader = PendingReaderContentLoader()
        requestStore = ReaderRequestStore().apply { put(request) }
        viewModel = ReaderViewModel(requestStore, loader)
        viewModel.load(request.requestId, session())
    }

    @After
    fun tearDown() {
        val currentRequestId = (viewModel.uiState.value as? ReaderUiState.Active)?.requestId
        viewModel.close(currentRequestId ?: request.requestId)
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test
    fun `reselecting current chapter preserves pending pagination`() = runTest(dispatcher) {
        viewModel.loadMoreImages(session())
        runCurrent()

        viewModel.selectChapter(session(), 0)
        runCurrent()

        assertFalse(loader.pageCancelled)
        assertEquals(listOf(request.content), loader.pageRequests)
        assertTrue(loader.chapterRequests.isEmpty())
        loader.pageResult.complete(AppResult.Success(nextPage()))
        runCurrent()

        val state = viewModel.uiState.value as ReaderUiState.Image
        assertEquals(request.content.images + "next-page.jpg", state.content.images)
        assertEquals(0L, state.contentRevision)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `invalid chapter indices preserve pending pagination`() = runTest(dispatcher) {
        viewModel.loadMoreImages(session())
        runCurrent()

        for (chapterIndex in listOf(-1, request.content.chapters.size)) {
            viewModel.selectChapter(session(), chapterIndex)
            runCurrent()
            assertFalse(loader.pageCancelled)
        }

        assertEquals(listOf(request.content), loader.pageRequests)
        assertTrue(loader.chapterRequests.isEmpty())
        loader.pageResult.complete(AppResult.Success(nextPage()))
        runCurrent()

        val state = viewModel.uiState.value as ReaderUiState.Image
        assertEquals(request.content.images + "next-page.jpg", state.content.images)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `selecting another chapter cancels pending pagination`() = runTest(dispatcher) {
        viewModel.loadMoreImages(session())
        runCurrent()

        viewModel.selectChapter(session(), 1)
        runCurrent()

        assertTrue(loader.pageCancelled)
        assertEquals(listOf(1), loader.chapterRequests)
        val loading = viewModel.uiState.value as ReaderUiState.Image
        assertTrue(loading.isChapterLoading)
        assertFalse(loading.isLoadingMore)
        val replacement = imageContent(1)
        loader.chapterResult.complete(AppResult.Success(replacement))
        runCurrent()
        loader.pageResult.complete(AppResult.Success(nextPage()))
        runCurrent()

        val state = viewModel.uiState.value as ReaderUiState.Image
        assertEquals(replacement, state.content)
        assertEquals(1L, state.contentRevision)
        assertFalse(state.isChapterLoading)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `closing during chapter loading cancels the request and stays idle`() = runTest(dispatcher) {
        viewModel.selectChapter(session(), 1)
        runCurrent()
        assertTrue((viewModel.uiState.value as ReaderUiState.Image).isChapterLoading)

        viewModel.close(request.requestId)
        runCurrent()

        assertTrue(loader.chapterCancelled)
        assertEquals(ReaderUiState.Idle, viewModel.uiState.value)
        loader.chapterResult.complete(AppResult.Success(imageContent(1)))
        runCurrent()
        assertEquals(ReaderUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `late close of previous reader preserves new chapter loading`() = runTest(dispatcher) {
        val nextRequest = request.copy(requestId = "reader-2")
        requestStore.put(nextRequest)
        viewModel.load(nextRequest.requestId, session())
        viewModel.selectChapter(session(), 1)
        runCurrent()

        viewModel.close(request.requestId)
        runCurrent()

        assertFalse(loader.chapterCancelled)
        assertNull(requestStore.get(request.requestId))
        assertEquals(nextRequest, requestStore.get(nextRequest.requestId))
        val loading = viewModel.uiState.value as ReaderUiState.Image
        assertEquals(nextRequest.requestId, loading.requestId)
        assertTrue(loading.isChapterLoading)

        val replacement = imageContent(1)
        loader.chapterResult.complete(AppResult.Success(replacement))
        runCurrent()
        val loaded = viewModel.uiState.value as ReaderUiState.Image
        assertEquals(replacement, loaded.content)
        assertEquals(1L, loaded.contentRevision)
        assertFalse(loaded.isChapterLoading)

        // The stale close must also retain the loaded ID so recomposition cannot reset content.
        viewModel.load(nextRequest.requestId, session())
        assertEquals(loaded, viewModel.uiState.value)
    }

    @Test
    fun `late close of previous reader preserves new pagination`() = runTest(dispatcher) {
        val nextRequest = request.copy(requestId = "reader-2")
        requestStore.put(nextRequest)
        viewModel.load(nextRequest.requestId, session())
        viewModel.loadMoreImages(session())
        runCurrent()

        viewModel.close(request.requestId)
        runCurrent()

        assertFalse(loader.pageCancelled)
        assertNull(requestStore.get(request.requestId))
        assertEquals(nextRequest, requestStore.get(nextRequest.requestId))
        val loading = viewModel.uiState.value as ReaderUiState.Image
        assertEquals(nextRequest.requestId, loading.requestId)
        assertTrue(loading.isLoadingMore)

        loader.pageResult.complete(AppResult.Success(nextPage()))
        runCurrent()
        val loaded = viewModel.uiState.value as ReaderUiState.Image
        assertEquals(nextRequest.content.images + "next-page.jpg", loaded.content.images)
        assertFalse(loaded.isLoadingMore)
        viewModel.load(nextRequest.requestId, session())
        assertEquals(loaded, viewModel.uiState.value)
    }
}

private class PendingReaderContentLoader : StubReaderContentLoader() {
    val pageResult = CompletableDeferred<AppResult<ReaderImagePage>>()
    val chapterResult = CompletableDeferred<AppResult<ReaderContent>>()
    val pageRequests = mutableListOf<ReaderImageContent>()
    val chapterRequests = mutableListOf<Int>()
    var pageCancelled = false
    var chapterCancelled = false

    override suspend fun resolveChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent> {
        chapterRequests += chapterIndex
        return try {
            chapterResult.await()
        } catch (error: CancellationException) {
            chapterCancelled = true
            throw error
        }
    }

    override suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
        previous: Boolean,
    ): AppResult<ReaderImagePage> {
        pageRequests += content
        return try {
            pageResult.await()
        } catch (error: CancellationException) {
            pageCancelled = true
            throw error
        }
    }
}

private fun imageContent(chapterIndex: Int) = ReaderImageContent.network(
    indexerId = 11,
    resourceId = "comic-1",
    chapterId = "c$chapterIndex",
    title = "Chapter $chapterIndex",
    images = listOf("chapter-$chapterIndex.jpg"),
    imageCount = 3,
    chapters = listOf(ReaderChapter("c0", "Chapter 0"), ReaderChapter("c1", "Chapter 1")),
    selectedChapterIndex = chapterIndex,
)

private fun nextPage() = ReaderImagePage(
    images = listOf("next-page.jpg"),
    imageCount = 3,
    exhausted = false,
)

private fun session() = Session(
    server = SavedServer("server-id", "Test server", "https://server.example"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
