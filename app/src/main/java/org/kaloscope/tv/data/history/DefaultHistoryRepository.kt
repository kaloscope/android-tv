package org.kaloscope.tv.data.history

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.HistoryItemData
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
}

internal fun HistoryItemData.toModel(): WatchHistoryItem? {
    // History can outlive deleted media, which must not create dead TV cards.
    val source = media ?: return null
    // Older servers may expose only the original file name.
    val resolvedTitle = source.title?.trim().orEmpty().ifBlank { source.name.trim() }
    if (source.id <= 0 || resolvedTitle.isBlank()) {
        return null
    }
    return WatchHistoryItem(
        historyId = id,
        mediaId = source.id,
        title = resolvedTitle,
        fileName = source.name,
        positionSeconds = position.coerceAtLeast(0),
        percentage = percentage.coerceIn(0, 100),
        year = source.year,
        season = source.season,
        episode = source.episode,
        posterPath = source.poster?.takeIf(String::isNotBlank),
        backdropPath = source.backdrop?.takeIf(String::isNotBlank),
        rating = source.rating?.toDoubleOrNull(),
        updatedAt = updatedAt,
    )
}
