package org.kaloscope.tv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.model.DanmakuSettings

class DanmakuPlaybackSynchronizerTest {
    @Test
    fun `resume seeks before start and pause stops the runtime clock`() {
        val runtime = RecordingDanmakuRuntime()
        val synchronizer = DanmakuPlaybackSynchronizer(runtime)

        synchronizer.onIsPlayingChanged(
            isPlaying = true,
            positionMillis = 42_500,
            playbackSpeed = 1.25f,
        )
        synchronizer.onIsPlayingChanged(
            isPlaying = false,
            positionMillis = 43_000,
            playbackSpeed = 1.25f,
        )

        assertEquals(
            listOf("speed:1.25", "seek:42500", "start", "pause"),
            runtime.commands,
        )
    }

    @Test
    fun `position discontinuity clamps negative position without restarting`() {
        val runtime = RecordingDanmakuRuntime()
        val synchronizer = DanmakuPlaybackSynchronizer(runtime)

        synchronizer.onPositionDiscontinuity(-10)

        assertEquals(listOf("seek:0"), runtime.commands)
    }

    @Test
    fun `block setting update does not pause seek or restart`() {
        val runtime = RecordingDanmakuRuntime()
        val synchronizer = DanmakuPlaybackSynchronizer(runtime)

        synchronizer.onSettingsChanged(
            DanmakuSettings(
                opacityPercent = 50,
                blockColored = true,
            ),
        )

        assertEquals(listOf("settings:50"), runtime.commands)
    }

    @Test
    fun `invalid speed is sanitized and dispose stops forwarding without releasing runtime`() {
        val runtime = RecordingDanmakuRuntime()
        val synchronizer = DanmakuPlaybackSynchronizer(runtime)
        val settings = DanmakuSettings(opacityPercent = 50)

        synchronizer.onPlaybackSpeedChanged(0f)
        synchronizer.onSettingsChanged(settings)
        synchronizer.dispose()
        synchronizer.onPositionDiscontinuity(500)
        synchronizer.dispose()

        assertEquals(
            listOf("speed:1.0", "settings:50"),
            runtime.commands,
        )
    }
}
