package org.kaloscope.tv.core.player

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackBufferingPolicyTest {
    @Test
    fun `ready state latches for the controller lifetime`() {
        assertFalse(
            PlaybackBufferingPolicy.hasBeenReady(
                previouslyReady = false,
                playbackState = Player.STATE_BUFFERING,
            ),
        )
        assertTrue(
            PlaybackBufferingPolicy.hasBeenReady(
                previouslyReady = false,
                playbackState = Player.STATE_READY,
            ),
        )
        assertTrue(
            PlaybackBufferingPolicy.hasBeenReady(
                previouslyReady = true,
                playbackState = Player.STATE_IDLE,
            ),
        )
    }

    @Test
    fun `buffering before first ready is initial loading`() {
        assertFalse(
            PlaybackBufferingPolicy.isRebuffering(
                hasBeenReady = false,
                playbackState = Player.STATE_BUFFERING,
            ),
        )
    }

    @Test
    fun `buffering after ready is rebuffering`() {
        assertTrue(
            PlaybackBufferingPolicy.isRebuffering(
                hasBeenReady = true,
                playbackState = Player.STATE_BUFFERING,
            ),
        )
    }

    @Test
    fun `non buffering states never report rebuffering`() {
        listOf(
            Player.STATE_IDLE,
            Player.STATE_READY,
            Player.STATE_ENDED,
        ).forEach { playbackState ->
            assertFalse(
                PlaybackBufferingPolicy.isRebuffering(
                    hasBeenReady = true,
                    playbackState = playbackState,
                ),
            )
        }
    }
}
