package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface

@Composable
fun KaloscopeFocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape,
    containerColor: Color = Color.Transparent,
    focusedContainerColor: Color = PanelElevated,
    focusScale: Float = 1.03f,
    content: @Composable BoxScope.() -> Unit,
) {
    val restingColor = if (selected) PanelSelected else containerColor
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = restingColor,
            focusedContainerColor = focusedContainerColor,
            contentColor = OnBackground,
            focusedContentColor = OnBackground,
            disabledContainerColor = restingColor.copy(alpha = 0.45f),
            disabledContentColor = Muted,
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = focusScale,
            disabledScale = 1f,
            focusedDisabledScale = 1f,
        ),
        content = content,
    )
}
