package org.kaloscope.tv.feature.player

import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuView
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.DanmakuSettings

internal class AkDanmakuRuntime : DanmakuRuntimeControl {
    private val player = DanmakuPlayer(SimpleRenderer())
    private var config: DanmakuConfig = DanmakuSettings().toAkDanmakuConfig()
    private var released = false

    fun bind(view: DanmakuView) {
        if (!released) {
            player.bindView(view)
        }
    }

    fun load(comments: List<DanmakuComment>) {
        if (!released) {
            player.updateData(comments.toAkDanmakuData())
        }
    }

    override fun updateSettings(settings: DanmakuSettings) {
        if (released) {
            return
        }
        config = settings.toAkDanmakuConfig()
        player.updateConfig(config)
    }

    override fun start() {
        if (!released) {
            player.start(config)
        }
    }

    override fun pause() {
        if (!released) {
            player.pause()
        }
    }

    override fun seekTo(positionMillis: Long) {
        if (!released) {
            player.seekTo(positionMillis.coerceAtLeast(0))
        }
    }

    override fun updatePlaybackSpeed(speed: Float) {
        if (!released) {
            player.updatePlaySpeed(speed)
        }
    }

    fun release() {
        if (released) {
            return
        }
        released = true
        player.release()
    }
}
