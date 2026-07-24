package org.kaloscope.tv.feature.home

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.data.history.HistoryRepository

class HomeCoordinatorTest {
    @Test
    fun `loads real history into content state`() = runBlocking {
        val item = historyItem()
        val coordinator = HomeCoordinator(
            FakeHistoryRepository(AppResult.Success(listOf(item))),
        )

        coordinator.load(session())

        assertEquals(HomeUiState.Content(listOf(item)), coordinator.state.value)
    }

    @Test
    fun `uses empty state when server has no history`() = runBlocking {
        val coordinator = HomeCoordinator(
            FakeHistoryRepository(AppResult.Success(emptyList())),
        )

        coordinator.load(session())

        assertEquals(HomeUiState.Empty, coordinator.state.value)
    }

    @Test
    fun `exposes a retryable error when history fails`() = runBlocking {
        val coordinator = HomeCoordinator(
            FakeHistoryRepository(AppResult.Failure(AppError.Offline)),
        )

        coordinator.load(session())

        assertEquals(HomeUiState.Error(AppError.Offline), coordinator.state.value)
    }
}

private class FakeHistoryRepository(
    private val result: AppResult<List<WatchHistoryItem>>,
) : HistoryRepository {
    override suspend fun getRecentVideos(
        session: Session,
    ): AppResult<List<WatchHistoryItem>> = result

    override suspend fun recordVideoProgress(
        session: Session,
        mediaId: Long,
        positionSeconds: Long,
        percentage: Int,
    ): AppResult<Unit> = error("Not used")
}

private fun historyItem() = WatchHistoryItem(
    historyId = 401,
    mediaId = 301,
    title = "启程",
    fileName = "S01E01.mkv",
    path = "/media/tv/S01E01.mkv",
    positionSeconds = 1694,
    percentage = 63,
    year = 2026,
    season = 1,
    episode = 1,
    posterPath = null,
    backdropPath = null,
    rating = 8.5,
    updatedAt = "2026-07-23T08:00:00Z",
)

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
