package org.kaloscope.tv.feature.library

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.test.StubMediaRepository

class LibraryCoordinatorTest {
    @Test
    fun `viewport is remembered for the current library dataset`() = runBlocking {
        val coordinator = LibraryCoordinator(
            FakeMediaRepository(
                libraries = AppResult.Success(libraries()),
                pages = mutableListOf(AppResult.Success(page(1, total = 1))),
            ),
        )
        coordinator.load(session())

        coordinator.rememberGridViewport(GridViewportSnapshot(15, 32))

        val state = coordinator.state.value as LibraryUiState.Content
        assertEquals(GridViewportSnapshot(15, 32), state.gridViewport)
    }

    @Test
    fun `initial load selects first real library and first page`() = runBlocking {
        val repository = FakeMediaRepository(
            libraries = AppResult.Success(libraries()),
            pages = mutableListOf(AppResult.Success(page(1, total = 21))),
        )
        val coordinator = LibraryCoordinator(repository)

        coordinator.load(session())

        val state = coordinator.state.value as LibraryUiState.Content
        assertEquals(21L, state.selectedLibraryId)
        assertEquals("剧集库", state.selectedLibrary.name)
        assertEquals(listOf(201L), state.items.items.map { it.id })
        assertTrue(state.items.hasNext)
        assertEquals(listOf(PageCall(21, 1, 20, null)), repository.pageCalls)
    }

    @Test
    fun `empty real library list uses source empty state`() = runBlocking {
        val coordinator = LibraryCoordinator(
            FakeMediaRepository(libraries = AppResult.Success(emptyList())),
        )

        coordinator.load(session())

        assertEquals(LibraryUiState.EmptyLibraries, coordinator.state.value)
    }

    @Test
    fun `first page failure keeps source menu and exposes retry`() = runBlocking {
        val coordinator = LibraryCoordinator(
            FakeMediaRepository(
                libraries = AppResult.Success(libraries()),
                pages = mutableListOf(AppResult.Failure(AppError.Offline)),
            ),
        )

        coordinator.load(session())

        val state = coordinator.state.value as LibraryUiState.Content
        assertEquals(LibraryItemsState.Error(AppError.Offline), state.items)
    }

    @Test
    fun `load more appends stable unique items`() = runBlocking {
        val coordinator = LibraryCoordinator(
            FakeMediaRepository(
                libraries = AppResult.Success(libraries()),
                pages = mutableListOf(
                    AppResult.Success(page(1, total = 21)),
                    AppResult.Success(
                        MediaPage(
                            items = listOf(summary(201), summary(202)),
                            total = 21,
                            pageNumber = 2,
                            pageSize = 20,
                            hasNext = false,
                        ),
                    ),
                ),
            ),
        )
        coordinator.load(session())

        coordinator.loadNext(session())

        val state = coordinator.state.value as LibraryUiState.Content
        assertEquals(listOf(201L, 202L), state.items.items.map { it.id })
        assertFalse(state.items.hasNext)
    }

    @Test
    fun `switching library clears query and loads its first page`() = runBlocking {
        val repository = FakeMediaRepository(
            libraries = AppResult.Success(libraries()),
            pages = mutableListOf(
                AppResult.Success(page(1, total = 1)),
                AppResult.Success(
                    MediaPage(
                        items = listOf(summary(501)),
                        total = 1,
                        pageNumber = 1,
                        pageSize = 20,
                        hasNext = false,
                    ),
                ),
            ),
        )
        val coordinator = LibraryCoordinator(repository)
        coordinator.load(session())
        coordinator.updateQuery("旧关键词")
        coordinator.rememberFocusedMedia(201)
        coordinator.rememberGridViewport(GridViewportSnapshot(7, 20))

        coordinator.selectLibrary(session(), 22)

        val state = coordinator.state.value as LibraryUiState.Content
        assertEquals(22L, state.selectedLibraryId)
        assertEquals("", state.query)
        assertEquals("", state.submittedKeyword)
        assertEquals(listOf(501L), state.items.items.map { it.id })
        assertEquals(null, state.focusedMediaId)
        assertEquals(GridViewportSnapshot.Top, state.gridViewport)
        assertEquals(PageCall(22, 1, 20, null), repository.pageCalls.last())
    }

    @Test
    fun `submitting a new library search clears viewport and focus`() = runBlocking {
        val coordinator = LibraryCoordinator(
            FakeMediaRepository(
                libraries = AppResult.Success(libraries()),
                pages = mutableListOf(
                    AppResult.Success(page(1, total = 1)),
                    AppResult.Success(
                        MediaPage(
                            items = listOf(summary(501)),
                            total = 1,
                            pageNumber = 1,
                            pageSize = 20,
                            hasNext = false,
                        ),
                    ),
                ),
            ),
        )
        coordinator.load(session())
        coordinator.rememberFocusedMedia(201)
        coordinator.rememberGridViewport(GridViewportSnapshot(5, 14))

        coordinator.updateQuery("新关键词")
        coordinator.search(session())

        val state = coordinator.state.value as LibraryUiState.Content
        assertEquals(null, state.focusedMediaId)
        assertEquals(GridViewportSnapshot.Top, state.gridViewport)
        assertEquals(listOf(501L), state.items.items.map { it.id })
    }

    @Test
    fun `load more failure preserves content and retries the same page`() = runBlocking {
        val repository = FakeMediaRepository(
            libraries = AppResult.Success(libraries()),
            pages = mutableListOf(
                AppResult.Success(page(1, total = 21)),
                AppResult.Failure(AppError.Offline),
                AppResult.Success(
                    MediaPage(
                        items = listOf(summary(202)),
                        total = 21,
                        pageNumber = 2,
                        pageSize = 20,
                        hasNext = false,
                    ),
                ),
            ),
        )
        val coordinator = LibraryCoordinator(repository)
        coordinator.load(session())

        coordinator.loadNext(session())

        val failed = coordinator.state.value as LibraryUiState.Content
        val failedItems = failed.items as LibraryItemsState.Content
        assertEquals(listOf(201L), failedItems.items.map { it.id })
        assertEquals(1, failedItems.pageNumber)
        assertEquals(AppError.Offline, failedItems.loadMoreError)

        coordinator.loadNext(session())

        val recovered = coordinator.state.value as LibraryUiState.Content
        val recoveredItems = recovered.items as LibraryItemsState.Content
        assertEquals(listOf(201L, 202L), recoveredItems.items.map { it.id })
        assertEquals(listOf(2, 2), repository.pageCalls.drop(1).map { it.pageNumber })
    }

    @Test
    fun `final library page ignores load more`() = runBlocking {
        val repository = FakeMediaRepository(
            libraries = AppResult.Success(libraries()),
            pages = mutableListOf(AppResult.Success(page(1, total = 1))),
        )
        val coordinator = LibraryCoordinator(repository)
        coordinator.load(session())

        coordinator.loadNext(session())

        assertEquals(listOf(1), repository.pageCalls.map { it.pageNumber })
    }
}

private data class PageCall(
    val libraryId: Long,
    val pageNumber: Int,
    val pageSize: Int,
    val keyword: String?,
)

private class FakeMediaRepository(
    private val libraries: AppResult<List<MediaLibrary>>,
    private val pages: MutableList<AppResult<MediaPage>> = mutableListOf(),
    private val details: AppResult<MediaDetail> = AppResult.Failure(AppError.NotFound),
) : StubMediaRepository() {
    val pageCalls = mutableListOf<PageCall>()

    override suspend fun getLibraries(session: Session): AppResult<List<MediaLibrary>> =
        libraries

    override suspend fun getMediaPage(
        session: Session,
        libraryId: Long,
        pageNumber: Int,
        pageSize: Int,
        keyword: String?,
    ): AppResult<MediaPage> {
        pageCalls += PageCall(libraryId, pageNumber, pageSize, keyword)
        return pages.removeAt(0)
    }

    override suspend fun getMediaDetail(
        session: Session,
        mediaId: Long,
    ): AppResult<MediaDetail> = details
}

private fun libraries() = listOf(
    MediaLibrary(21, "剧集库", MediaLibraryType.TvShow),
    MediaLibrary(22, "电影库", MediaLibraryType.Movie),
)

private fun page(pageNumber: Int, total: Int) = MediaPage(
    items = listOf(summary(201)),
    total = total,
    pageNumber = pageNumber,
    pageSize = 20,
    hasNext = pageNumber * 20 < total,
)

private fun summary(id: Long) = MediaSummary(
    id = id,
    title = "媒体$id",
    path = "/media/$id",
    posterPath = null,
    backdropPath = null,
    year = null,
    rating = null,
    season = null,
    episode = null,
)

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
