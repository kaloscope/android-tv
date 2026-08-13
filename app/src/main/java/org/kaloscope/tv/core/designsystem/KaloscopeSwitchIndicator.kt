package org.kaloscope.tv.core.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor

@Composable
fun KaloscopeSwitchIndicator(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentColor = LocalContentColor.current
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
                color = contentColor.copy(alpha = TrackAlpha),
                shape = RoundedCornerShape(percent = 50),
            )
            .padding(TrackPadding)
            .testTag("setting-switch-indicator"),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(ThumbSize)
                .background(contentColor, CircleShape)
                .testTag("setting-switch-thumb"),
        )
    }
}

private val TrackWidth = 36.dp
private val TrackHeight = 20.dp
private val TrackPadding = 2.dp
private val ThumbSize = 16.dp
private val ThumbTravel = TrackWidth - ThumbSize - TrackPadding * 2
private const val TrackAlpha = 0.38f
