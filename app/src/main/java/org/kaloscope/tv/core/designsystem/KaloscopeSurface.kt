package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
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
    selectedContainerColor: Color? = null,
    focusedContainerColor: Color = PanelElevated,
    focusScale: Float = 1.03f,
    content: @Composable BoxScope.() -> Unit,
) {
    val restingColor = if (selected) {
        selectedContainerColor ?: LocalAccentPalette.current.panelSelected
    } else {
        containerColor
    }
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
        border = ClickableSurfaceDefaults.border(
            focusedDisabledBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape,
            ),
        ),
        content = content,
    )
}
