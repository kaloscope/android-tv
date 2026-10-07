package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

@Composable
fun KaloscopeBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val accentPalette = LocalAccentPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .drawWithCache {
                val glow = Brush.radialGradient(
                    colors = listOf(
                        accentPalette.backgroundGlow.copy(
                            alpha = accentPalette.backgroundGlow.alpha * 0.35f,
                        ),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                    radius = size.maxDimension.coerceAtLeast(1f) * 0.9f,
                )
                onDrawBehind { drawRect(glow) }
            }
            .testTag("kaloscope-background"),
        content = content,
    )
}
