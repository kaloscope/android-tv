package org.kaloscope.tv.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.MediaActor
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
internal fun CastStrip(
    session: Session,
    actors: List<MediaActor>,
    focusRequester: FocusRequester,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (actors.isEmpty()) return
    val accentPalette = LocalAccentPalette.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedActorIndex by remember(actors) { mutableIntStateOf(0) }
    var carouselFocused by remember { mutableStateOf(false) }
    val canScrollBackward by remember {
        derivedStateOf { listState.canScrollBackward }
    }
    val canScrollForward by remember {
        derivedStateOf { listState.canScrollForward }
    }
    val selectedActor = actors[selectedActorIndex.coerceIn(actors.indices)]

    Column(modifier.testTag("cast-strip")) {
        Text(
            text = stringResource(R.string.cast_title),
            color = OnBackground,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp),
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { carouselFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        val handled = when (event.key) {
                            Key.DirectionLeft,
                            Key.DirectionRight,
                            Key.DirectionUp,
                            Key.DirectionDown,
                            -> true

                            else -> false
                        }
                        if (!handled) return@onPreviewKeyEvent false
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    selectedActorIndex = (selectedActorIndex - 1).coerceAtLeast(0)
                                    scope.launch {
                                        listState.animateScrollToItem(selectedActorIndex)
                                    }
                                }

                                Key.DirectionRight -> {
                                    selectedActorIndex = (selectedActorIndex + 1)
                                        .coerceAtMost(actors.lastIndex)
                                    scope.launch {
                                        listState.animateScrollToItem(selectedActorIndex)
                                    }
                                }

                                Key.DirectionUp -> onNavigateUp()
                                Key.DirectionDown -> Unit
                            }
                        }
                        true
                    }
                    .semantics {
                        contentDescription = listOfNotNull(
                            selectedActor.name,
                            selectedActor.role?.takeIf(String::isNotBlank),
                        ).joinToString("，")
                    }
                    .focusable()
                    .testTag("cast-carousel"),
                contentPadding = PaddingValues(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(
                    items = actors,
                    key = { index, actor -> "$index:${actor.name}:${actor.role.orEmpty()}" },
                ) { index, actor ->
                    val selected = carouselFocused && index == selectedActorIndex
                    Column(
                        modifier = Modifier
                            .width(88.dp)
                            .background(
                                color = if (selected) {
                                    accentPalette.panelSelected
                                } else {
                                    Panel.copy(alpha = 0.36f)
                                },
                                shape = RoundedCornerShape(12.dp),
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) {
                                    accentPalette.primary
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(vertical = 6.dp)
                            .semantics { this.selected = selected }
                            .testTag("cast-item-$index"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ServerImage(
                            session = session,
                            rawValue = actor.thumbPath,
                            contentDescription = null,
                            policy = ServerImagePolicy.Store,
                            modifier = Modifier.size(64.dp).clip(CircleShape),
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = actor.name,
                            color = OnBackground,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        actor.role?.takeIf(String::isNotBlank)?.let { role ->
                            Text(
                                text = role,
                                color = Muted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            if (canScrollBackward) {
                CastEdgeFade(start = true)
            }
            if (canScrollForward) {
                CastEdgeFade(start = false)
            }
        }
    }
}

@Composable
private fun BoxScope.CastEdgeFade(start: Boolean) {
    Box(
        modifier = Modifier
            .align(if (start) Alignment.CenterStart else Alignment.CenterEnd)
            .width(if (start) 36.dp else 72.dp)
            .height(124.dp)
            .background(
                Brush.horizontalGradient(
                    colors = if (start) {
                        listOf(Background, Color.Transparent)
                    } else {
                        listOf(Color.Transparent, Background)
                    },
                ),
            )
            .testTag(if (start) "cast-start-fade" else "cast-end-fade"),
    )
}
