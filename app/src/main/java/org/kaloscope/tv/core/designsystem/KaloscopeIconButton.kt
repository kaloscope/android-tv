package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults

@Composable
fun KaloscopeIconButton(
    onClick: () -> Unit,
    colors: ButtonColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    shape: Shape = CircleShape,
    scale: ButtonScale = IconButtonDefaults.scale(),
    content: @Composable BoxScope.() -> Unit,
) {
    val outline = Border(
        border = BorderStroke(1.dp, Outline),
        shape = shape,
    )
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics {
            this.selected = selected
        },
        enabled = enabled,
        scale = scale,
        shape = IconButtonDefaults.shape(shape),
        colors = colors,
        border = IconButtonDefaults.border(
            border = outline,
            focusedBorder = outline,
            pressedBorder = outline,
            disabledBorder = outline,
            focusedDisabledBorder = outline,
        ),
        content = content,
    )
}
