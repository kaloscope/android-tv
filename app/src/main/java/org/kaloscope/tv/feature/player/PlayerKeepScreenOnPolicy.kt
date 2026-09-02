package org.kaloscope.tv.feature.player

import androidx.media3.common.Player

internal object PlayerKeepScreenOnPolicy {
    fun shouldKeepScreenOn(
        playWhenReady: Boolean,
        playbackState: Int,
        hasFailure: Boolean,
    ): Boolean =
        !hasFailure &&
            playWhenReady &&
            (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY)
}
