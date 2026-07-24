package org.kaloscope.tv.core.model

data class NetworkIndexer(
    val id: Long,
    val name: String,
    val iconPath: String?,
)

data class IndexerSourceProfile(
    val indexer: NetworkIndexer,
    val pageSize: Int,
    val keywordRequired: Boolean,
    val webAuthRequired: Boolean,
)

data class NetworkSearchResult(
    val id: String,
    val title: String,
    val coverPath: String?,
    val rating: Double?,
    val category: String?,
    val uploader: String?,
    val uploadedAt: String?,
)

data class NetworkSearchPage(
    val items: List<NetworkSearchResult>,
    val total: Int?,
    val pageNumber: Int,
    val pageSize: Int,
    val hasNext: Boolean,
)

enum class NetworkVideoType {
    Hls,
    Mp4,
    Unknown,
}

data class NetworkPlaybackSource(
    val indexerId: Long,
    val resourceId: String,
    val title: String,
    val url: String,
    val videoType: NetworkVideoType,
    val danmakus: List<DanmakuComment>,
)
