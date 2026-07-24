package org.kaloscope.tv.feature.player

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.core.player.PlaybackOrigin
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.data.media.MediaRepository

class PlayerCoordinatorTest {
    @Test
    fun `load resolves local request and playback extras`() = runTest {
        val store = PlaybackRequestStore()
        val request = request()
        store.put(request)
        val coordinator = PlayerCoordinator(
            requestStore = store,
            mediaRepository = FakeMediaRepository(
                subtitles = AppResult.Success(listOf(subtitle())),
                danmakus = AppResult.Success(listOf(danmaku())),
            ),
        )

        coordinator.load(session(), request.requestId)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals(request, content.request)
        assertEquals("subtitle-1", content.subtitles.single().id)
        assertEquals(12_500, content.danmakus.single().startMillis)
        assertTrue(content.extraErrors.isEmpty())
    }

    @Test
    fun `supplementary failures do not block local playback`() = runTest {
        val store = PlaybackRequestStore()
        val request = request()
        store.put(request)
        val coordinator = PlayerCoordinator(
            requestStore = store,
            mediaRepository = FakeMediaRepository(
                subtitles = AppResult.Failure(AppError.Offline),
                danmakus = AppResult.Failure(AppError.Timeout),
            ),
        )

        coordinator.load(session(), request.requestId)

        val content = coordinator.state.value as PlayerUiState.Content
        assertTrue(content.subtitles.isEmpty())
        assertTrue(content.danmakus.isEmpty())
        assertEquals(setOf(PlayerExtra.Subtitles, PlayerExtra.Danmakus), content.extraErrors)
    }

    @Test
    fun `missing request produces recoverable state`() = runTest {
        val coordinator = PlayerCoordinator(
            requestStore = PlaybackRequestStore(),
            mediaRepository = FakeMediaRepository(),
        )

        coordinator.load(session(), "missing")

        assertEquals(PlayerUiState.MissingRequest, coordinator.state.value)
    }

    @Test
    fun `progress failure keeps playback content available`() = runTest {
        val store = PlaybackRequestStore()
        val request = request()
        store.put(request)
        val coordinator = PlayerCoordinator(store, FakeMediaRepository())
        coordinator.load(session(), request.requestId)

        coordinator.reportProgressFailure(AppError.Offline)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals(request, content.request)
        assertEquals(AppError.Offline, content.progressError)
    }
}

private class FakeMediaRepository(
    private val subtitles: AppResult<List<SubtitleTrack>> = AppResult.Success(emptyList()),
    private val danmakus: AppResult<List<DanmakuComment>> = AppResult.Success(emptyList()),
) : MediaRepository {
    override suspend fun getLibraries(session: Session): AppResult<List<MediaLibrary>> =
        error("Not used")

    override suspend fun getMediaPage(
        session: Session,
        libraryId: Long,
        pageNumber: Int,
        pageSize: Int,
        keyword: String?,
    ): AppResult<MediaPage> = error("Not used")

    override suspend fun getMediaDetail(
        session: Session,
        mediaId: Long,
    ): AppResult<MediaDetail> = error("Not used")

    override suspend fun getSubtitleTracks(
        session: Session,
        path: String,
    ): AppResult<List<SubtitleTrack>> = subtitles

    override suspend fun getDanmakus(
        session: Session,
        path: String,
    ): AppResult<List<DanmakuComment>> = danmakus
}

private fun request() = PlaybackRequest.LocalMedia(
    requestId = "request-1",
    serverId = "server-1",
    mediaId = 301,
    path = "/media/video.mkv",
    title = "Episode 1",
    resumePositionSeconds = 42,
    origin = PlaybackOrigin.MediaDetail,
)

private fun subtitle() = SubtitleTrack(
    id = "subtitle-1",
    label = "简体中文",
    url = "/_api/subtitle/content?path=fixture",
    language = "zh-CN",
)

private fun danmaku() = DanmakuComment(
    id = "danmaku-1",
    text = "Ready",
    mode = "scroll",
    color = "#FFFFFF",
    startMillis = 12_500,
)

private fun session() = Session(
    server = org.kaloscope.tv.core.model.SavedServer(
        id = "server-1",
        name = "Home",
        origin = "http://127.0.0.1:8000",
    ),
    token = "fixture-token",
    user = SessionUser(
        id = 1,
        username = "tv",
        role = "user",
    ),
)
