package org.kaloscope.tv.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun KaloscopeLoadingLayout(
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loading-rotation",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(48.dp)
                .progressSemantics()
                .testTag("$testTag-indicator")
                .rotate(rotation),
        ) {
            val stroke = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            inset(stroke.width / 2f) {
                drawCircle(
                    color = Primary.copy(alpha = 0.18f),
                    style = stroke,
                )
                drawArc(
                    color = Primary,
                    startAngle = -90f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = stroke,
                )
            }
        }
    }
}
