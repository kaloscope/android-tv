package org.kaloscope.tv.core.player

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PlaybackStallWatchdog(
    private val scope: CoroutineScope,
    private val onTimeout: () -> Unit,
) {
    private var timeoutJob: Job? = null

    fun update(
        playbackState: Int,
        playWhenReady: Boolean,
        hasFailure: Boolean,
    ) {
        if (playbackState != Player.STATE_BUFFERING || !playWhenReady || hasFailure) {
            cancel()
        } else if (timeoutJob == null) {
            // Metadata and loading events must not extend a continuous buffering deadline.
            timeoutJob = scope.launch {
                delay(60_000)
                onTimeout()
            }
        }
    }

    fun cancel() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
}
