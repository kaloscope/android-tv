package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.Border
import androidx.tv.material3.Glow
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults

@Composable
fun KaloscopeIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    variant: KaloscopeControlVariant = KaloscopeControlVariant.Filled,
    size: KaloscopeControlSize = KaloscopeControlSize.Compact,
    tone: KaloscopeControlTone = KaloscopeControlTone.Default,
    scaleOnFocus: Boolean = true,
    shape: Shape = CircleShape,
    content: @Composable BoxScope.() -> Unit,
) {
    val visuals = rememberKaloscopeControlVisuals(
        variant = variant,
        size = size,
        tone = tone,
        selected = selected,
        enabled = enabled,
        scaleOnFocus = scaleOnFocus,
    )
    val colors = IconButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = visuals.animatedContentColor,
        focusedContainerColor = Color.Transparent,
        focusedContentColor = visuals.animatedContentColor,
        pressedContainerColor = Color.Transparent,
        pressedContentColor = visuals.animatedContentColor,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = visuals.animatedContentColor,
    )
    IconButton(
        onClick = onClick,
        modifier = modifier
            .focusProperties { canFocus = enabled }
            .kaloscopeControlVisuals(visuals, selected, shape),
        enabled = enabled,
        scale = IconButtonDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
            pressedScale = 1f,
            disabledScale = 1f,
            focusedDisabledScale = 1f,
        ),
        glow = IconButtonDefaults.glow(
            glow = Glow.None,
            focusedGlow = Glow.None,
            pressedGlow = Glow.None,
        ),
        shape = IconButtonDefaults.shape(shape),
        colors = colors,
        border = IconButtonDefaults.border(
            border = Border.None,
            focusedBorder = Border.None,
            pressedBorder = Border.None,
            disabledBorder = Border.None,
            focusedDisabledBorder = Border.None,
        ),
        interactionSource = visuals.interactionSource,
        content = content,
    )
}
