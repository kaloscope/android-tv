package org.kaloscope.tv.core.model

data class WatchHistoryItem(
    val historyId: Long,
    val mediaId: Long,
    val title: String,
    val fileName: String,
    val path: String,
    val positionSeconds: Long,
    val percentage: Int,
    val year: Int?,
    val season: Int?,
    val episode: Int?,
    val posterPath: String?,
    val backdropPath: String?,
    val rating: Double?,
    val updatedAt: String?,
)
