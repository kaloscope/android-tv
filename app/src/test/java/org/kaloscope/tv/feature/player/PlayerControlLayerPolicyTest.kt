package org.kaloscope.tv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerControlLayerPolicyTest {
    @Test
    fun `lightweight commands keep the player in preview`() {
        assertEquals(
            PlayerControlLayerTransition(PlayerControlLayer.Preview),
            PlayerControlLayerPolicy.transition(
                PlayerControlCommand.ShowPreview,
            ),
        )
        assertEquals(
            PlayerControlLayerTransition(PlayerControlLayer.Preview),
            PlayerControlLayerPolicy.transition(
                PlayerControlCommand.SeekAndShowPreview(10_000),
            ),
        )
    }

    @Test
    fun `full controls preserve the requested initial focus`() {
        assertEquals(
            PlayerControlLayerTransition(
                layer = PlayerControlLayer.Controls,
                focusTarget = PlayerControlFocusTarget.Progress,
            ),
            PlayerControlLayerPolicy.transition(
                PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.Progress),
            ),
        )
        assertEquals(
            PlayerControlLayerTransition(
                layer = PlayerControlLayer.Controls,
                focusTarget = PlayerControlFocusTarget.PlayPause,
            ),
            PlayerControlLayerPolicy.transition(
                PlayerControlCommand.TogglePlaybackAndShowControls,
            ),
        )
    }

    @Test
    fun `hide command returns to immersive playback`() {
        assertEquals(
            PlayerControlLayerTransition(PlayerControlLayer.Hidden),
            PlayerControlLayerPolicy.transition(PlayerControlCommand.HideControls),
        )
    }

    @Test
    fun `preview dismisses sooner than full controls`() {
        assertEquals(
            2_600L,
            PlayerControlLayerPolicy.autoHideDelayMillis(PlayerControlLayer.Preview),
        )
        assertEquals(
            4_000L,
            PlayerControlLayerPolicy.autoHideDelayMillis(PlayerControlLayer.Controls),
        )
        assertEquals(
            null,
            PlayerControlLayerPolicy.autoHideDelayMillis(PlayerControlLayer.Hidden),
        )
    }
}
