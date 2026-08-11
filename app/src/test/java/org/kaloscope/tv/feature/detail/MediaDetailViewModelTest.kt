package org.kaloscope.tv.feature.detail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.MediaProbe
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.data.media.MediaRepository

@OptIn(ExperimentalCoroutinesApi::class)
class MediaDetailViewModelTest {
    @Test
    fun `rapid focus changes load only the settled child detail`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = DetailViewModelFakeRepository(detailFixtures())
        val viewModel = MediaDetailViewModel(repository)
        try {
            viewModel.load(session(), 201)
            runCurrent()

            viewModel.rememberFocusedChild(301)
            viewModel.rememberFocusedChild(302)
            advanceUntilIdle()

            assertEquals(listOf(201L, 302L), repository.detailCalls)
        } finally {
            viewModel.reset()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `returning to a loaded child reuses its cached detail`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = DetailViewModelFakeRepository(detailFixtures())
        val viewModel = MediaDetailViewModel(repository)
        try {
            viewModel.load(session(), 201)
            advanceUntilIdle()

            val firstContent = viewModel.uiState.value as MediaDetailUiState.Content
            assertEquals("第一集简介", firstContent.focusedChildDetail?.plot)

            viewModel.rememberFocusedChild(302)
            advanceUntilIdle()
            viewModel.rememberFocusedChild(301)
            advanceUntilIdle()

            assertEquals(listOf(201L, 301L, 302L), repository.detailCalls)
        } finally {
            viewModel.reset()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `child detail failure preserves parent content for fallback`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val parent = detailFixtures().getValue(201L)
        val repository = DetailViewModelFakeRepository(mapOf(201L to parent))
        val viewModel = MediaDetailViewModel(repository)
        try {
            viewModel.load(session(), 201)
            advanceUntilIdle()

            val content = viewModel.uiState.value as MediaDetailUiState.Content
            assertEquals(parent, content.parent)
            assertEquals(301L, content.focusedChildId)
            assertEquals(null, content.focusedChildDetail)
            assertEquals(AppError.NotFound, content.childDetailError)
        } finally {
            viewModel.reset()
            Dispatchers.resetMain()
        }
    }
}

private class DetailViewModelFakeRepository(
    private val details: Map<Long, MediaDetail>,
) : MediaRepository {
    val detailCalls = mutableListOf<Long>()

    override suspend fun getLibraries(session: Session): AppResult<List<MediaLibrary>> =
        AppResult.Success(emptyList())

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
        return details[mediaId]
            ?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.NotFound)
    }

    override suspend fun getMediaProbe(
        session: Session,
        path: String,
    ): AppResult<MediaProbe> = error("Not used")

    override suspend fun getSubtitleTracks(
        session: Session,
        path: String,
    ): AppResult<List<SubtitleTrack>> = error("Not used")

    override suspend fun getDanmakus(
        session: Session,
        path: String,
    ): AppResult<List<DanmakuComment>> = error("Not used")
}

private fun detailFixtures(): Map<Long, MediaDetail> {
    val first = childSummary(301, "启程")
    val second = childSummary(302, "返程")
    return mapOf(
        201L to detail(201, "群星档案", "整部剧简介", children = listOf(first, second)),
        301L to detail(301, "启程", "第一集简介"),
        302L to detail(302, "返程", "第二集简介"),
    )
}

private fun detail(
    id: Long,
    title: String,
    plot: String,
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
    season = if (id == 201L) null else 1,
    episode = if (id == 201L) null else (id - 300).toInt(),
    aired = null,
    plot = plot,
    genres = listOf("科幻"),
    directors = emptyList(),
    writers = emptyList(),
    studios = emptyList(),
    actors = emptyList(),
    children = children,
)

private fun childSummary(id: Long, title: String) = MediaSummary(
    id = id,
    title = title,
    path = "/media/$id",
    posterPath = null,
    backdropPath = null,
    year = 2026,
    rating = null,
    season = 1,
    episode = (id - 300).toInt(),
)

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "fixture-token",
    user = SessionUser(1, "tv_user", "user"),
)
