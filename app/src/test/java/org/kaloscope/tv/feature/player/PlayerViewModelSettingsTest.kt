package org.kaloscope.tv.feature.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.ProgressReason
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.data.history.HistoryRepository
import org.kaloscope.tv.data.media.MediaRepository
import org.kaloscope.tv.data.search.SearchRepository

class PlayerViewModelSettingsTest {
    @Test
    fun `history playback request uses persisted playback defaults`() {
        val store = PlaybackRequestStore()
        val viewModel = PlayerViewModel(
            requestStore = store,
            mediaRepository = unusedMediaRepository(),
            historyRepository = unusedHistoryRepository(),
            searchRepository = unusedSearchRepository(),
        )
        val expectedDanmaku = DanmakuSettings(
            enabled = false,
            speed = DanmakuSpeed.Fast,
            opacityPercent = 50,
        )
        val settings = TvSettings(
            playbackMode = PlaybackMode.Transcode,
            transcodeResolution = TranscodeResolution.P720,
            autoplayNext = false,
            danmaku = expectedDanmaku,
            subtitleEnabled = false,
        )

        val requestId = viewModel.createFromHistory(session(), history(), settings)

        val request = store.get(checkNotNull(requestId)) as PlaybackRequest.LocalMedia
        assertEquals(PlaybackMode.Transcode, request.playbackMode)
        assertEquals(TranscodeResolution.P720, request.transcodeResolution)
        assertFalse(request.autoplayNext)
        assertEquals(expectedDanmaku, request.danmakuSettings)
        assertFalse(request.subtitleEnabled)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `unknown duration still records local playback position`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val store = PlaybackRequestStore()
            val historyRepository = RecordingHistoryRepository()
            val viewModel = PlayerViewModel(
                requestStore = store,
                mediaRepository = unusedMediaRepository(),
                historyRepository = historyRepository,
                searchRepository = unusedSearchRepository(),
            )
            val requestId = checkNotNull(
                viewModel.createFromHistory(session(), history()),
            )
            val request = store.get(requestId) as PlaybackRequest.LocalMedia
            var savedCallbacks = 0

            viewModel.recordProgress(
                session = session(),
                request = request,
                positionMillis = 12_500,
                durationMillis = -1,
                reason = ProgressReason.Exit,
                nowMillis = 20_000,
                onSaved = { savedCallbacks += 1 },
            )
            advanceUntilIdle()

            assertEquals(12L, historyRepository.positionSeconds)
            assertEquals(0, historyRepository.percentage)
            assertEquals(1, savedCallbacks)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `progress writes for one media complete in playback order`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val store = PlaybackRequestStore()
            val historyRepository = SequencedHistoryRepository()
            val viewModel = PlayerViewModel(
                requestStore = store,
                mediaRepository = unusedMediaRepository(),
                historyRepository = historyRepository,
                searchRepository = unusedSearchRepository(),
            )
            val requestId = checkNotNull(
                viewModel.createFromHistory(session(), history()),
            )
            val request = store.get(requestId) as PlaybackRequest.LocalMedia

            viewModel.recordProgress(
                session(),
                request,
                positionMillis = 10_000,
                durationMillis = 60_000,
                reason = ProgressReason.Started,
                nowMillis = 0,
            )
            viewModel.recordProgress(
                session(),
                request,
                positionMillis = 20_000,
                durationMillis = 60_000,
                reason = ProgressReason.Seeked,
                nowMillis = 1_000,
            )
            advanceUntilIdle()

            assertEquals(listOf(10L, 20L), historyRepository.completedPositions)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class RecordingHistoryRepository : HistoryRepository {
    var positionSeconds: Long? = null
    var percentage: Int? = null

    override suspend fun getRecentVideos(
        session: Session,
    ): AppResult<List<WatchHistoryItem>> = error("Not used")

    override suspend fun recordVideoProgress(
        session: Session,
        mediaId: Long,
        positionSeconds: Long,
        percentage: Int,
    ): AppResult<Unit> {
        this.positionSeconds = positionSeconds
        this.percentage = percentage
        return AppResult.Success(Unit)
    }
}

private class SequencedHistoryRepository : HistoryRepository {
    val completedPositions = mutableListOf<Long>()

    override suspend fun getRecentVideos(
        session: Session,
    ): AppResult<List<WatchHistoryItem>> = error("Not used")

    override suspend fun recordVideoProgress(
        session: Session,
        mediaId: Long,
        positionSeconds: Long,
        percentage: Int,
    ): AppResult<Unit> {
        if (positionSeconds == 10L) {
            delay(100)
        }
        completedPositions += positionSeconds
        return AppResult.Success(Unit)
    }
}

private fun unusedHistoryRepository() = object : HistoryRepository {
    override suspend fun getRecentVideos(
        session: Session,
    ) = error("Not used")

    override suspend fun recordVideoProgress(
        session: Session,
        mediaId: Long,
        positionSeconds: Long,
        percentage: Int,
    ): AppResult<Unit> = error("Not used")
}

private fun unusedMediaRepository() = object : MediaRepository {
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
    ): AppResult<List<SubtitleTrack>> = error("Not used")

    override suspend fun getDanmakus(
        session: Session,
        path: String,
    ): AppResult<List<DanmakuComment>> = error("Not used")
}

private fun unusedSearchRepository() = object : SearchRepository {
    override suspend fun getIndexers(session: Session): AppResult<List<NetworkIndexer>> =
        error("Not used")

    override suspend fun getProfile(
        session: Session,
        indexer: NetworkIndexer,
    ) = error("Not used")

    override suspend fun search(
        session: Session,
        profile: org.kaloscope.tv.core.model.IndexerSourceProfile,
        keyword: String,
        pageNumber: Int,
    ): AppResult<NetworkSearchPage> = error("Not used")

    override suspend fun resolvePlayback(
        session: Session,
        indexerId: Long,
        result: NetworkSearchResult,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource> = error("Not used")

    override suspend fun resolveChapter(
        session: Session,
        source: NetworkPlaybackSource,
        chapterIndex: Int,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource> = error("Not used")
}

private fun session() = Session(
    server = SavedServer("server-1", "Home", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv", "user"),
)

private fun history() = WatchHistoryItem(
    historyId = 1,
    mediaId = 301,
    title = "Episode 1",
    fileName = "episode-1.mkv",
    path = "/episode-1.mkv",
    posterPath = null,
    backdropPath = null,
    year = null,
    season = 1,
    episode = 1,
    positionSeconds = 42,
    percentage = 20,
    rating = null,
    updatedAt = null,
)
