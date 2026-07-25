package org.kaloscope.tv.feature.player

import org.kaloscope.tv.core.model.DanmakuSettings

internal interface DanmakuRuntimeControl {
    fun updateSettings(settings: DanmakuSettings)

    fun start()

    fun pause()

    fun seekTo(positionMillis: Long)

    fun updatePlaybackSpeed(speed: Float)
}

internal class DanmakuPlaybackSynchronizer(
    private val runtime: DanmakuRuntimeControl,
) {
    private var released = false

    fun onIsPlayingChanged(
        isPlaying: Boolean,
        positionMillis: Long,
        playbackSpeed: Float,
    ) {
        if (released) {
            return
        }
        if (isPlaying) {
            runtime.updatePlaybackSpeed(playbackSpeed.sanitized())
            runtime.seekTo(positionMillis.coerceAtLeast(0))
            runtime.start()
        } else {
            runtime.pause()
        }
    }

    fun onPositionDiscontinuity(positionMillis: Long) {
        if (!released) {
            runtime.seekTo(positionMillis.coerceAtLeast(0))
        }
    }

    fun onPlaybackSpeedChanged(speed: Float) {
        if (!released) {
            runtime.updatePlaybackSpeed(speed.sanitized())
        }
    }

    fun onSettingsChanged(settings: DanmakuSettings) {
        if (!released) {
            runtime.updateSettings(settings)
        }
    }

    fun dispose() {
        if (released) {
            return
        }
        released = true
    }

    private fun Float.sanitized(): Float =
        takeIf { isFinite() && this > 0f } ?: 1f
}
