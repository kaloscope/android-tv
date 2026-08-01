package org.kaloscope.tv.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.kaloscope.tv.core.designsystem.KaloscopeMotion

@Composable
internal fun AnimatedPlayerControlLayer(
    layer: PlayerControlLayer,
    modifier: Modifier = Modifier,
    content: @Composable (PlayerControlLayer) -> Unit,
) {
    AnimatedContent(
        targetState = layer,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(tween(KaloscopeMotion.ContentMillis)) togetherWith
                fadeOut(tween(KaloscopeMotion.ContentMillis))
        },
        contentAlignment = Alignment.TopStart,
        label = "player-control-layer",
    ) { renderedLayer ->
        Box(Modifier.fillMaxSize()) {
            content(renderedLayer)
        }
    }
}
