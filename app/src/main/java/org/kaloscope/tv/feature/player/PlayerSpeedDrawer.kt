package org.kaloscope.tv.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSelectionRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSessionHint
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSize
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel

internal val PlayerPlaybackSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
internal fun PlayerSpeedDrawer(
    speed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    val initialIndex = PlayerPlaybackSpeeds.indexOf(speed)
        .takeIf { it >= 0 }
        ?: 0
    LaunchedEffect(speed) {
        withFrameNanos { }
        initialFocus.requestFocus()
    }
    KaloscopeSidePanel(
        title = stringResource(R.string.playback_speed),
        palette = KaloscopeSidePanelPalette(
            panelColor = Panel,
            textColor = OnBackground,
            mutedColor = Muted,
        ),
        onDismiss = onDismiss,
        size = KaloscopeSidePanelSize.Compact,
        modifier = Modifier.testTag("player-speed-drawer"),
        footer = {
            KaloscopeSidePanelSessionHint(
                text = stringResource(R.string.player_session_settings_description),
                color = Muted,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(PlayerPlaybackSpeeds) { index, value ->
                KaloscopeSidePanelSelectionRow(
                    title = formatPlaybackSpeed(value),
                    selected = value == speed,
                    onClick = { onSelect(value) },
                    modifier = if (index == initialIndex) {
                        Modifier.focusRequester(initialFocus)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

internal fun formatPlaybackSpeed(speed: Float): String =
    when (speed) {
        1f, 2f -> "%.1fx".format(speed)
        else -> "${speed}x"
    }
