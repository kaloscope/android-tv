package org.kaloscope.tv.core.player

import androidx.media3.common.Player

object PlaybackSettingsPolicy {
    val supportedSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    fun shouldAutoAdvance(
        playbackState: Int,
        playWhenReady: Boolean,
        autoplayNext: Boolean,
        hasNext: Boolean,
        switchingItem: Boolean,
    ): Boolean =
        // Reaching the end must respect pause intent and any pending manual episode selection.
        playbackState == Player.STATE_ENDED &&
            playWhenReady &&
            autoplayNext &&
            hasNext &&
            !switchingItem
}
