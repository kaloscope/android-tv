package org.kaloscope.tv.data.reader.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocalContentData(
    @SerialName("item_id") val itemId: Long,
    @SerialName("source_item_id") val sourceItemId: Long,
    val title: String,
    val version: String,
    @SerialName("chapter_id") val chapterId: String,
    val chapters: List<LocalChapterData>,
    @SerialName("media_type") val mediaType: String,
    val format: String,
    @SerialName("content_type") val contentType: String,
    val text: List<String> = emptyList(),
    val blocks: List<LocalBlockData> = emptyList(),
    val images: List<String> = emptyList(),
    val offset: Int = 0,
    @SerialName("image_count") val imageCount: Int = 0,
    @SerialName("next_offset") val nextOffset: Int? = null,
)

@Serializable
data class LocalChapterData(
    val id: String,
    val title: String? = null,
    val part: Int = 1,
    val volume: String? = null,
)

@Serializable
data class LocalRunData(val text: String, val marks: List<String> = emptyList())

@Serializable
data class LocalBlockData(
    val id: String,
    val type: String,
    val runs: List<LocalRunData> = emptyList(),
    val level: Int? = null,
    val ordered: Boolean = false,
    val items: List<List<LocalRunData>> = emptyList(),
    val start: Int? = null,
    val url: String? = null,
    val alt: String = "",
)

@Serializable
data class ReadingLocatorData(
    val version: String,
    @SerialName("chapter_id") val chapterId: String? = null,
    val paragraph: Int? = null,
    @SerialName("block_id") val blockId: String? = null,
    @SerialName("chapter_item_id") val chapterItemId: Long? = null,
    @SerialName("page_id") val pageId: String? = null,
    val offset: Double = 0.0,
)

@Serializable
data class ReadingRecordData(
    @SerialName("rel_type") val relationType: String,
    @SerialName("rel_id") val relationId: Long,
    val percentage: Int,
    val locator: ReadingLocatorData,
)

@Serializable
data class ReadingHistoryData(
    @SerialName("rel_type") val relationType: String,
    @SerialName("rel_id") val relationId: Long,
    val percentage: Int? = null,
    val locator: ReadingLocatorData? = null,
)

@Serializable
data class ReadingHistoryPageData(val items: List<ReadingHistoryData> = emptyList())
