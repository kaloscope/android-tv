package org.kaloscope.tv.feature.player

internal enum class PlayerControlLayer {
    Hidden,
    Preview,
    Controls,
}

internal data class PlayerControlLayerTransition(
    val layer: PlayerControlLayer,
    val focusTarget: PlayerControlFocusTarget? = null,
    val actionRowVisible: Boolean? = null,
)

private const val PREVIEW_AUTO_HIDE_MILLIS = 2_600L
private const val CONTROLS_AUTO_HIDE_MILLIS = 4_000L

internal object PlayerControlLayerPolicy {
    fun initialTransition(): PlayerControlLayerTransition =
        PlayerControlLayerTransition(
            layer = PlayerControlLayer.Controls,
            focusTarget = PlayerControlFocusTarget.Progress,
            actionRowVisible = false,
        )

    fun autoHideDelayMillis(layer: PlayerControlLayer): Long? =
        when (layer) {
            PlayerControlLayer.Hidden -> null
            PlayerControlLayer.Preview -> PREVIEW_AUTO_HIDE_MILLIS
            PlayerControlLayer.Controls -> CONTROLS_AUTO_HIDE_MILLIS
        }

    fun transition(command: PlayerControlCommand): PlayerControlLayerTransition? =
        when (command) {
            PlayerControlCommand.ShowPreview,
            is PlayerControlCommand.SeekAndShowPreview,
            -> PlayerControlLayerTransition(PlayerControlLayer.Preview)

            PlayerControlCommand.TogglePlaybackAndShowControls ->
                PlayerControlLayerTransition(
                    layer = PlayerControlLayer.Controls,
                    focusTarget = PlayerControlFocusTarget.Progress,
                    actionRowVisible = false,
                )

            is PlayerControlCommand.ShowFullControls ->
                PlayerControlLayerTransition(
                    layer = PlayerControlLayer.Controls,
                    focusTarget = command.focusTarget,
                    actionRowVisible =
                        command.focusTarget == PlayerControlFocusTarget.PlayPause,
                )

            PlayerControlCommand.HideControls ->
                PlayerControlLayerTransition(PlayerControlLayer.Hidden)

            else -> null
        }
}
