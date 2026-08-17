package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun BoxScope.KaloscopeCarouselEdgeFade(
    start: Boolean,
    tagPrefix: String,
) {
    val fadeWidth = if (start) 48.dp else 112.dp
    val fadeBrush = if (start) {
        Brush.horizontalGradient(
            colors = listOf(Background, Color.Transparent),
        )
    } else {
        Brush.horizontalGradient(
            0f to Color.Transparent,
            0.42f to Background.copy(alpha = 0.2f),
            0.72f to Background.copy(alpha = 0.72f),
            1f to Background,
        )
    }
    Box(
        modifier = Modifier
            .align(if (start) Alignment.CenterStart else Alignment.CenterEnd)
            .width(fadeWidth)
            .fillMaxHeight()
            .background(fadeBrush)
            .testTag("$tagPrefix-${if (start) "start" else "end"}-fade"),
    )
}
