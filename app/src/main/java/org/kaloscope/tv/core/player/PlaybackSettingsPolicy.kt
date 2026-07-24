package org.kaloscope.tv.core.player

import androidx.media3.common.Player

object PlaybackSettingsPolicy {
    fun shouldAutoAdvance(
        playbackState: Int,
        autoplayNext: Boolean,
        hasNext: Boolean,
    ): Boolean =
        playbackState == Player.STATE_ENDED && autoplayNext && hasNext
}
