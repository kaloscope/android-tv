package org.kaloscope.tv.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon

@Composable
fun KaloscopeNavigationIcon(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun KaloscopeSelectableNavigationIcon(
    @DrawableRes iconRes: Int,
    @DrawableRes selectedIconRes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = selected,
        modifier = modifier,
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "navigation-icon-selection",
    ) { targetSelected ->
        KaloscopeNavigationIcon(
            iconRes = if (targetSelected) selectedIconRes else iconRes,
        )
    }
}
