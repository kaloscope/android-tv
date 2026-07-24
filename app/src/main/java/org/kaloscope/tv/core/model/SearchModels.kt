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
    Dash,
    Mp4,
    Unknown,
}

data class NetworkDefinition(
    val label: String,
    val url: String,
)

data class NetworkChapter(
    val id: String?,
    val url: String?,
    val title: String,
    val volume: String?,
)

data class NetworkPlaybackSource(
    val indexerId: Long,
    val resourceId: String,
    val title: String,
    val url: String,
    val videoType: NetworkVideoType,
    val danmakus: List<DanmakuComment>,
    val definitions: List<NetworkDefinition> = emptyList(),
    val chapters: List<NetworkChapter> = emptyList(),
    val selectedDefinitionIndex: Int? = null,
    val selectedChapterIndex: Int? = null,
) {
    val selectedDefinition: NetworkDefinition?
        get() = selectedDefinitionIndex?.let(definitions::getOrNull)
}
