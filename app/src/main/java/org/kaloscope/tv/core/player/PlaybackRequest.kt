package org.kaloscope.tv.core.player

sealed interface PlaybackRequest {
    val requestId: String
    val serverId: String
    val title: String
    val origin: PlaybackOrigin

    data class LocalMedia(
        override val requestId: String,
        override val serverId: String,
        val mediaId: Long,
        val path: String,
        override val title: String,
        val resumePositionSeconds: Long?,
        override val origin: PlaybackOrigin,
        val playbackMode: PlaybackMode = PlaybackMode.Auto,
        val transcodeResolution: TranscodeResolution = TranscodeResolution.P1080,
    ) : PlaybackRequest
}

enum class PlaybackOrigin {
    Home,
    MediaDetail,
}
