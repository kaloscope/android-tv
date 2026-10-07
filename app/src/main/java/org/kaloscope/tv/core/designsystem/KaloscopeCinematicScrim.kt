package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

@Composable
fun KaloscopeCinematicScrim(
    modifier: Modifier = Modifier,
    protectFullWidth: Boolean = false,
) {
    // Detail text spans the screen. Home can reveal more artwork on the right.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    0f to Background.copy(alpha = 0.98f),
                    0.38f to Background.copy(alpha = 0.92f),
                    0.64f to Background.copy(alpha = if (protectFullWidth) 0.86f else 0.62f),
                    1f to Background.copy(alpha = if (protectFullWidth) 0.82f else 0.28f),
                ),
            )
            .background(
                Brush.verticalGradient(
                    0f to Background.copy(alpha = 0.76f),
                    0.22f to Background.copy(alpha = 0.12f),
                    0.48f to Background.copy(alpha = 0f),
                    0.72f to Background.copy(alpha = 0.48f),
                    1f to Background,
                ),
            ),
    )
}
