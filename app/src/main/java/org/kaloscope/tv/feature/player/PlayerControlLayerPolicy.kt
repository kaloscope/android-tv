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

private const val AUTO_HIDE_DELAY_MILLIS = 3_000L

internal object PlayerControlLayerPolicy {
    fun initialTransition(): PlayerControlLayerTransition =
        PlayerControlLayerTransition(
            layer = PlayerControlLayer.Controls,
            focusTarget = PlayerControlFocusTarget.Progress,
            actionRowVisible = false,
        )

    fun autoHideDelayMillis(
        layer: PlayerControlLayer,
        actionRowVisible: Boolean,
    ): Long? =
        when (layer) {
            PlayerControlLayer.Hidden -> null
            PlayerControlLayer.Preview -> AUTO_HIDE_DELAY_MILLIS
            PlayerControlLayer.Controls ->
                if (actionRowVisible) null else AUTO_HIDE_DELAY_MILLIS
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
