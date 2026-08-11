package org.kaloscope.tv.feature.detail

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaActor
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.data.media.MediaRepository

class MediaDetailCoordinatorTest {
    @Test
    fun `loads real media detail`() = runBlocking {
        val detail = detail(201)
        val coordinator = MediaDetailCoordinator(
            DetailFakeRepository(mutableListOf(AppResult.Success(detail))),
        )

        coordinator.load(session(), 201)

        assertEquals(MediaDetailUiState.Content(detail), coordinator.state.value)
    }

    @Test
    fun `detail error remains retryable`() = runBlocking {
        val coordinator = MediaDetailCoordinator(
            DetailFakeRepository(mutableListOf(AppResult.Failure(AppError.Offline))),
        )

        coordinator.load(session(), 201)

        assertEquals(MediaDetailUiState.Error(AppError.Offline), coordinator.state.value)
    }

    @Test
    fun `load chooses first child without loading child detail`() =
        runBlocking {
            val parent = detail(
                201,
                children = listOf(
                    summary(100, season = 0),
                    summary(301, season = 1),
                ),
            )
            val repository = DetailFakeRepository(mutableListOf(AppResult.Success(parent)))
            val coordinator = MediaDetailCoordinator(repository)

            coordinator.load(session(), 201)

            val content = coordinator.state.value as MediaDetailUiState.Content
            assertEquals(100L, content.focusedChildId)
            assertEquals(listOf(201L), repository.detailCalls)
        }

    @Test
    fun `focus and viewport updates never call repository`() = runBlocking {
        val parent = detail(201, children = listOf(summary(301), summary(302)))
        val repository = DetailFakeRepository(mutableListOf(AppResult.Success(parent)))
        val coordinator = MediaDetailCoordinator(repository)
        coordinator.load(session(), 201)

        coordinator.rememberFocusedChild(302)
        coordinator.rememberChildViewport(GridViewportSnapshot(1, 24))

        val content = coordinator.state.value as MediaDetailUiState.Content
        assertEquals(302L, content.focusedChildId)
        assertEquals(GridViewportSnapshot(1, 24), content.childViewport)
        assertEquals(listOf(201L), repository.detailCalls)
    }

    @Test
    fun `focus moves across season numbers without repository calls`() = runBlocking {
        val parent = detail(
            201,
            children = listOf(
                summary(100, season = 0),
                summary(301, season = 1),
            ),
        )
        val repository = DetailFakeRepository(mutableListOf(AppResult.Success(parent)))
        val coordinator = MediaDetailCoordinator(repository)
        coordinator.load(session(), 201)

        coordinator.rememberFocusedChild(301)

        val content = coordinator.state.value as MediaDetailUiState.Content
        assertEquals(301L, content.focusedChildId)
        assertEquals(listOf(201L), repository.detailCalls)
    }

    @Test
    fun `child detail failure retains parent content`() = runBlocking {
        val parent = detail(201, children = listOf(summary(301)))
        val repository = DetailFakeRepository(
            mutableListOf(
                AppResult.Success(parent),
                AppResult.Failure(AppError.Offline),
            ),
        )
        val coordinator = MediaDetailCoordinator(repository)
        coordinator.load(session(), 201)

        coordinator.rememberFocusedChild(301)
        coordinator.loadFocusedChild(session(), 301)

        val content = coordinator.state.value as MediaDetailUiState.Content
        assertEquals(parent, content.parent)
        assertEquals(301L, content.focusedChildId)
        assertEquals(null, content.focusedChildDetail)
        assertEquals(AppError.Offline, content.childDetailError)
    }
}

private class DetailFakeRepository(
    private val details: MutableList<AppResult<MediaDetail>>,
) : MediaRepository {
    val detailCalls = mutableListOf<Long>()

    override suspend fun getLibraries(
        session: Session,
    ): AppResult<List<MediaLibrary>> = AppResult.Success(emptyList())

    override suspend fun getMediaPage(
        session: Session,
        libraryId: Long,
        pageNumber: Int,
        pageSize: Int,
        keyword: String?,
    ): AppResult<MediaPage> = AppResult.Success(MediaPage(emptyList(), 0, 1, 20, false))

    override suspend fun getMediaDetail(
        session: Session,
        mediaId: Long,
    ): AppResult<MediaDetail> {
        detailCalls += mediaId
        return details.removeAt(0)
    }

    override suspend fun getMediaProbe(
        session: Session,
        path: String,
    ): AppResult<org.kaloscope.tv.core.model.MediaProbe> = error("Not used")

    override suspend fun getSubtitleTracks(
        session: Session,
        path: String,
    ): AppResult<List<SubtitleTrack>> = error("Not used")

    override suspend fun getDanmakus(
        session: Session,
        path: String,
    ): AppResult<List<DanmakuComment>> = error("Not used")
}

private fun detail(
    id: Long,
    title: String = "群星档案",
    children: List<MediaSummary> = emptyList(),
) = MediaDetail(
    id = id,
    library = MediaLibrary(21, "剧集库", MediaLibraryType.TvShow),
    title = title,
    path = "/media/$id",
    posterPath = null,
    backdropPath = null,
    year = 2026,
    rating = 8.8,
    season = null,
    episode = null,
    aired = null,
    plot = "简介",
    genres = listOf("科幻"),
    directors = emptyList(),
    writers = emptyList(),
    studios = emptyList(),
    actors = listOf(MediaActor("沈川", "队长", null)),
    children = children,
)

private fun summary(
    id: Long,
    season: Int = 1,
) = MediaSummary(
    id = id,
    title = "启程",
    path = "/media/$id",
    posterPath = null,
    backdropPath = null,
    year = 2026,
    rating = null,
    season = season,
    episode = 1,
)

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
