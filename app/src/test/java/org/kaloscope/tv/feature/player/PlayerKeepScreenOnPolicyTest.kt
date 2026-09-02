package org.kaloscope.tv.feature.player

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerKeepScreenOnPolicyTest {
    @Test
    fun `requested playback keeps the screen on while buffering or ready`() {
        assertTrue(
            PlayerKeepScreenOnPolicy.shouldKeepScreenOn(
                playWhenReady = true,
                playbackState = Player.STATE_BUFFERING,
                hasFailure = false,
            ),
        )
        assertTrue(
            PlayerKeepScreenOnPolicy.shouldKeepScreenOn(
                playWhenReady = true,
                playbackState = Player.STATE_READY,
                hasFailure = false,
            ),
        )
    }

    @Test
    fun `paused ended idle or failed playback lets the screen sleep`() {
        assertFalse(
            PlayerKeepScreenOnPolicy.shouldKeepScreenOn(
                playWhenReady = false,
                playbackState = Player.STATE_READY,
                hasFailure = false,
            ),
        )
        assertFalse(
            PlayerKeepScreenOnPolicy.shouldKeepScreenOn(
                playWhenReady = true,
                playbackState = Player.STATE_ENDED,
                hasFailure = false,
            ),
        )
        assertFalse(
            PlayerKeepScreenOnPolicy.shouldKeepScreenOn(
                playWhenReady = true,
                playbackState = Player.STATE_IDLE,
                hasFailure = false,
            ),
        )
        assertFalse(
            PlayerKeepScreenOnPolicy.shouldKeepScreenOn(
                playWhenReady = true,
                playbackState = Player.STATE_READY,
                hasFailure = true,
            ),
        )
    }
}
