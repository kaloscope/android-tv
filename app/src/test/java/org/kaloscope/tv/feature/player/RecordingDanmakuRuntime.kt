package org.kaloscope.tv.feature.player

import org.kaloscope.tv.core.model.DanmakuSettings

internal class RecordingDanmakuRuntime : DanmakuRuntimeControl {
    val commands = mutableListOf<String>()

    override fun updateSettings(settings: DanmakuSettings) {
        commands += "settings:${settings.opacityPercent}"
    }

    override fun start() {
        commands += "start"
    }

    override fun pause() {
        commands += "pause"
    }

    override fun seekTo(positionMillis: Long) {
        commands += "seek:$positionMillis"
    }

    override fun updatePlaybackSpeed(speed: Float) {
        commands += "speed:$speed"
    }
}
