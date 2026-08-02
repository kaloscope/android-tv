package org.kaloscope.tv.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackIntentPolicyTest {
    @Test
    fun `toggle pauses while playback still intends to play`() {
        assertFalse(
            PlaybackIntentPolicy.afterToggle(playWhenReady = true),
        )
    }

    @Test
    fun `toggle plays after a manual pause`() {
        assertTrue(
            PlaybackIntentPolicy.afterToggle(playWhenReady = false),
        )
    }
}
