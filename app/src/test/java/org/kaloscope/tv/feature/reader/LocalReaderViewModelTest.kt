package org.kaloscope.tv.feature.reader

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.model.ReaderSource
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.core.reader.ReaderRequestStore
import org.kaloscope.tv.test.StubReaderContentLoader

@OptIn(ExperimentalCoroutinesApi::class)
class LocalReaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = Session(SavedServer("test", "Test", "https://library.example"), "fixture-token", SessionUser(1, "reader", "user"))
    private val source = ReaderSource.Local(1, 1, "first", "version", "txt")
    private val content = ReaderTextContent(
        source, "Book", "One\n\nTwo\n\nThree",
        chapters = listOf(ReaderChapter("first", "First"), ReaderChapter("second", "Second")),
        selectedChapterIndex = 0,
    )
    private val request = ReaderRequest.Local(
        "local", "test", "Book", 1, MediaLibraryType.Novel,
        imageSettings = ImageReaderSettings(), textSettings = TextReaderSettings(),
        chapterOrder = ReaderChapterOrder.Ascending,
    )
    private val writes = mutableListOf<Pair<ReaderContent, ReaderPosition>>()
    private lateinit var viewModel: ReaderViewModel

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        viewModel.clearServer(session.server.id)
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test fun `rapid progress updates coalesce and close flushes latest location once`() = runTest(dispatcher) {
        open()
        viewModel.updateProgress(session, content, ReaderPosition(0, 0.2))
        viewModel.updateProgress(session, content, ReaderPosition(1, 0.4))
        advanceTimeBy(999)
        assertTrue(writes.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(content to ReaderPosition(1, 0.4)), writes)
        viewModel.updateProgress(session, content, ReaderPosition(2, 0.6))
        viewModel.close(request.requestId)
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(listOf(ReaderPosition(1, 0.4), ReaderPosition(2, 0.6)), writes.map { it.second })
        assertEquals(ReaderUiState.Idle, viewModel.uiState.value)
    }

    @Test fun `chapter change flushes old chapter and rejects retiring surface updates`() = runTest(dispatcher) {
        open()
        viewModel.updateProgress(session, content, ReaderPosition(1))
        viewModel.selectChapter(session, 1)
        runCurrent()
        viewModel.updateProgress(session, content, ReaderPosition(2))
        viewModel.flushProgress()
        runCurrent()
        assertEquals(listOf(content to ReaderPosition(1)), writes)
        assertEquals(1, (viewModel.uiState.value as ReaderUiState.Text).content.selectedChapterIndex)
    }

    @Test fun `server exit drops pending progress and cancels active writes`() = runTest(dispatcher) {
        val cancelled = CompletableDeferred<Unit>()
        open(object : Loader() {
            override suspend fun recordProgress(session: Session, content: ReaderContent, position: ReaderPosition): AppResult<Unit> {
                try { awaitCancellation() } finally { cancelled.complete(Unit) }
            }
        })
        viewModel.updateProgress(session, content, ReaderPosition(1))
        viewModel.flushProgress()
        runCurrent()
        viewModel.updateProgress(session, content, ReaderPosition(2))
        viewModel.clearServer(session.server.id)
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(cancelled.isCompleted)
        assertTrue(writes.isEmpty())
        assertEquals(ReaderUiState.Idle, viewModel.uiState.value)
    }

    @Test fun `another server and online content cannot write local reading progress`() = runTest(dispatcher) {
        open()
        viewModel.updateProgress(session.copy(server = session.server.copy(id = "other")), content, ReaderPosition(1))
        viewModel.updateProgress(session, content.copy(source = ReaderSource.Network(1, "book", "chapter")), ReaderPosition(1))
        viewModel.flushProgress()
        runCurrent()
        assertTrue(writes.isEmpty())
    }

    @Test fun `failed progress is retried on exit without requiring another page turn`() = runTest(dispatcher) {
        var attempts = 0
        open(object : Loader() {
            override suspend fun recordProgress(session: Session, content: ReaderContent, position: ReaderPosition): AppResult<Unit> {
                attempts++
                return if (attempts == 1) AppResult.Failure(AppError.Offline)
                else super.recordProgress(session, content, position)
            }
        })
        viewModel.updateProgress(session, content, ReaderPosition(1, 0.4))
        viewModel.flushProgress()
        runCurrent()
        viewModel.dismissProgressError()
        viewModel.close(request.requestId)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(listOf(content to ReaderPosition(1, 0.4)), writes)
    }

    private fun open(loader: Loader = Loader()) {
        viewModel = ReaderViewModel(ReaderRequestStore().apply { put(request) }, loader)
        viewModel.load(request.requestId, session)
        dispatcher.scheduler.runCurrent()
    }

    private open inner class Loader : StubReaderContentLoader() {
        override suspend fun openLocal(session: Session, request: ReaderRequest.Local) = AppResult.Success(content)
        override suspend fun recordProgress(session: Session, content: ReaderContent, position: ReaderPosition): AppResult<Unit> {
            writes += content to position
            return AppResult.Success(Unit)
        }
        override suspend fun resolveChapter(session: Session, content: ReaderContent, chapterIndex: Int) = AppResult.Success(
            this@LocalReaderViewModelTest.content.copy(source = source.copy(chapterId = "second"), selectedChapterIndex = chapterIndex),
        )
    }
}
