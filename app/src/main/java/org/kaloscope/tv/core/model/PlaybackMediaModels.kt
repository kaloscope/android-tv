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
