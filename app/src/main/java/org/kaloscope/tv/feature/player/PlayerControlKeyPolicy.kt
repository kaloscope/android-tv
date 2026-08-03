package org.kaloscope.tv.feature.player

internal enum class PlayerRemoteKey {
    Center,
    Left,
    Right,
    Up,
    Down,
    Back,
}

internal enum class PlayerKeyPhase {
    Down,
    Up,
}

internal enum class PlayerControlContext {
    HiddenControls,
    Preview,
    Progress,
}

internal enum class PlayerControlFocusTarget {
    Progress,
    PlayPause,
}

internal enum class PlayerBackContext {
    SettingsDrawer,
    SpeedDrawer,
    DefinitionDrawer,
    Controls,
    Player,
}

internal sealed interface PlayerControlCommand {
    data object TogglePlaybackAndShowControls : PlayerControlCommand

    data class SeekAndShowPreview(
        val offsetMillis: Long,
    ) : PlayerControlCommand

    data object ShowPreview : PlayerControlCommand

    data class ShowFullControls(
        val focusTarget: PlayerControlFocusTarget,
    ) : PlayerControlCommand

    data class PreviewSeek(
        val offsetMillis: Long,
    ) : PlayerControlCommand

    data object SubmitSeekPreview : PlayerControlCommand

    data object HideControls : PlayerControlCommand

    data object CloseSettingsDrawer : PlayerControlCommand

    data object CloseDefinitionDrawer : PlayerControlCommand

    data object CloseSpeedDrawer : PlayerControlCommand

    data object ExitPlayer : PlayerControlCommand
}

internal object PlayerControlKeyPolicy {
    const val SEEK_INCREMENT_MILLIS = 10_000L

    fun command(
        context: PlayerControlContext,
        key: PlayerRemoteKey,
        phase: PlayerKeyPhase,
    ): PlayerControlCommand? =
        when (context) {
            PlayerControlContext.HiddenControls -> hiddenControlsCommand(key, phase)
            PlayerControlContext.Preview -> previewCommand(key, phase)
            PlayerControlContext.Progress -> progressCommand(key, phase)
        }

    fun previewTarget(
        currentTargetMillis: Long,
        durationMillis: Long,
        offsetMillis: Long,
    ): Long? {
        if (durationMillis <= 0) {
            return null
        }
        return (currentTargetMillis + offsetMillis).coerceIn(0, durationMillis)
    }

    fun backCommand(context: PlayerBackContext): PlayerControlCommand =
        when (context) {
            PlayerBackContext.SettingsDrawer -> PlayerControlCommand.CloseSettingsDrawer
            PlayerBackContext.SpeedDrawer -> PlayerControlCommand.CloseSpeedDrawer
            PlayerBackContext.DefinitionDrawer -> PlayerControlCommand.CloseDefinitionDrawer
            PlayerBackContext.Controls -> PlayerControlCommand.HideControls
            PlayerBackContext.Player -> PlayerControlCommand.ExitPlayer
        }

    private fun hiddenControlsCommand(
        key: PlayerRemoteKey,
        phase: PlayerKeyPhase,
    ): PlayerControlCommand? {
        if (
            phase == PlayerKeyPhase.Up &&
            (key == PlayerRemoteKey.Left || key == PlayerRemoteKey.Right)
        ) {
            return PlayerControlCommand.SubmitSeekPreview
        }
        if (phase != PlayerKeyPhase.Down) {
            return null
        }
        return when (key) {
            PlayerRemoteKey.Center ->
                PlayerControlCommand.TogglePlaybackAndShowControls

            PlayerRemoteKey.Left ->
                PlayerControlCommand.SeekAndShowPreview(-SEEK_INCREMENT_MILLIS)

            PlayerRemoteKey.Right ->
                PlayerControlCommand.SeekAndShowPreview(SEEK_INCREMENT_MILLIS)

            PlayerRemoteKey.Up,
            PlayerRemoteKey.Down,
            -> PlayerControlCommand.ShowPreview

            PlayerRemoteKey.Back -> null
        }
    }

    private fun previewCommand(
        key: PlayerRemoteKey,
        phase: PlayerKeyPhase,
    ): PlayerControlCommand? {
        if (
            phase == PlayerKeyPhase.Up &&
            (key == PlayerRemoteKey.Left || key == PlayerRemoteKey.Right)
        ) {
            return PlayerControlCommand.SubmitSeekPreview
        }
        if (phase != PlayerKeyPhase.Down) {
            return null
        }
        return when (key) {
            PlayerRemoteKey.Center ->
                PlayerControlCommand.TogglePlaybackAndShowControls

            PlayerRemoteKey.Left ->
                PlayerControlCommand.SeekAndShowPreview(-SEEK_INCREMENT_MILLIS)

            PlayerRemoteKey.Right ->
                PlayerControlCommand.SeekAndShowPreview(SEEK_INCREMENT_MILLIS)

            PlayerRemoteKey.Up ->
                PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.Progress)

            PlayerRemoteKey.Down ->
                PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.PlayPause)

            PlayerRemoteKey.Back -> PlayerControlCommand.HideControls
        }
    }

    private fun progressCommand(
        key: PlayerRemoteKey,
        phase: PlayerKeyPhase,
    ): PlayerControlCommand? =
        when {
            phase == PlayerKeyPhase.Down && key == PlayerRemoteKey.Left ->
                PlayerControlCommand.PreviewSeek(-SEEK_INCREMENT_MILLIS)

            phase == PlayerKeyPhase.Down && key == PlayerRemoteKey.Right ->
                PlayerControlCommand.PreviewSeek(SEEK_INCREMENT_MILLIS)

            phase == PlayerKeyPhase.Up &&
                (key == PlayerRemoteKey.Left || key == PlayerRemoteKey.Right) ->
                PlayerControlCommand.SubmitSeekPreview

            phase == PlayerKeyPhase.Down && key == PlayerRemoteKey.Center ->
                PlayerControlCommand.TogglePlaybackAndShowControls

            phase == PlayerKeyPhase.Down && key == PlayerRemoteKey.Down ->
                PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.PlayPause)

            phase == PlayerKeyPhase.Down && key == PlayerRemoteKey.Back ->
                PlayerControlCommand.HideControls

            else -> null
        }
}
