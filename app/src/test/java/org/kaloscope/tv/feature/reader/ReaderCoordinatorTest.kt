package org.kaloscope.tv.feature.reader

import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.app.hasUnauthorized
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.network.networkCall
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.core.reader.ReaderRequestStore
import org.kaloscope.tv.data.reader.ReaderContentLoader
import org.kaloscope.tv.test.StubReaderContentLoader
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderCoordinatorTest {
    @Test
    fun `missing or wrong server request becomes an explicit error`() {
        val store = ReaderRequestStore()
        store.put(imageRequest(serverId = "other-server"))
        val coordinator = ReaderCoordinator(store, FakeReaderContentLoader())

        coordinator.load("missing", session())
        assertEquals(AppError.InvalidData("reader_request"), errorState(coordinator).error)

        coordinator.load("reader-1", session())
        assertEquals(AppError.InvalidData("reader_server"), errorState(coordinator).error)
    }

    @Test
    fun `chapter failure retains content position revision and session settings`() = runTest {
        val store = ReaderRequestStore().apply { put(imageRequest()) }
        val loader = FakeReaderContentLoader(
            chapterResult = AppResult.Failure(AppError.Offline),
        )
        val coordinator = ReaderCoordinator(store, loader)
        coordinator.load("reader-1", session())
        coordinator.updateImageSettings(ImageReaderSettings(readMode = ImageReadMode.Paged))
        val before = coordinator.state.value as ReaderUiState.Image

        coordinator.selectChapter(session(), 1)

        val after = coordinator.state.value as ReaderUiState.Image
        assertEquals(before.content, after.content)
        assertEquals(before.contentRevision, after.contentRevision)
        assertEquals(ImageReadMode.Paged, after.settings.readMode)
        assertEquals(AppError.Offline, after.chapterError)
        assertFalse(after.isChapterLoading)
    }

    @Test
    fun `cancelled chapter drops queued errors and clears loading without replacing content`() = runTest {
        val response = PendingNetworkResponse<ReaderContent>()
        val loader = object : ReaderContentLoader by FakeReaderContentLoader() {
            override suspend fun resolveChapter(
                session: Session,
                content: ReaderContent,
                chapterIndex: Int,
            ) = response.await()
        }
        val coordinator = ReaderCoordinator(
            ReaderRequestStore().apply { put(imageRequest()) },
            loader,
        )
        coordinator.load("reader-1", session())
        val original = coordinator.state.value
        var returnedNormally = false
        val job = launch {
            coordinator.selectChapter(session(), 1)
            returnedNormally = true
        }
        runCurrent()
        assertTrue((coordinator.state.value as ReaderUiState.Image).isChapterLoading)

        response.failUnauthorized()
        job.cancel()
        runCurrent()

        assertEquals(original, coordinator.state.value)
        assertFalse(coordinator.state.value.hasUnauthorized())
        assertFalse(returnedNormally)
    }

    @Test
    fun `active chapter retains queued authorization errors for root handling`() = runTest {
        val response = PendingNetworkResponse<ReaderContent>()
        val loader = object : ReaderContentLoader by FakeReaderContentLoader() {
            override suspend fun resolveChapter(
                session: Session,
                content: ReaderContent,
                chapterIndex: Int,
            ) = response.await()
        }
        val coordinator = ReaderCoordinator(
            ReaderRequestStore().apply { put(imageRequest()) },
            loader,
        )
        coordinator.load("reader-1", session())
        val original = coordinator.state.value as ReaderUiState.Image
        val job = launch { coordinator.selectChapter(session(), 1) }
        runCurrent()

        response.failUnauthorized()
        job.join()

        assertEquals(original.copy(chapterError = AppError.Unauthorized), coordinator.state.value)
        assertTrue(coordinator.state.value.hasUnauthorized())
    }

    @Test
    fun `chapter authorization errors cannot be dismissed before root handling`() = runTest {
        for (request in listOf(imageRequest(), textRequest())) {
            val loader = FakeReaderContentLoader(
                chapterResult = AppResult.Failure(AppError.Unauthorized),
            )
            val coordinator = ReaderCoordinator(ReaderRequestStore().apply { put(request) }, loader)
            coordinator.load(request.requestId, session())
            coordinator.selectChapter(session(), 1)
            val failed = coordinator.state.value

            coordinator.dismissChapterError()

            assertEquals(failed, coordinator.state.value)
            assertTrue(coordinator.state.value.hasUnauthorized())
        }
    }

    @Test
    fun `chapter authorization errors block further chapter and page requests`() = runTest {
        for (request in listOf(imageRequest(), textRequest())) {
            val loader = FakeReaderContentLoader(
                chapterResult = AppResult.Failure(AppError.Unauthorized),
                pageResults = ArrayDeque(listOf(AppResult.Failure(AppError.Offline))),
            )
            val coordinator = ReaderCoordinator(ReaderRequestStore().apply { put(request) }, loader)
            coordinator.load(request.requestId, session())
            coordinator.selectChapter(session(), 1)
            val failed = coordinator.state.value

            coordinator.selectChapter(session(), 2)
            coordinator.loadMoreImages(session())

            assertEquals(failed, coordinator.state.value)
            assertEquals(listOf(1), loader.chapterRequests)
            assertTrue(loader.pageRequests.isEmpty())
            assertTrue(coordinator.state.value.hasUnauthorized())
        }
    }

    @Test
    fun `ordinary chapter errors remain dismissible and retryable`() = runTest {
        for (error in listOf(AppError.Forbidden, AppError.Offline, AppError.Timeout)) {
            for (request in listOf(imageRequest(), textRequest())) {
                val loader = FakeReaderContentLoader(chapterResult = AppResult.Failure(error))
                val coordinator = ReaderCoordinator(ReaderRequestStore().apply { put(request) }, loader)
                coordinator.load(request.requestId, session())
                val original = coordinator.state.value
                coordinator.selectChapter(session(), 1)
                val failed = coordinator.state.value
                assertEquals(error, (failed as ReaderUiState.Active).chapterError)
                assertFalse(failed.hasUnauthorized())

                coordinator.dismissChapterError()
                assertEquals(original, coordinator.state.value)
                coordinator.selectChapter(session(), 1)

                assertEquals(failed, coordinator.state.value)
                assertEquals(listOf(1, 1), loader.chapterRequests)
            }
        }
    }

    @Test
    fun `late chapter error dismissal preserves active image and text retries`() = runTest {
        for (request in listOf(imageRequest(), textRequest())) {
            val pendingChapters = mutableMapOf<Int, CompletableDeferred<AppResult<ReaderContent>>>()
            val loader = FakeReaderContentLoader(
                chapterResult = AppResult.Failure(AppError.Offline),
                chapterResults = pendingChapters,
            )
            val coordinator = ReaderCoordinator(ReaderRequestStore().apply { put(request) }, loader)
            coordinator.load(request.requestId, session())
            coordinator.selectChapter(session(), 1)
            assertEquals(AppError.Offline, (coordinator.state.value as ReaderUiState.Active).chapterError)

            val pending = CompletableDeferred<AppResult<ReaderContent>>()
            pendingChapters[1] = pending
            val retry = launch { coordinator.selectChapter(session(), 1) }
            runCurrent()
            val loading = coordinator.state.value as ReaderUiState.Active
            assertTrue(loading.isChapterLoading)
            assertNull(loading.chapterError)

            coordinator.dismissChapterError()

            assertEquals(loading, coordinator.state.value)
            val replacement = when (val content = loading.content) {
                is ReaderImageContent -> content.copy(
                    images = listOf("chapter-1.jpg"),
                    selectedChapterIndex = 1,
                )

                is ReaderTextContent -> content.copy(text = "Second chapter", selectedChapterIndex = 1)
            }
            pending.complete(AppResult.Success(replacement))
            retry.join()

            val loaded = coordinator.state.value as ReaderUiState.Active
            assertEquals(replacement, loaded.content)
            assertEquals(1L, loaded.contentRevision)
            assertFalse(loaded.isChapterLoading)
            assertNull(loaded.chapterError)
            assertEquals(listOf(1, 1), loader.chapterRequests)
        }
    }

    @Test
    fun `late chapter error dismissal cannot start pagination for outgoing content`() = runTest {
        val pendingChapters = mutableMapOf<Int, CompletableDeferred<AppResult<ReaderContent>>>()
        val loader = FakeReaderContentLoader(
            chapterResult = AppResult.Failure(AppError.Offline),
            chapterResults = pendingChapters,
            pageResults = ArrayDeque(
                listOf(AppResult.Success(ReaderImagePage(listOf("old-page.jpg"), 3, false))),
            ),
        )
        val coordinator = ReaderCoordinator(ReaderRequestStore().apply { put(imageRequest()) }, loader)
        coordinator.load("reader-1", session())
        coordinator.selectChapter(session(), 1)
        val pending = CompletableDeferred<AppResult<ReaderContent>>()
        pendingChapters[1] = pending
        val retry = launch { coordinator.selectChapter(session(), 1) }
        runCurrent()

        coordinator.dismissChapterError()
        coordinator.loadMoreImages(session())
        pending.complete(AppResult.Success(chapterContent(1)))
        retry.join()

        assertTrue(loader.pageRequests.isEmpty())
        assertEquals(chapterContent(1), (coordinator.state.value as ReaderUiState.Image).content)
    }

    @Test
    fun `text chapter replacement retains reader settings and advances content revision`() = runTest {
        val content = ReaderTextContent.network(
            indexerId = 11,
            resourceId = "book-1",
            title = "Book",
            text = "First chapter",
            chapters = listOf(
                ReaderChapter("c0", "Chapter 0"),
                ReaderChapter("c1", "Chapter 1"),
            ),
            selectedChapterIndex = 0,
        )
        val request = ReaderRequest.Text(
            requestId = "text-reader",
            serverId = "server-id",
            content = content,
            settings = TextReaderSettings(),
            chapterOrder = ReaderChapterOrder.Descending,
        )
        val pending = CompletableDeferred<AppResult<ReaderContent>>()
        val coordinator = ReaderCoordinator(
            ReaderRequestStore().apply { put(request) },
            FakeReaderContentLoader(chapterResults = mutableMapOf(1 to pending)),
        )
        coordinator.load(request.requestId, session())
        val chapterJob = launch { coordinator.selectChapter(session(), 1) }
        runCurrent()
        val loading = coordinator.state.value as ReaderUiState.Text
        assertEquals("First chapter", loading.content.text)
        assertTrue(loading.isChapterLoading)
        coordinator.updateTextSettings(TextReaderSettings(fontSizeSp = 32))

        pending.complete(
            AppResult.Success(content.copy(text = "Second chapter", selectedChapterIndex = 1)),
        )
        chapterJob.join()

        val updated = coordinator.state.value as ReaderUiState.Text
        assertEquals("Second chapter", updated.content.text)
        assertEquals(1, updated.content.selectedChapterIndex)
        assertEquals(1L, updated.contentRevision)
        assertEquals(32, updated.settings.fontSizeSp)
        assertEquals(ReaderChapterOrder.Descending, updated.chapterOrder)
        assertFalse(updated.isChapterLoading)
        assertNull(updated.chapterError)
    }

    @Test
    fun `newer chapter wins when older request completes later`() = runTest {
        val first = CompletableDeferred<AppResult<ReaderContent>>()
        val second = CompletableDeferred<AppResult<ReaderContent>>()
        val store = ReaderRequestStore().apply { put(imageRequest()) }
        val loader = FakeReaderContentLoader(
            chapterResults = mutableMapOf(1 to first, 2 to second),
        )
        val coordinator = ReaderCoordinator(store, loader)
        coordinator.load("reader-1", session())

        val older = launch { coordinator.selectChapter(session(), 1) }
        runCurrent()
        val newer = launch { coordinator.selectChapter(session(), 2) }
        runCurrent()
        second.complete(AppResult.Success(chapterContent(2)))
        newer.join()
        first.complete(AppResult.Success(chapterContent(1)))
        older.join()

        val state = coordinator.state.value as ReaderUiState.Image
        assertEquals(2, state.content.selectedChapterIndex)
        assertEquals(listOf("chapter-2.jpg"), state.content.images)
    }

    @Test
    fun `pagination appends in order deduplicates and preserves content on failure`() = runTest {
        val store = ReaderRequestStore().apply { put(imageRequest(imageCount = 5)) }
        val loader = FakeReaderContentLoader(
            pageResults = ArrayDeque(
                listOf(
                    AppResult.Success(
                        ReaderImagePage(
                            images = listOf("two.jpg", "three.jpg", "three.jpg"),
                            imageCount = 4,
                            exhausted = false,
                        ),
                    ),
                    AppResult.Failure(AppError.Offline),
                ),
            ),
        )
        val coordinator = ReaderCoordinator(store, loader)
        coordinator.load("reader-1", session())

        coordinator.loadMoreImages(session())
        coordinator.loadMoreImages(session())

        val state = coordinator.state.value as ReaderUiState.Image
        assertEquals(listOf("one.jpg", "two.jpg", "three.jpg"), state.content.images)
        assertEquals(AppError.Offline, state.pageError)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `empty final image page clears retry error and stops loading without replacing content`() = runTest {
        val store = ReaderRequestStore().apply { put(imageRequest(imageCount = 5)) }
        val loader = FakeReaderContentLoader(
            pageResults = ArrayDeque(
                listOf(
                    AppResult.Failure(AppError.Offline),
                    AppResult.Success(ReaderImagePage(emptyList(), 5, exhausted = true)),
                ),
            ),
        )
        val coordinator = ReaderCoordinator(store, loader)
        coordinator.load("reader-1", session())
        val original = coordinator.state.value as ReaderUiState.Image

        coordinator.loadMoreImages(session())
        assertEquals(original.copy(pageError = AppError.Offline), coordinator.state.value)
        coordinator.loadMoreImages(session())

        assertEquals(original.copy(imagesExhausted = true), coordinator.state.value)
        coordinator.loadMoreImages(session())
        assertEquals(2, loader.pageRequests.size)
    }

    @Test
    fun `cancelled pagination drops queued errors and clears loading without replacing content`() = runTest {
        val response = PendingNetworkResponse<ReaderImagePage>()
        val loader = object : ReaderContentLoader by FakeReaderContentLoader() {
            override suspend fun loadImagePage(
                session: Session,
                content: ReaderImageContent,
                previous: Boolean,
            ) = response.await()
        }
        val coordinator = ReaderCoordinator(
            ReaderRequestStore().apply { put(imageRequest()) },
            loader,
        )
        coordinator.load("reader-1", session())
        val original = coordinator.state.value
        var returnedNormally = false
        val job = launch {
            coordinator.loadMoreImages(session())
            returnedNormally = true
        }
        runCurrent()
        assertTrue((coordinator.state.value as ReaderUiState.Image).isLoadingMore)

        response.failUnauthorized()
        job.cancel()
        runCurrent()

        assertEquals(original, coordinator.state.value)
        assertFalse(coordinator.state.value.hasUnauthorized())
        assertFalse(returnedNormally)
    }

    @Test
    fun `active pagination retains queued authorization errors for root handling`() = runTest {
        val response = PendingNetworkResponse<ReaderImagePage>()
        val loader = object : ReaderContentLoader by FakeReaderContentLoader() {
            override suspend fun loadImagePage(
                session: Session,
                content: ReaderImageContent,
                previous: Boolean,
            ) = response.await()
        }
        val coordinator = ReaderCoordinator(
            ReaderRequestStore().apply { put(imageRequest()) },
            loader,
        )
        coordinator.load("reader-1", session())
        val original = coordinator.state.value as ReaderUiState.Image
        val job = launch { coordinator.loadMoreImages(session()) }
        runCurrent()

        response.failUnauthorized()
        job.join()

        assertEquals(original.copy(pageError = AppError.Unauthorized), coordinator.state.value)
        assertTrue(coordinator.state.value.hasUnauthorized())
    }

    @Test
    fun `page authorization errors cannot be dismissed before root handling`() = runTest {
        val loader = FakeReaderContentLoader(
            pageResults = ArrayDeque(listOf(AppResult.Failure(AppError.Unauthorized))),
        )
        val coordinator = ReaderCoordinator(
            ReaderRequestStore().apply { put(imageRequest()) },
            loader,
        )
        coordinator.load("reader-1", session())
        coordinator.loadMoreImages(session())
        val failed = coordinator.state.value

        coordinator.dismissPageError()

        assertEquals(failed, coordinator.state.value)
        assertTrue(coordinator.state.value.hasUnauthorized())
    }

    @Test
    fun `page authorization errors block further page and chapter requests`() = runTest {
        val loader = FakeReaderContentLoader(
            chapterResult = AppResult.Success(chapterContent(1)),
            pageResults = ArrayDeque(
                listOf(
                    AppResult.Failure(AppError.Unauthorized),
                    AppResult.Failure(AppError.Offline),
                ),
            ),
        )
        val coordinator = ReaderCoordinator(
            ReaderRequestStore().apply { put(imageRequest()) },
            loader,
        )
        coordinator.load("reader-1", session())
        coordinator.loadMoreImages(session())
        val failed = coordinator.state.value

        coordinator.loadMoreImages(session())
        coordinator.selectChapter(session(), 1)

        assertEquals(failed, coordinator.state.value)
        assertEquals(1, loader.pageRequests.size)
        assertTrue(loader.chapterRequests.isEmpty())
        assertTrue(coordinator.state.value.hasUnauthorized())
    }

    @Test
    fun `ordinary page errors remain dismissible and retryable`() = runTest {
        for (error in listOf(AppError.Forbidden, AppError.Offline, AppError.Timeout)) {
            val loader = FakeReaderContentLoader(
                pageResults = ArrayDeque(
                    listOf(
                        AppResult.Failure(error),
                        AppResult.Success(ReaderImagePage(listOf("two.jpg"), 3, false)),
                    ),
                ),
            )
            val coordinator = ReaderCoordinator(
                ReaderRequestStore().apply { put(imageRequest()) },
                loader,
            )
            coordinator.load("reader-1", session())
            val original = coordinator.state.value as ReaderUiState.Image
            coordinator.loadMoreImages(session())
            assertEquals(original.copy(pageError = error), coordinator.state.value)
            assertFalse(coordinator.state.value.hasUnauthorized())

            coordinator.dismissPageError()
            assertEquals(original, coordinator.state.value)
            coordinator.loadMoreImages(session())

            assertEquals(
                original.copy(content = original.content.copy(images = listOf("one.jpg", "two.jpg"))),
                coordinator.state.value,
            )
            assertEquals(2, loader.pageRequests.size)
        }
    }

    @Test
    fun `pagination waits for chapter replacement before loading its pages`() = runTest {
        val pendingChapter = CompletableDeferred<AppResult<ReaderContent>>()
        val pendingPage = CompletableDeferred<AppResult<ReaderImagePage>>()
        val loader = FakeReaderContentLoader(
            chapterResults = mutableMapOf(1 to pendingChapter),
            pendingPageResult = pendingPage,
        )
        val coordinator = ReaderCoordinator(
            ReaderRequestStore().apply { put(imageRequest(imageCount = 5)) },
            loader,
        )
        coordinator.load("reader-1", session())
        val chapterJob = launch { coordinator.selectChapter(session(), 1) }
        runCurrent()
        val pageJob = launch { coordinator.loadMoreImages(session()) }
        runCurrent()

        val replacement = chapterContent(1).copy(imageCount = 3)
        pendingChapter.complete(AppResult.Success(replacement))
        chapterJob.join()
        pendingPage.complete(
            AppResult.Success(
                ReaderImagePage(
                    images = listOf("next-page.jpg"),
                    imageCount = 3,
                    exhausted = false,
                ),
            ),
        )
        pageJob.join()

        val replaced = coordinator.state.value as ReaderUiState.Image
        assertEquals(replacement, replaced.content)
        assertEquals(1L, replaced.contentRevision)
        assertFalse(replaced.isChapterLoading)
        assertFalse(replaced.isLoadingMore)
        assertTrue(loader.pageRequests.isEmpty())

        coordinator.loadMoreImages(session())

        val paged = coordinator.state.value as ReaderUiState.Image
        assertEquals(listOf(replacement), loader.pageRequests)
        assertEquals(replacement.images + "next-page.jpg", paged.content.images)
        assertFalse(paged.isLoadingMore)
        assertNull(paged.pageError)
    }

    @Test
    fun `pagination resumes on retained content after chapter failure`() = runTest {
        val request = imageRequest()
        val pendingChapter = CompletableDeferred<AppResult<ReaderContent>>()
        val loader = FakeReaderContentLoader(
            chapterResults = mutableMapOf(1 to pendingChapter),
            pageResults = ArrayDeque(
                listOf(
                    AppResult.Success(
                        ReaderImagePage(
                            images = listOf("two.jpg"),
                            imageCount = 3,
                            exhausted = false,
                        ),
                    ),
                ),
            ),
        )
        val coordinator = ReaderCoordinator(ReaderRequestStore().apply { put(request) }, loader)
        coordinator.load(request.requestId, session())
        val chapterJob = launch { coordinator.selectChapter(session(), 1) }
        runCurrent()

        coordinator.loadMoreImages(session())
        pendingChapter.complete(AppResult.Failure(AppError.Offline))
        chapterJob.join()

        val retained = coordinator.state.value as ReaderUiState.Image
        assertEquals(request.content, retained.content)
        assertEquals(0L, retained.contentRevision)
        assertEquals(AppError.Offline, retained.chapterError)
        assertFalse(retained.isChapterLoading)
        assertTrue(loader.pageRequests.isEmpty())

        coordinator.loadMoreImages(session())

        val paged = coordinator.state.value as ReaderUiState.Image
        assertEquals(listOf(request.content), loader.pageRequests)
        assertEquals(listOf("one.jpg", "two.jpg"), paged.content.images)
        assertFalse(paged.isLoadingMore)
        assertNull(paged.pageError)
    }

    @Test
    fun `chapter failure clears pagination cancelled by source change`() = runTest {
        val pendingPage = CompletableDeferred<AppResult<ReaderImagePage>>()
        val store = ReaderRequestStore().apply { put(imageRequest(imageCount = 5)) }
        val loader = FakeReaderContentLoader(
            chapterResult = AppResult.Failure(AppError.Offline),
            pendingPageResult = pendingPage,
        )
        val coordinator = ReaderCoordinator(store, loader)
        coordinator.load("reader-1", session())
        val pageJob = launch { coordinator.loadMoreImages(session()) }
        runCurrent()
        assertTrue((coordinator.state.value as ReaderUiState.Image).isLoadingMore)

        pageJob.cancel()
        coordinator.selectChapter(session(), 1)
        runCurrent()

        val state = coordinator.state.value as ReaderUiState.Image
        assertFalse(state.isLoadingMore)
        assertEquals(AppError.Offline, state.chapterError)
    }

    @Test
    fun `session settings and order change without replacing stored defaults`() {
        val request = imageRequest()
        val store = ReaderRequestStore().apply { put(request) }
        val coordinator = ReaderCoordinator(store, FakeReaderContentLoader())
        coordinator.load("reader-1", session())

        coordinator.updateImageSettings(ImageReaderSettings(readMode = ImageReadMode.Paged))
        coordinator.updateChapterOrder(ReaderChapterOrder.Descending)

        val state = coordinator.state.value as ReaderUiState.Image
        assertEquals(ImageReadMode.Paged, state.settings.readMode)
        assertEquals(ReaderChapterOrder.Descending, state.chapterOrder)
        assertEquals(ImageReadMode.Scroll, (store.get("reader-1") as ReaderRequest.Image).settings.readMode)
    }

    @Test
    fun `close removes request and returns coordinator to idle`() {
        val store = ReaderRequestStore().apply { put(imageRequest()) }
        val coordinator = ReaderCoordinator(store, FakeReaderContentLoader())
        coordinator.load("reader-1", session())

        coordinator.close("reader-1")

        assertNull(store.get("reader-1"))
        assertEquals(ReaderUiState.Idle, coordinator.state.value)
    }

    @Test
    fun `closing an old request preserves the current reader error`() {
        val store = ReaderRequestStore().apply { put(imageRequest()) }
        val coordinator = ReaderCoordinator(store, FakeReaderContentLoader())
        coordinator.load("missing", session())
        val error = coordinator.state.value

        coordinator.close("reader-1")

        assertNull(store.get("reader-1"))
        assertEquals(error, coordinator.state.value)
    }

    @Test
    fun `closing the current missing or wrong server request clears its error`() {
        for (requestId in listOf("missing", "reader-1")) {
            val store = ReaderRequestStore().apply { put(imageRequest(serverId = "other-server")) }
            val coordinator = ReaderCoordinator(store, FakeReaderContentLoader())
            coordinator.load(requestId, session())
            assertTrue(coordinator.state.value is ReaderUiState.Error)

            coordinator.close(requestId)

            assertNull(store.get(requestId))
            assertEquals(ReaderUiState.Idle, coordinator.state.value)
        }
    }

    private fun errorState(coordinator: ReaderCoordinator) =
        coordinator.state.value as ReaderUiState.Error
}

private class FakeReaderContentLoader(
    private val chapterResult: AppResult<ReaderContent> = AppResult.Failure(AppError.NotFound),
    private val chapterResults: MutableMap<Int, CompletableDeferred<AppResult<ReaderContent>>> =
        mutableMapOf(),
    private val pageResults: ArrayDeque<AppResult<ReaderImagePage>> = ArrayDeque(),
    private val pendingPageResult: CompletableDeferred<AppResult<ReaderImagePage>>? = null,
) : StubReaderContentLoader() {
    val chapterRequests = mutableListOf<Int>()
    val pageRequests = mutableListOf<ReaderImageContent>()

    override suspend fun resolveChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent> {
        chapterRequests += chapterIndex
        return chapterResults[chapterIndex]?.await() ?: chapterResult
    }

    override suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
        previous: Boolean,
    ): AppResult<ReaderImagePage> {
        pageRequests += content
        return pendingPageResult?.await() ?: pageResults.removeFirst()
    }
}

private class PendingNetworkResponse<T> {
    private var continuation: CancellableContinuation<T>? = null

    suspend fun await(): AppResult<T> = networkCall(Json) {
        // Preserve the production callback cancellation and HTTP error mapping.
        suspendCancellableCoroutine { continuation = it }
    }

    fun failUnauthorized() {
        checkNotNull(continuation).resumeWithException(
            HttpException(Response.error<Unit>(401, "".toResponseBody())),
        )
        continuation = null
    }
}

private fun imageRequest(
    serverId: String = "server-id",
    imageCount: Int = 3,
) = ReaderRequest.Image(
    requestId = "reader-1",
    serverId = serverId,
    content = ReaderImageContent.network(
        indexerId = 11,
        resourceId = "comic-1",
        chapterId = "c0",
        title = "Comic",
        images = listOf("one.jpg"),
        imageCount = imageCount,
        chapters = listOf(
            ReaderChapter("c0", "Chapter 0"),
            ReaderChapter("c1", "Chapter 1"),
            ReaderChapter("c2", "Chapter 2"),
        ),
        selectedChapterIndex = 0,
    ),
    settings = ImageReaderSettings(),
    chapterOrder = ReaderChapterOrder.Ascending,
)

private fun chapterContent(index: Int) = ReaderImageContent.network(
    indexerId = 11,
    resourceId = "comic-1",
    chapterId = "c$index",
    title = "Chapter $index",
    images = listOf("chapter-$index.jpg"),
    imageCount = 1,
    chapters = imageRequest().content.chapters,
    selectedChapterIndex = index,
)

private fun textRequest() = ReaderRequest.Text(
    requestId = "text-reader",
    serverId = "server-id",
    content = ReaderTextContent.network(
        indexerId = 11,
        resourceId = "book-1",
        title = "Book",
        text = "First chapter",
        chapters = imageRequest().content.chapters,
        selectedChapterIndex = 0,
    ),
    settings = TextReaderSettings(),
    chapterOrder = ReaderChapterOrder.Ascending,
)

private fun session() = Session(
    server = SavedServer("server-id", "Home", "https://tv.example"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
