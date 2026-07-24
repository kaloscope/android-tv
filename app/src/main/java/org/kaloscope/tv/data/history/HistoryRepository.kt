package org.kaloscope.tv.data.history

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem

interface HistoryRepository {
    suspend fun getRecentVideos(
        session: Session,
    ): AppResult<List<WatchHistoryItem>>

    suspend fun recordVideoProgress(
        session: Session,
        mediaId: Long,
        positionSeconds: Long,
        percentage: Int,
    ): AppResult<Unit>
}
