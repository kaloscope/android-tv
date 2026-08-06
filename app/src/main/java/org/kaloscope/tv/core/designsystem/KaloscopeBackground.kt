package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            .background(
                Brush.linearGradient(
                    colors = listOf(Background, Color(0xFF070B15), Background),
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(accentPalette.backgroundGlow, Color.Transparent),
                    radius = 1_050f,
                ),
            )
            .testTag("kaloscope-background"),
        content = content,
    )
}
