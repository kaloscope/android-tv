package org.kaloscope.tv.core.model

data class SubtitleTrack(
    val id: String,
    val label: String,
    val url: String,
    val language: String?,
)

data class DanmakuComment(
    val id: String?,
    val text: String,
    val mode: String,
    val color: String?,
    val startMillis: Long,
)

data class MediaProbe(
    val durationMillis: Long,
    val chapters: List<MediaChapter>,
)

data class MediaChapter(
    val id: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
)
