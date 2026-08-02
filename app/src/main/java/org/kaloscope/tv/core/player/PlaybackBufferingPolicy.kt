package org.kaloscope.tv.core.player

import androidx.media3.common.Player

internal object PlaybackBufferingPolicy {
    fun hasBeenReady(
        previouslyReady: Boolean,
        playbackState: Int,
    ): Boolean = previouslyReady || playbackState == Player.STATE_READY

    fun isRebuffering(
        hasBeenReady: Boolean,
        playbackState: Int,
        playWhenReady: Boolean,
    ): Boolean =
        hasBeenReady &&
            playbackState == Player.STATE_BUFFERING &&
            playWhenReady
}
