package org.kaloscope.tv.data.search.remote

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Serializable
data class IndexerPageData(
    val total: Int? = null,
    val items: List<IndexerData> = emptyList(),
)

@Serializable
data class IndexerData(
    val id: Long,
    val name: String = "",
    val icon: String? = null,
    @SerialName("node_types")
    val nodeTypes: List<String> = emptyList(),
)

@Serializable
data class IndexerConfigData(
    val auth: IndexerAuthConfigData? = null,
    val search: IndexerSearchConfigData? = null,
    val details: IndexerDetailsConfigData? = null,
)

@Serializable
data class IndexerAuthConfigData(
    val login: IndexerLoginConfigData? = null,
)

@Serializable
data class IndexerLoginConfigData(
    val required: Boolean = false,
)

@Serializable
data class IndexerSearchConfigData(
    val display: IndexerDisplayConfigData? = null,
    val keyword: IndexerKeywordConfigData? = null,
)

@Serializable
data class IndexerDisplayConfigData(
    @SerialName("page_size")
    val pageSize: Int? = null,
    @SerialName("cover_ratio")
    val coverRatio: String? = null,
)

@Serializable
data class IndexerKeywordConfigData(
    val required: Boolean = true,
)

@Serializable
data class IndexerDetailsConfigData(
    val specific: IndexerDetailsSpecificData? = null,
)

@Serializable
data class IndexerDetailsSpecificData(
    @SerialName("media_type")
    val mediaType: String? = null,
    @SerialName("video_type")
    val videoType: String? = null,
)

@Serializable
data class IndexerAuthData(
    val name: String? = null,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class IndexerSearchRequestData(
    @SerialName("\$start")
    @EncodeDefault
    val start: String = "search_start",
    @SerialName("page_num")
    val pageNumber: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val keyword: String,
    @EncodeDefault
    val mobile: Boolean = false,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class IndexerDetailsRequestData(
    @SerialName("\$start")
    @EncodeDefault
    val start: String = "details_start",
    @SerialName("id")
    val resourceId: String,
    @SerialName("chapter_id")
    @EncodeDefault
    val chapterId: JsonElement = JsonNull,
    @SerialName("dash_supported")
    @EncodeDefault
    val dashSupported: Boolean = true,
    @EncodeDefault
    val ua: IndexerUserAgentData = IndexerUserAgentData(),
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class IndexerUserAgentData(
    @EncodeDefault
    val device: IndexerDeviceData = IndexerDeviceData(),
    @EncodeDefault
    val os: IndexerOsData = IndexerOsData(),
    @EncodeDefault
    val navigator: IndexerNavigatorData = IndexerNavigatorData(),
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class IndexerDeviceData(
    @EncodeDefault
    val type: String = "smarttv",
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class IndexerOsData(
    @EncodeDefault
    val name: String = "Android",
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class IndexerNavigatorData(
    @EncodeDefault
    val platform: String = "Android",
    @EncodeDefault
    val maxTouchPoints: Int = 0,
)

@Serializable
data class IndexerResourcePageData(
    val total: Int? = null,
    @SerialName("totalPages")
    val totalPages: Int? = null,
    val items: List<IndexerResourceData> = emptyList(),
)

@Serializable
data class IndexerResourceData(
    @Serializable(with = WorkflowIdSerializer::class)
    val id: String? = null,
    val title: String? = null,
    val cover: String? = null,
    val rating: JsonElement? = null,
    val category: String? = null,
    val uploader: String? = null,
    @SerialName("uploaded_at")
    val uploadedAt: String? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    val url: String? = null,
    @SerialName("video_type")
    val videoType: String? = null,
    val definitions: List<IndexerDefinitionData>? = null,
    val chapters: List<IndexerChapterData>? = null,
    val danmakus: List<IndexerDanmakuData>? = null,
)

@Serializable
data class IndexerDefinitionData(
    val url: String? = null,
    val definition: JsonElement? = null,
)

@Serializable
data class IndexerChapterData(
    @Serializable(with = WorkflowIdSerializer::class)
    val id: String? = null,
    val url: String? = null,
    val title: String? = null,
    val volume: String? = null,
)

@Serializable
data class IndexerDanmakuData(
    @Serializable(with = WorkflowIdSerializer::class)
    val id: String? = null,
    val text: String? = null,
    val mode: String? = null,
    val color: String? = null,
    val start: Long? = null,
)
