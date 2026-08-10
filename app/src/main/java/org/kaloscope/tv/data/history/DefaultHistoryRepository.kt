package org.kaloscope.tv.data.history

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.common.trimmedOrNull
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.HistoryItemData
import org.kaloscope.tv.core.network.HistoryMediaData
import org.kaloscope.tv.core.network.HistoryRecordData
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall

@Singleton
class DefaultHistoryRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val json: Json,
) : HistoryRepository {
    override suspend fun getRecentVideos(
        session: Session,
    ): AppResult<List<WatchHistoryItem>> =
        networkCall(json) {
            apiClientFactory.create(session.server.origin)
                .getVideoHistory("Token ${session.token}")
                .dataOrThrow()
                .also { result ->
                    if (result.total < 0) {
                        throw SerializationException("Invalid history total")
                    }
                }
                .items
                .mapNotNull(HistoryItemData::toModel)
        }

    override suspend fun recordVideoProgress(
        session: Session,
        mediaId: Long,
        positionSeconds: Long,
        percentage: Int,
    ): AppResult<Unit> =
        networkCall(json) {
            val response = apiClientFactory.create(session.server.origin)
                .recordVideoProgress(
                    authorization = "Token ${session.token}",
                    body = HistoryRecordData(
                        relationType = "video",
                        relationId = mediaId,
                        position = positionSeconds.coerceAtLeast(0),
                        percentage = percentage.coerceIn(0, 100),
                    ),
                )
            if (!response.isSuccessful) {
                throw retrofit2.HttpException(response)
            }
        }
}

internal fun HistoryItemData.toModel(): WatchHistoryItem? {
    // History can outlive deleted media, which must not create dead TV cards.
    val source = media ?: return null
    // Older servers may expose only the original file name.
    val resolvedTitle = source.resolvedTitle()
    if (source.id <= 0 || resolvedTitle.isBlank()) {
        return null
    }
    val parent = source.parent
    val posterPath = parent?.poster.trimmedOrNull()
        ?: source.poster.trimmedOrNull()
    val backdropPath = parent?.backdrop.trimmedOrNull()
        ?: source.backdrop.trimmedOrNull()
        ?: posterPath
    return WatchHistoryItem(
        historyId = id,
        mediaId = source.id,
        detailMediaId = parent?.id?.takeIf { it > 0 } ?: source.id,
        title = resolvedTitle,
        fileName = source.name,
        path = source.path,
        positionSeconds = position.coerceAtLeast(0),
        percentage = percentage.coerceIn(0, 100),
        year = parent?.year ?: source.year,
        season = source.season,
        episode = source.episode,
        posterPath = posterPath,
        backdropPath = backdropPath,
        rating = parent?.rating?.toDoubleOrNull() ?: source.rating?.toDoubleOrNull(),
        updatedAt = updatedAt,
        parentTitle = parent?.resolvedTitle().trimmedOrNull(),
    )
}

private fun HistoryMediaData.resolvedTitle(): String =
    title.trimmedOrNull() ?: name.trim()
