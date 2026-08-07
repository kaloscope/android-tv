package org.kaloscope.tv.feature.reader

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
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
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.core.reader.ReaderRequestStore
import org.kaloscope.tv.data.reader.ReaderContentLoader

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

    private fun errorState(coordinator: ReaderCoordinator) =
        coordinator.state.value as ReaderUiState.Error
}

private class FakeReaderContentLoader(
    private val chapterResult: AppResult<ReaderContent> = AppResult.Failure(AppError.NotFound),
    private val chapterResults: MutableMap<Int, CompletableDeferred<AppResult<ReaderContent>>> =
        mutableMapOf(),
    private val pageResults: ArrayDeque<AppResult<ReaderImagePage>> = ArrayDeque(),
    private val pendingPageResult: CompletableDeferred<AppResult<ReaderImagePage>>? = null,
) : ReaderContentLoader {
    override suspend fun resolveChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent> = chapterResults[chapterIndex]?.await() ?: chapterResult

    override suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
    ): AppResult<ReaderImagePage> = pendingPageResult?.await() ?: pageResults.removeFirst()
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

private fun session() = Session(
    server = SavedServer("server-id", "Home", "https://tv.example"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
