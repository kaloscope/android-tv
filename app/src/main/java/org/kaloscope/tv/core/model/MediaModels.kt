package org.kaloscope.tv.core.model

enum class MediaLibraryType {
    Movie,
    TvShow,
    Unknown,
}

data class MediaLibrary(
    val id: Long,
    val name: String,
    val type: MediaLibraryType,
)

data class MediaSummary(
    val id: Long,
    val title: String,
    val path: String,
    val posterPath: String?,
    val backdropPath: String?,
    val year: Int?,
    val rating: Double?,
    val season: Int?,
    val episode: Int?,
)

data class MediaPage(
    val items: List<MediaSummary>,
    val total: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val hasNext: Boolean,
)

data class MediaActor(
    val name: String,
    val role: String?,
    val thumbPath: String?,
)

data class MediaDetail(
    val id: Long,
    val library: MediaLibrary?,
    val title: String,
    val path: String,
    val posterPath: String?,
    val backdropPath: String?,
    val year: Int?,
    val rating: Double?,
    val season: Int?,
    val episode: Int?,
    val aired: String?,
    val plot: String?,
    val genres: List<String>,
    val directors: List<String>,
    val writers: List<String>,
    val studios: List<String>,
    val actors: List<MediaActor>,
    val children: List<MediaSummary>,
)
