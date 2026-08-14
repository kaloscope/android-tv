package org.kaloscope.tv.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme

@Composable
fun KaloscopeSwitchIndicator(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentColor = LocalContentColor.current
    val accentColor = LocalAccentPalette.current.primary
    val uncheckedColor = contentColor.copy(alpha = UncheckedAlpha)
    val trackColor by animateColorAsState(
        targetValue = if (checked) accentColor else Color.Transparent,
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "switch-track-color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor else uncheckedColor,
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "switch-border-color",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onBackground
        } else {
            uncheckedColor
        },
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "switch-thumb-color",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) ThumbTravel else 0.dp,
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "switch-thumb-offset",
    )
    Box(
        modifier = modifier
            .size(width = TrackWidth, height = TrackHeight)
            .background(
                color = trackColor,
                shape = TrackShape,
            )
            .border(width = TrackBorderWidth, color = borderColor, shape = TrackShape)
            .testTag("setting-switch-indicator")
            .padding(ThumbInset),
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbOffset.roundToPx(), y = 0) }
                .size(ThumbSize)
                .background(thumbColor, CircleShape)
                .testTag("setting-switch-thumb"),
        )
    }
}

private val TrackWidth = 36.dp
private val TrackHeight = 20.dp
private val TrackBorderWidth = 1.dp
private val ThumbInset = 2.dp
private val ThumbSize = 16.dp
private val ThumbTravel = TrackWidth - ThumbSize - ThumbInset * 2
private val TrackShape = RoundedCornerShape(percent = 50)
private const val UncheckedAlpha = 0.5f
