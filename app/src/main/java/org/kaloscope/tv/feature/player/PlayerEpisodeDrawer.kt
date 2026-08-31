package org.kaloscope.tv.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSelectionRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSide
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
internal fun PlayerEpisodeDrawer(
    session: Session,
    episodes: List<PlayerEpisodeEntry>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val initialIndex = episodes.indexOfFirst(PlayerEpisodeEntry::selected)
        .takeIf { it >= 0 }
        ?: 0
    LaunchedEffect(episodes) {
        if (episodes.isNotEmpty()) {
            listState.scrollToItem(initialIndex)
            withFrameNanos { }
            initialFocus.requestFocus()
        }
    }
    KaloscopeSidePanel(
        title = stringResource(R.string.episode_selection),
        palette = KaloscopeSidePanelPalette(
            panelColor = Panel,
            textColor = OnBackground,
            mutedColor = Muted,
        ),
        onDismiss = onDismiss,
        side = KaloscopeSidePanelSide.Start,
        modifier = Modifier.testTag("player-episode-drawer"),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = episodes,
                key = PlayerEpisodeEntry::stableId,
            ) { episode ->
                val rowModifier = if (episode.sourceIndex == initialIndex) {
                    Modifier.focusRequester(initialFocus)
                } else {
                    Modifier
                }
                KaloscopeSidePanelSelectionRow(
                    title = episode.title,
                    selected = episode.selected,
                    onClick = { onSelect(episode.sourceIndex) },
                    modifier = rowModifier,
                    maxLines = 2,
                    leadingContent = if (episode.showPoster) {
                        {
                            Box(
                                modifier = Modifier
                                    .width(112.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .testTag("player-episode-poster"),
                            ) {
                                ServerImage(
                                    session = session,
                                    rawValue = episode.posterPath,
                                    contentDescription = null,
                                    policy = ServerImagePolicy.Store,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
