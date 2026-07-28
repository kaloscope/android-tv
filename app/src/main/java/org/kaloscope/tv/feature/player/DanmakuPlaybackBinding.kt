package org.kaloscope.tv.feature.player

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player

internal class DanmakuPlaybackBinding(
    private val player: Player,
    private val synchronizer: DanmakuPlaybackSynchronizer,
) {
    private var listener: Player.Listener? = null
    private var disposed = false

    fun attach() {
        if (disposed || listener != null) {
            return
        }
        val attachedListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                synchronizer.onIsPlayingChanged(
                    isPlaying = isPlaying,
                    positionMillis = player.currentPosition,
                    playbackSpeed = player.playbackParameters.speed,
                )
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                synchronizer.onPositionDiscontinuity(player.currentPosition)
            }

            override fun onPlaybackParametersChanged(
                playbackParameters: PlaybackParameters,
            ) {
                synchronizer.onPlaybackSpeedChanged(playbackParameters.speed)
            }
        }
        player.addListener(attachedListener)
        listener = attachedListener
        synchronizer.onIsPlayingChanged(
            isPlaying = player.isPlaying,
            positionMillis = player.currentPosition,
            playbackSpeed = player.playbackParameters.speed,
        )
    }

    fun detach() {
        listener?.let(player::removeListener)
        listener = null
    }

    fun dispose() {
        if (disposed) {
            return
        }
        disposed = true
        detach()
        synchronizer.dispose()
    }
}
