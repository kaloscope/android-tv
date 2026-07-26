package org.kaloscope.tv.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun KaloscopeSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val offset = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton-offset",
    ).value
    val base = Color(0xFF202B40)
    val highlight = Color(0xFF34425E)
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to base,
            (offset - 0.18f).coerceIn(0f, 1f) to base,
            offset.coerceIn(0f, 1f) to highlight,
            (offset + 0.18f).coerceIn(0f, 1f) to base,
            1f to base,
        ),
    )
    androidx.compose.foundation.layout.Box(modifier = modifier.background(brush))
}
