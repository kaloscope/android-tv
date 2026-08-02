package org.kaloscope.tv.core.player

import androidx.media3.common.Player

enum class PlaybackFeedback {
    Preparing,
    Ready,
    Rebuffering,
    FallingBack,
    SwitchingItem,
    Failed,
}

object PlaybackFeedbackPolicy {
    fun resolve(
        playbackState: Int,
        hasBeenReady: Boolean,
        fallbackInProgress: Boolean,
        switchingItem: Boolean,
        failure: PlaybackFailure?,
        playWhenReady: Boolean,
    ): PlaybackFeedback =
        when {
            failure != null -> PlaybackFeedback.Failed
            switchingItem -> PlaybackFeedback.SwitchingItem
            fallbackInProgress -> PlaybackFeedback.FallingBack
            !hasBeenReady && playbackState != Player.STATE_READY ->
                PlaybackFeedback.Preparing

            PlaybackBufferingPolicy.isRebuffering(
                hasBeenReady = hasBeenReady,
                playbackState = playbackState,
                playWhenReady = playWhenReady,
            ) ->
                PlaybackFeedback.Rebuffering

            else -> PlaybackFeedback.Ready
        }
}
