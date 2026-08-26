package org.kaloscope.tv.feature.player

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
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
import org.kaloscope.tv.core.model.MediaProbe
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.core.player.PlaybackOrigin
import org.kaloscope.tv.core.player.PlaybackPreparationStage
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.data.media.MediaRepository

class PlayerCoordinatorTest {
    @Test
    fun `local load reports resource then danmaku preparation stages`() = runTest {
        val store = PlaybackRequestStore()
        val request = request()
        store.put(request)
        val subtitles = CompletableDeferred<AppResult<List<SubtitleTrack>>>()
        val probe = CompletableDeferred<AppResult<MediaProbe>>()
        val danmakus = CompletableDeferred<AppResult<List<DanmakuComment>>>()
        val coordinator = PlayerCoordinator(
            requestStore = store,
            mediaRepository = FakeMediaRepository(
                deferredSubtitles = subtitles,
                deferredDanmakus = danmakus,
                deferredProbe = probe,
            ),
        )

        val loadJob = launch { coordinator.load(session(), request.requestId) }
        runCurrent()

        assertEquals(
            PlaybackPreparationStage.Resource,
            (coordinator.state.value as PlayerUiState.Loading).stage,
        )

        subtitles.complete(AppResult.Success(emptyList()))
        probe.complete(AppResult.Success(probe()))
        runCurrent()

        assertEquals(
            PlaybackPreparationStage.Danmaku,
            (coordinator.state.value as PlayerUiState.Loading).stage,
        )

        danmakus.complete(AppResult.Success(listOf(danmaku())))
        loadJob.join()

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals("danmaku-1", content.danmakus.single().id)
    }

    @Test
    fun `load resolves local request and playback extras`() = runTest {
        val store = PlaybackRequestStore()
        val request = request()
        store.put(request)
        val repository = FakeMediaRepository(
            subtitles = AppResult.Success(listOf(subtitle())),
            danmakus = AppResult.Success(listOf(danmaku())),
            probe = AppResult.Success(probe()),
        )
        val coordinator = PlayerCoordinator(
            requestStore = store,
            mediaRepository = repository,
        )

        coordinator.load(session(), request.requestId)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals(request, content.request)
        assertEquals("subtitle-1", content.subtitles.single().id)
        assertEquals(12_500, content.danmakus.single().startMillis)
        assertEquals(90_000L, content.mediaProbe?.durationMillis)
        assertEquals(1, repository.probeCalls)
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
    fun `probe failure preserves playable content and other extras`() = runTest {
        val store = PlaybackRequestStore()
        val request = request()
        store.put(request)
        val coordinator = PlayerCoordinator(
            requestStore = store,
            mediaRepository = FakeMediaRepository(
                subtitles = AppResult.Success(listOf(subtitle())),
                danmakus = AppResult.Success(listOf(danmaku())),
                probe = AppResult.Failure(AppError.Offline),
            ),
        )

        coordinator.load(session(), request.requestId)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals(null, content.mediaProbe)
        assertEquals(setOf(PlayerExtra.MediaProbe), content.extraErrors)
        assertEquals("subtitle-1", content.subtitles.single().id)
        assertEquals("danmaku-1", content.danmakus.single().id)
    }

    @Test
    fun `network request uses resolved extras without local media endpoints`() = runTest {
        val store = PlaybackRequestStore()
        val request = PlaybackRequest.NetworkVideo(
            requestId = "network-request",
            serverId = "server-1",
            title = "Network video",
            source = org.kaloscope.tv.core.model.NetworkPlaybackSource(
                indexerId = 11,
                resourceId = "v1",
                title = "Network video",
                url = "/_api/media/proxy?id=1",
                videoType = org.kaloscope.tv.core.model.NetworkVideoType.Hls,
                danmakus = listOf(danmaku()),
            ),
        )
        store.put(request)
        val repository = FakeMediaRepository()
        val coordinator = PlayerCoordinator(store, repository)

        coordinator.load(session(), request.requestId)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals(request, content.request)
        assertTrue(content.subtitles.isEmpty())
        assertEquals("danmaku-1", content.danmakus.single().id)
        assertEquals(0, repository.subtitleCalls)
        assertEquals(0, repository.danmakuCalls)
        assertEquals(0, repository.probeCalls)
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

    @Test
    fun `network chapter failure preserves current playback`() = runTest {
        val store = PlaybackRequestStore()
        val request = networkRequest()
        store.put(request)
        val coordinator = PlayerCoordinator(store, FakeMediaRepository())
        coordinator.load(session(), request.requestId)

        coordinator.beginItemSwitch()
        coordinator.reportItemSwitchFailure(AppError.Offline)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals(request, content.request)
        assertEquals(AppError.Offline, content.switchError)
        assertTrue(!content.switchingItem)
    }

    @Test
    fun `network request replacement publishes the new chapter`() = runTest {
        val store = PlaybackRequestStore()
        val request = networkRequest()
        store.put(request)
        val repository = FakeMediaRepository()
        val coordinator = PlayerCoordinator(store, repository)
        coordinator.load(session(), request.requestId)
        val next = request.copy(
            title = "Episode 2",
            source = request.source.copy(
                title = "Episode 2",
                url = "https://cdn.example/episode-2.mpd",
                selectedChapterIndex = 1,
            ),
        )

        coordinator.replaceRequest(session(), next)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals(next, content.request)
        assertEquals(next, store.get(request.requestId))
        assertEquals(0, repository.subtitleCalls)
        assertEquals(0, repository.danmakuCalls)
        assertEquals(0, repository.probeCalls)
    }

    @Test
    fun `local request replacement reloads extras for the new path`() = runTest {
        val store = PlaybackRequestStore()
        val request = request()
        store.put(request)
        val repository = FakeMediaRepository(probe = AppResult.Success(probe()))
        val coordinator = PlayerCoordinator(store, repository)
        coordinator.load(session(), request.requestId)
        val next = request.copy(path = "/media/video-2.mkv", mediaId = 302)

        coordinator.replaceRequest(session(), next)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals(next, content.request)
        assertEquals(listOf("/media/video.mkv", "/media/video-2.mkv"), repository.probePaths)
    }

    @Test
    fun `subtitle retry replaces tracks and clears only subtitle failure`() = runTest {
        val store = PlaybackRequestStore()
        val request = request()
        store.put(request)
        val repository = FakeMediaRepository(
            subtitles = AppResult.Failure(AppError.Offline),
            danmakus = AppResult.Success(listOf(danmaku())),
        )
        val coordinator = PlayerCoordinator(store, repository)
        coordinator.load(session(), request.requestId)
        repository.subtitles = AppResult.Success(listOf(subtitle()))

        coordinator.retryExtra(session(), PlayerExtra.Subtitles)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals("subtitle-1", content.subtitles.single().id)
        assertEquals("danmaku-1", content.danmakus.single().id)
        assertTrue(PlayerExtra.Subtitles !in content.extraErrors)
        assertEquals(2, repository.subtitleCalls)
        assertEquals(1, repository.danmakuCalls)
        assertEquals(1, repository.probeCalls)
    }

    @Test
    fun `failed danmaku retry preserves subtitles and updates its error`() = runTest {
        val store = PlaybackRequestStore()
        val request = request()
        store.put(request)
        val repository = FakeMediaRepository(
            subtitles = AppResult.Success(listOf(subtitle())),
            danmakus = AppResult.Failure(AppError.Timeout),
        )
        val coordinator = PlayerCoordinator(store, repository)
        coordinator.load(session(), request.requestId)
        repository.danmakus = AppResult.Failure(AppError.Offline)

        coordinator.retryExtra(session(), PlayerExtra.Danmakus)

        val content = coordinator.state.value as PlayerUiState.Content
        assertEquals("subtitle-1", content.subtitles.single().id)
        assertTrue(content.danmakus.isEmpty())
        assertEquals(AppError.Offline, content.extraFailures[PlayerExtra.Danmakus])
        assertEquals(1, repository.subtitleCalls)
        assertEquals(2, repository.danmakuCalls)
        assertEquals(1, repository.probeCalls)
    }
}

private class FakeMediaRepository(
    var subtitles: AppResult<List<SubtitleTrack>> = AppResult.Success(emptyList()),
    var danmakus: AppResult<List<DanmakuComment>> = AppResult.Success(emptyList()),
    var probe: AppResult<MediaProbe> = AppResult.Success(MediaProbe(0, emptyList())),
    private val deferredSubtitles: CompletableDeferred<AppResult<List<SubtitleTrack>>>? = null,
    private val deferredDanmakus: CompletableDeferred<AppResult<List<DanmakuComment>>>? = null,
    private val deferredProbe: CompletableDeferred<AppResult<MediaProbe>>? = null,
) : MediaRepository {
    var subtitleCalls = 0
    var danmakuCalls = 0
    var probeCalls = 0
    val probePaths = mutableListOf<String>()

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

    override suspend fun getMediaProbe(
        session: Session,
        path: String,
    ): AppResult<MediaProbe> {
        probeCalls += 1
        probePaths += path
        return deferredProbe?.await() ?: probe
    }

    override suspend fun getSubtitleTracks(
        session: Session,
        path: String,
    ): AppResult<List<SubtitleTrack>> {
        subtitleCalls += 1
        return deferredSubtitles?.await() ?: subtitles
    }

    override suspend fun getDanmakus(
        session: Session,
        path: String,
    ): AppResult<List<DanmakuComment>> {
        danmakuCalls += 1
        return deferredDanmakus?.await() ?: danmakus
    }
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

private fun probe() = MediaProbe(
    durationMillis = 90_000,
    chapters = emptyList(),
)

private fun networkRequest() = PlaybackRequest.NetworkVideo(
    requestId = "network-request",
    serverId = "server-1",
    title = "Episode 1",
    source = org.kaloscope.tv.core.model.NetworkPlaybackSource(
        indexerId = 11,
        resourceId = "series-1",
        title = "Episode 1",
        url = "https://cdn.example/episode-1.mpd",
        videoType = org.kaloscope.tv.core.model.NetworkVideoType.Dash,
        danmakus = emptyList(),
        chapters = listOf(
            org.kaloscope.tv.core.model.NetworkChapter("ep-1", null, "Episode 1", null),
            org.kaloscope.tv.core.model.NetworkChapter("ep-2", null, "Episode 2", null),
        ),
        selectedChapterIndex = 0,
    ),
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
