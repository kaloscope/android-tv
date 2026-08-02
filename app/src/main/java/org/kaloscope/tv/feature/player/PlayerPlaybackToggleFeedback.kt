package org.kaloscope.tv.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kaloscope.tv.R

internal data class PlayerPlaybackToggleEvent(
    val id: Long,
    val playWhenReady: Boolean,
)

@Composable
internal fun PlayerPlaybackToggleFeedback(
    event: PlayerPlaybackToggleEvent?,
    onFinished: (Long) -> Unit,
) {
    if (event == null) {
        return
    }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(1f) }
    val latestOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(event.id) {
        alpha.snapTo(1f)
        scale.snapTo(1f)
        delay(100)
        coroutineScope {
            launch {
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 1_100,
                        easing = LinearOutSlowInEasing,
                    ),
                )
            }
            launch {
                scale.animateTo(
                    targetValue = 1.3f,
                    animationSpec = tween(
                        durationMillis = 1_100,
                        easing = LinearOutSlowInEasing,
                    ),
                )
            }
        }
        latestOnFinished(event.id)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .background(Color(0xFF4D4D4D).copy(alpha = 0.30f), CircleShape)
                .testTag("player-playback-toggle-feedback"),
            contentAlignment = Alignment.Center,
        ) {
            val iconTag = if (event.playWhenReady) {
                "player-playback-toggle-play"
            } else {
                "player-playback-toggle-pause"
            }
            Image(
                painter = painterResource(
                    if (event.playWhenReady) {
                        R.drawable.ic_action_play
                    } else {
                        R.drawable.ic_action_pause
                    },
                ),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .size(32.dp)
                    .testTag(iconTag),
            )
        }
    }
}
