package org.kaloscope.tv.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.KaloscopeMotion
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSide
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Outline
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
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
                PlayerEpisodeRow(
                    session = session,
                    episode = episode,
                    selected = episode.selected,
                    onClick = { onSelect(episode.sourceIndex) },
                    modifier = rowModifier,
                )
            }
        }
    }
}

@Composable
private fun PlayerEpisodeRow(
    session: Session,
    episode: PlayerEpisodeEntry,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val accentPalette = LocalAccentPalette.current
    var isFocused by remember(episode.stableId) { mutableStateOf(false) }
    val titleColor by animateColorAsState(
        targetValue = if (isFocused) accentPalette.primary else OnBackground,
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "player-episode-title-color",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            selected -> accentPalette.primary.copy(alpha = 0.28f)
            else -> Outline
        },
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "player-episode-border-color",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "player-episode-border-width",
    )

    KaloscopeFocusSurface(
        onClick = onClick,
        selected = selected,
        shape = shape,
        containerColor = Panel.copy(alpha = 0.74f),
        selectedContainerColor = accentPalette.panelSelected.copy(alpha = 0.42f),
        focusedContainerColor = PanelElevated,
        focusScale = 1f,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .border(borderWidth, borderColor, shape)
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
            }
            .testTag("player-episode-row-${episode.stableId}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (episode.showPoster) {
                Box(
                    modifier = Modifier
                        .width(112.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
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
                Spacer(Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = episode.title,
                    color = titleColor,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                episode.supportingText?.let { supportingText ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = supportingText,
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
