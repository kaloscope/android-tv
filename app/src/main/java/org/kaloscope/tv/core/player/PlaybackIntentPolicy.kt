package org.kaloscope.tv.core.player

internal object PlaybackIntentPolicy {
    fun afterToggle(playWhenReady: Boolean): Boolean = !playWhenReady
}
