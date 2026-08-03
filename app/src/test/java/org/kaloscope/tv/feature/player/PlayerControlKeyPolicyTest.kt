package org.kaloscope.tv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerControlKeyPolicyTest {
    @Test
    fun `hidden controls map playback keys to the preview layer on key down`() {
        assertEquals(
            PlayerControlCommand.TogglePlaybackAndShowControls,
            command(PlayerRemoteKey.Center),
        )
        assertEquals(
            PlayerControlCommand.SeekAndShowPreview(-10_000),
            command(PlayerRemoteKey.Left),
        )
        assertEquals(
            PlayerControlCommand.SeekAndShowPreview(10_000),
            command(PlayerRemoteKey.Right),
        )
        assertEquals(
            PlayerControlCommand.ShowPreview,
            command(PlayerRemoteKey.Up),
        )
        assertEquals(
            PlayerControlCommand.ShowPreview,
            command(PlayerRemoteKey.Down),
        )
    }

    @Test
    fun `preview escalates vertical keys to the matching full controls focus`() {
        assertEquals(
            PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.Progress),
            previewCommand(PlayerRemoteKey.Up),
        )
        assertEquals(
            PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.PlayPause),
            previewCommand(PlayerRemoteKey.Down),
        )
    }

    @Test
    fun `preview keeps playback shortcuts lightweight and back hides it`() {
        assertEquals(
            PlayerControlCommand.TogglePlaybackAndShowControls,
            previewCommand(PlayerRemoteKey.Center),
        )
        assertEquals(
            PlayerControlCommand.SeekAndShowPreview(-10_000),
            previewCommand(PlayerRemoteKey.Left),
        )
        assertEquals(
            PlayerControlCommand.SeekAndShowPreview(10_000),
            previewCommand(PlayerRemoteKey.Right),
        )
        assertEquals(
            PlayerControlCommand.HideControls,
            previewCommand(PlayerRemoteKey.Back),
        )
        assertEquals(
            PlayerControlCommand.SubmitSeekPreview,
            PlayerControlKeyPolicy.command(
                context = PlayerControlContext.Preview,
                key = PlayerRemoteKey.Right,
                phase = PlayerKeyPhase.Up,
            ),
        )
    }

    @Test
    fun `hidden controls submit seek release and ignore unsupported releases`() {
        assertEquals(
            PlayerControlCommand.SubmitSeekPreview,
            PlayerControlKeyPolicy.command(
                context = PlayerControlContext.HiddenControls,
                key = PlayerRemoteKey.Left,
                phase = PlayerKeyPhase.Up,
            ),
        )
        assertNull(
            PlayerControlKeyPolicy.command(
                context = PlayerControlContext.HiddenControls,
                key = PlayerRemoteKey.Center,
                phase = PlayerKeyPhase.Up,
            ),
        )
        assertNull(command(PlayerRemoteKey.Back))
    }

    @Test
    fun `focused progress previews on key down and submits on key up`() {
        assertEquals(
            PlayerControlCommand.PreviewSeek(-10_000),
            PlayerControlKeyPolicy.command(
                context = PlayerControlContext.Progress,
                key = PlayerRemoteKey.Left,
                phase = PlayerKeyPhase.Down,
            ),
        )
        assertEquals(
            PlayerControlCommand.PreviewSeek(10_000),
            PlayerControlKeyPolicy.command(
                context = PlayerControlContext.Progress,
                key = PlayerRemoteKey.Right,
                phase = PlayerKeyPhase.Down,
            ),
        )
        assertEquals(
            PlayerControlCommand.SubmitSeekPreview,
            PlayerControlKeyPolicy.command(
                context = PlayerControlContext.Progress,
                key = PlayerRemoteKey.Right,
                phase = PlayerKeyPhase.Up,
            ),
        )
    }

    @Test
    fun `focused progress toggles playback and maps down and back`() {
        assertEquals(
            PlayerControlCommand.TogglePlaybackAndShowControls,
            progressCommand(PlayerRemoteKey.Center),
        )
        assertEquals(
            PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.PlayPause),
            progressCommand(PlayerRemoteKey.Down),
        )
        assertEquals(
            PlayerControlCommand.HideControls,
            progressCommand(PlayerRemoteKey.Back),
        )
        assertNull(
            PlayerControlKeyPolicy.command(
                context = PlayerControlContext.Progress,
                key = PlayerRemoteKey.Center,
                phase = PlayerKeyPhase.Up,
            ),
        )
    }

    @Test
    fun `seek preview clamps to playback boundaries`() {
        assertEquals(
            0L,
            PlayerControlKeyPolicy.previewTarget(
                currentTargetMillis = 5_000,
                durationMillis = 60_000,
                offsetMillis = -10_000,
            ),
        )
        assertEquals(
            60_000L,
            PlayerControlKeyPolicy.previewTarget(
                currentTargetMillis = 55_000,
                durationMillis = 60_000,
                offsetMillis = 10_000,
            ),
        )
        assertEquals(
            32_000L,
            PlayerControlKeyPolicy.previewTarget(
                currentTargetMillis = 22_000,
                durationMillis = 60_000,
                offsetMillis = 10_000,
            ),
        )
    }

    @Test
    fun `seek preview rejects unknown duration`() {
        assertNull(
            PlayerControlKeyPolicy.previewTarget(
                currentTargetMillis = 20_000,
                durationMillis = 0,
                offsetMillis = 10_000,
            ),
        )
        assertNull(
            PlayerControlKeyPolicy.previewTarget(
                currentTargetMillis = 20_000,
                durationMillis = -1,
                offsetMillis = -10_000,
            ),
        )
    }

    @Test
    fun `back closes the active player layer before leaving playback`() {
        assertEquals(
            PlayerControlCommand.CloseSettingsDrawer,
            PlayerControlKeyPolicy.backCommand(PlayerBackContext.SettingsDrawer),
        )
        assertEquals(
            PlayerControlCommand.CloseSpeedDrawer,
            PlayerControlKeyPolicy.backCommand(PlayerBackContext.SpeedDrawer),
        )
        assertEquals(
            PlayerControlCommand.CloseDefinitionDrawer,
            PlayerControlKeyPolicy.backCommand(PlayerBackContext.DefinitionDrawer),
        )
        assertEquals(
            PlayerControlCommand.HideControls,
            PlayerControlKeyPolicy.backCommand(PlayerBackContext.Controls),
        )
        assertEquals(
            PlayerControlCommand.ExitPlayer,
            PlayerControlKeyPolicy.backCommand(PlayerBackContext.Player),
        )
    }

    private fun command(key: PlayerRemoteKey): PlayerControlCommand? =
        PlayerControlKeyPolicy.command(
            context = PlayerControlContext.HiddenControls,
            key = key,
            phase = PlayerKeyPhase.Down,
        )

    private fun progressCommand(key: PlayerRemoteKey): PlayerControlCommand? =
        PlayerControlKeyPolicy.command(
            context = PlayerControlContext.Progress,
            key = key,
            phase = PlayerKeyPhase.Down,
        )

    private fun previewCommand(key: PlayerRemoteKey): PlayerControlCommand? =
        PlayerControlKeyPolicy.command(
            context = PlayerControlContext.Preview,
            key = key,
            phase = PlayerKeyPhase.Down,
        )
}
