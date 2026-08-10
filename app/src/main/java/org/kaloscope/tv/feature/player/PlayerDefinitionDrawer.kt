package org.kaloscope.tv.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSelectionRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSessionHint
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSize
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.model.NetworkDefinition

@Composable
internal fun PlayerDefinitionDrawer(
    definitions: List<NetworkDefinition>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val initialIndex = selectedIndex
        ?.takeIf(definitions.indices::contains)
        ?: 0
    LaunchedEffect(definitions, selectedIndex) {
        if (definitions.isNotEmpty()) {
            listState.scrollToItem(initialIndex)
            withFrameNanos { }
            initialFocus.requestFocus()
        }
    }
    KaloscopeSidePanel(
        title = stringResource(R.string.playback_quality),
        palette = KaloscopeSidePanelPalette(
            panelColor = Panel,
            textColor = OnBackground,
            mutedColor = Muted,
        ),
        onDismiss = onDismiss,
        size = KaloscopeSidePanelSize.Compact,
        modifier = Modifier.testTag("player-definition-drawer"),
        footer = {
            KaloscopeSidePanelSessionHint(
                text = stringResource(R.string.player_session_settings_description),
                color = Muted,
            )
        },
    ) {
        if (definitions.isEmpty()) {
            Text(
                text = stringResource(R.string.playback_quality_empty),
                color = Muted,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(definitions) { index, definition ->
                    KaloscopeSidePanelSelectionRow(
                        title = definition.label,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
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
}
