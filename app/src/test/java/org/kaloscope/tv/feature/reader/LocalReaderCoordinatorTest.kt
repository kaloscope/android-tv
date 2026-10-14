package org.kaloscope.tv.feature.reader

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.*
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.core.reader.ReaderRequestStore
import org.kaloscope.tv.test.StubReaderContentLoader

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LocalReaderCoordinatorTest {
    private val session = Session(SavedServer("server", "Test", "https://library.example"), "fixture-token", SessionUser(1, "reader", "user"))
    private val local = ReaderSource.Local(1, 2, "item:2", "a".repeat(64), "cbz")
    private val content = ReaderImageContent(local, "Comic", listOf("page-21", "page-22"), 22,
        listOf(ReaderChapter("item:2", "Chapter")), 0, imageOffset = 20)
    private val request = ReaderRequest.Local("request", "server", "Comic", 1, MediaLibraryType.Comic,
        imageSettings = ImageReaderSettings(), textSettings = TextReaderSettings(), chapterOrder = ReaderChapterOrder.Ascending)

    @Test fun `local loading retains request ownership and ignores completion after close`() = runTest {
        val pending = CompletableDeferred<AppResult<ReaderContent>>()
        val loader = object : StubReaderContentLoader() {
            override suspend fun openLocal(session: Session, request: ReaderRequest.Local) = pending.await()
        }
        val store = ReaderRequestStore().apply { put(request) }
        val coordinator = ReaderCoordinator(store, loader)
        coordinator.load(request.requestId, session)
        assertTrue(coordinator.state.value is ReaderUiState.Loading)
        val job = launch { coordinator.loadLocal(session) }
        runCurrent()
        coordinator.close(request.requestId)
        pending.complete(AppResult.Success(content))
        job.join()
        assertEquals(ReaderUiState.Idle, coordinator.state.value)
        assertNull(store.get(request.requestId))
    }

    @Test fun `wrong server cannot request local content`() = runTest {
        val coordinator = ReaderCoordinator(ReaderRequestStore().apply { put(request) }, StubReaderContentLoader())
        coordinator.load(request.requestId, session.copy(server = session.server.copy(id = "other")))
        coordinator.loadLocal(session)
        assertTrue((coordinator.state.value as ReaderUiState.Error).error is AppError.InvalidData)
    }

    @Test fun `server exit clears authorization failure from initial local load`() = runTest {
        val loader = object : StubReaderContentLoader() {
            override suspend fun openLocal(session: Session, request: ReaderRequest.Local) =
                AppResult.Failure(AppError.Unauthorized)
        }
        val coordinator = coordinator(loader)
        coordinator.loadLocal(session)
        assertEquals(AppError.Unauthorized, (coordinator.state.value as ReaderUiState.Error).error)
        coordinator.clearServer(session.server.id)
        assertEquals(ReaderUiState.Idle, coordinator.state.value)
    }

    @Test fun `previous comic page remains reachable even when forward pages are exhausted`() = runTest {
        val loader = object : StubReaderContentLoader() {
            override suspend fun openLocal(session: Session, request: ReaderRequest.Local) = AppResult.Success(content)
            override suspend fun loadImagePage(session: Session, content: ReaderImageContent, previous: Boolean): AppResult<ReaderImagePage> {
                assertTrue(previous)
                return AppResult.Success(ReaderImagePage((1..20).map { "page-$it" }, 22, false, 0, 20))
            }
        }
        val coordinator = coordinator(loader)
        coordinator.loadLocal(session)
        assertTrue((coordinator.state.value as ReaderUiState.Image).imagesExhausted)
        coordinator.loadMoreImages(session, previous = true)
        val current = coordinator.state.value as ReaderUiState.Image
        assertEquals(22, current.content.images.size)
        assertEquals(0, current.content.imageOffset)
        assertEquals(ReaderPosition(19), current.content.initialPosition)
        assertEquals(1L, current.contentRevision)
        assertTrue(current.imagesExhausted)
    }

    @Test fun `failed version recovery retains displayed images and permits retry`() = runTest {
        val partial = content.copy(imageOffset = 0, imageCount = 50, nextOffset = 2)
        var refreshes = 0
        val loader = object : StubReaderContentLoader() {
            override suspend fun openLocal(session: Session, request: ReaderRequest.Local) = AppResult.Success(partial)
            override suspend fun loadImagePage(session: Session, content: ReaderImageContent, previous: Boolean) =
                AppResult.Failure(AppError.Api("content_changed", null))
            override suspend fun refreshLocal(session: Session, content: ReaderContent): AppResult<ReaderContent> {
                refreshes++
                return AppResult.Failure(AppError.Offline)
            }
        }
        val coordinator = coordinator(loader)
        coordinator.loadLocal(session)
        coordinator.loadMoreImages(session)
        val current = coordinator.state.value as ReaderUiState.Image
        assertEquals(partial, current.content)
        assertEquals(AppError.Offline, current.chapterError)
        assertFalse(current.isChapterLoading)
        assertFalse(current.isLoadingMore)
        assertEquals(1, refreshes)
    }

    @Test fun `progress failures stay visible and authorization cannot be dismissed`() = runTest {
        val loader = object : StubReaderContentLoader() {
            override suspend fun openLocal(session: Session, request: ReaderRequest.Local) = AppResult.Success(content)
            override suspend fun recordProgress(session: Session, content: ReaderContent, position: ReaderPosition) =
                AppResult.Failure(AppError.Unauthorized)
        }
        val coordinator = coordinator(loader)
        coordinator.loadLocal(session)
        coordinator.recordProgress(session, content, ReaderPosition())
        coordinator.dismissProgressError()
        assertEquals(AppError.Unauthorized, (coordinator.state.value as ReaderUiState.Image).progressError)
    }

    @Test fun `unchanged image retry preserves the current content and revision`() = runTest {
        val loader = object : StubReaderContentLoader() {
            override suspend fun openLocal(session: Session, request: ReaderRequest.Local) = AppResult.Success(content)
            override suspend fun refreshLocal(session: Session, content: ReaderContent) = AppResult.Success(this@LocalReaderCoordinatorTest.content.copy(images = listOf("other-page")))
        }
        val coordinator = coordinator(loader)
        coordinator.loadLocal(session)
        coordinator.refreshLocal(session)
        val current = coordinator.state.value as ReaderUiState.Image
        assertEquals(content, current.content)
        assertEquals(0L, current.contentRevision)
    }

    private fun coordinator(loader: StubReaderContentLoader) = ReaderCoordinator(
        ReaderRequestStore().apply { put(request) }, loader,
    ).apply { load(request.requestId, session) }
}
