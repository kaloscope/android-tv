package org.kaloscope.tv.data.media.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MediaLibraryData(
    val id: Long,
    val name: String = "",
    @SerialName("lib_type")
    val libraryType: String = "",
)

@Serializable
data class MediaPageData(
    val total: Int = 0,
    val items: List<MediaItemData> = emptyList(),
)

@Serializable
data class MediaItemData(
    val id: Long,
    val name: String = "",
    val path: String = "",
    val visible: Boolean = true,
    val title: String? = null,
    val year: Int? = null,
    val aired: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val rating: JsonElement? = null,
    val lib: MediaLibraryData? = null,
    val children: List<MediaItemData> = emptyList(),
    val metadata: MediaMetadataData? = null,
)

@Serializable
data class MediaMetadataData(
    val plot: String? = null,
    val genres: List<String>? = null,
    val directors: List<String>? = null,
    val writers: List<String>? = null,
    val studios: List<String>? = null,
    val actors: List<MediaActorData>? = null,
    val poster: String? = null,
    val backdrop: String? = null,
)

@Serializable
data class MediaActorData(
    val name: String? = null,
    val role: String? = null,
    val thumb: String? = null,
)

@Serializable
data class SubtitleTrackData(
    val id: String = "",
    val label: String = "",
    val url: String? = null,
    val language: String? = null,
)

@Serializable
data class DanmakuWrapperData(
    val comments: List<DanmakuCommentData> = emptyList(),
)

@Serializable
data class DanmakuCommentData(
    val id: String? = null,
    val text: String = "",
    val mode: String? = null,
    val color: String? = null,
    val start: Long? = null,
)
