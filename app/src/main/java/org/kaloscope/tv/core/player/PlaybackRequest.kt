package org.kaloscope.tv.core.player

import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.DanmakuSettings

sealed interface PlaybackRequest {
    val requestId: String
    val serverId: String
    val title: String
    val origin: PlaybackOrigin
    val autoplayNext: Boolean
    val danmakuSettings: DanmakuSettings
    val subtitleEnabled: Boolean

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
        val siblings: List<LocalEpisodeRef> = emptyList(),
        override val autoplayNext: Boolean = true,
        override val danmakuSettings: DanmakuSettings = DanmakuSettings(),
        override val subtitleEnabled: Boolean = true,
    ) : PlaybackRequest

    data class NetworkVideo(
        override val requestId: String,
        override val serverId: String,
        override val title: String,
        override val origin: PlaybackOrigin = PlaybackOrigin.NetworkSearch,
        val source: NetworkPlaybackSource,
        val resumePositionMillis: Long = 0,
        val preferredDefinition: TranscodeResolution = TranscodeResolution.P1080,
        override val autoplayNext: Boolean = true,
        override val danmakuSettings: DanmakuSettings = DanmakuSettings(),
        override val subtitleEnabled: Boolean = true,
    ) : PlaybackRequest
}

data class LocalEpisodeRef(
    val mediaId: Long,
    val path: String,
    val title: String,
)

enum class PlaybackOrigin {
    Home,
    MediaDetail,
    NetworkSearch,
}
