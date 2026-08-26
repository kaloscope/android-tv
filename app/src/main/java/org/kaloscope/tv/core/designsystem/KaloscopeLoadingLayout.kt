package org.kaloscope.tv.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun KaloscopeLoadingLayout(
    testTag: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    blockInteraction: Boolean = false,
) {
    val accentPalette = LocalAccentPalette.current
    val focusRequester = remember { FocusRequester() }
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
    LaunchedEffect(blockInteraction) {
        if (blockInteraction) {
            withFrameNanos { }
            focusRequester.requestFocus()
        }
    }
    val interactionModifier = if (blockInteraction) {
        Modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event -> event.key != Key.Back }
            .focusable()
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(interactionModifier)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        color = accentPalette.primary.copy(alpha = 0.18f),
                        style = stroke,
                    )
                    drawArc(
                        color = accentPalette.primary,
                        startAngle = -90f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = stroke,
                    )
                }
            }
            message?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = it,
                    color = OnBackground,
                    fontSize = 17.sp,
                )
            }
        }
    }
}
