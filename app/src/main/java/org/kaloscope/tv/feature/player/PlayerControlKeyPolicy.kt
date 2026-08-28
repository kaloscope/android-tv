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

    data object ExitPlayer : PlayerControlCommand
}

internal object PlayerControlKeyPolicy {
    const val SEEK_INCREMENT_MILLIS = 10_000L

    fun command(
        context: PlayerControlContext,
        key: PlayerRemoteKey,
        phase: PlayerKeyPhase,
    ): PlayerControlCommand? {
        if (phase == PlayerKeyPhase.Up) {
            return if (key == PlayerRemoteKey.Left || key == PlayerRemoteKey.Right) {
                PlayerControlCommand.SubmitSeekPreview
            } else {
                null
            }
        }
        return when (context) {
            PlayerControlContext.HiddenControls,
            PlayerControlContext.Preview,
            -> overlayCommand(context, key)

            PlayerControlContext.Progress -> progressCommand(key)
        }
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
            PlayerBackContext.Controls -> PlayerControlCommand.HideControls
            PlayerBackContext.Player -> PlayerControlCommand.ExitPlayer
        }

    private fun overlayCommand(
        context: PlayerControlContext,
        key: PlayerRemoteKey,
    ): PlayerControlCommand? =
        when (key) {
            PlayerRemoteKey.Center ->
                PlayerControlCommand.TogglePlaybackAndShowControls

            PlayerRemoteKey.Left ->
                PlayerControlCommand.SeekAndShowPreview(-SEEK_INCREMENT_MILLIS)

            PlayerRemoteKey.Right ->
                PlayerControlCommand.SeekAndShowPreview(SEEK_INCREMENT_MILLIS)

            PlayerRemoteKey.Up ->
                if (context == PlayerControlContext.HiddenControls) {
                    PlayerControlCommand.ShowPreview
                } else {
                    PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.Progress)
                }

            PlayerRemoteKey.Down ->
                if (context == PlayerControlContext.HiddenControls) {
                    PlayerControlCommand.ShowPreview
                } else {
                    PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.PlayPause)
                }

            PlayerRemoteKey.Back ->
                if (context == PlayerControlContext.Preview) {
                    PlayerControlCommand.HideControls
                } else {
                    null
                }
        }

    private fun progressCommand(
        key: PlayerRemoteKey,
    ): PlayerControlCommand? =
        when (key) {
            PlayerRemoteKey.Left ->
                PlayerControlCommand.PreviewSeek(-SEEK_INCREMENT_MILLIS)

            PlayerRemoteKey.Right ->
                PlayerControlCommand.PreviewSeek(SEEK_INCREMENT_MILLIS)

            PlayerRemoteKey.Center ->
                PlayerControlCommand.TogglePlaybackAndShowControls

            PlayerRemoteKey.Down ->
                PlayerControlCommand.ShowFullControls(PlayerControlFocusTarget.PlayPause)

            PlayerRemoteKey.Back ->
                PlayerControlCommand.HideControls

            PlayerRemoteKey.Up -> null
        }
}
