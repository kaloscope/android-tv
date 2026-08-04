package org.kaloscope.tv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerControlLayerPolicyTest {
    @Test
    fun `initial controls focus progress without revealing actions`() {
        assertEquals(
            PlayerControlLayerTransition(
                layer = PlayerControlLayer.Controls,
                focusTarget = PlayerControlFocusTarget.Progress,
                actionRowVisible = false,
            ),
            PlayerControlLayerPolicy.initialTransition(),
        )
    }

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
                actionRowVisible = false,
            ),
            PlayerControlLayerPolicy.transition(
                PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.Progress),
            ),
        )
        assertEquals(
            PlayerControlLayerTransition(
                layer = PlayerControlLayer.Controls,
                focusTarget = PlayerControlFocusTarget.Progress,
                actionRowVisible = false,
            ),
            PlayerControlLayerPolicy.transition(
                PlayerControlCommand.TogglePlaybackAndShowControls,
            ),
        )
        assertEquals(
            PlayerControlLayerTransition(
                layer = PlayerControlLayer.Controls,
                focusTarget = PlayerControlFocusTarget.PlayPause,
                actionRowVisible = true,
            ),
            PlayerControlLayerPolicy.transition(
                PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.PlayPause),
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
    fun `preview hides after the shared delay regardless of action row state`() {
        assertEquals(
            3_000L,
            PlayerControlLayerPolicy.autoHideDelayMillis(
                layer = PlayerControlLayer.Preview,
                actionRowVisible = false,
            ),
        )
        assertEquals(
            3_000L,
            PlayerControlLayerPolicy.autoHideDelayMillis(
                layer = PlayerControlLayer.Preview,
                actionRowVisible = true,
            ),
        )
    }

    @Test
    fun `full controls hide after the shared delay when actions are hidden`() {
        assertEquals(
            3_000L,
            PlayerControlLayerPolicy.autoHideDelayMillis(
                layer = PlayerControlLayer.Controls,
                actionRowVisible = false,
            ),
        )
    }

    @Test
    fun `full controls stay visible while actions are visible`() {
        assertEquals(
            null,
            PlayerControlLayerPolicy.autoHideDelayMillis(
                layer = PlayerControlLayer.Controls,
                actionRowVisible = true,
            ),
        )
    }

    @Test
    fun `hidden layer does not schedule auto hide`() {
        assertEquals(
            null,
            PlayerControlLayerPolicy.autoHideDelayMillis(
                layer = PlayerControlLayer.Hidden,
                actionRowVisible = true,
            ),
        )
    }
}
