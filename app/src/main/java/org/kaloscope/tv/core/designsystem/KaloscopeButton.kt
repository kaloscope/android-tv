package org.kaloscope.tv.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow

internal data class KaloscopeControlVisuals(
    val interactionSource: MutableInteractionSource,
    val state: KaloscopeResolvedControlState,
    val animatedBaseColor: Color,
    val animatedFocusColor: Color,
    val animatedContentColor: Color,
    val animatedPressedShade: Color,
    val animatedElevation: Dp,
    val animatedScale: Float,
)

@Composable
internal fun rememberKaloscopeControlVisuals(
    variant: KaloscopeControlVariant,
    size: KaloscopeControlSize,
    tone: KaloscopeControlTone,
    selected: Boolean,
    enabled: Boolean,
): KaloscopeControlVisuals {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val state = resolveKaloscopeControlState(
        variant = variant,
        size = size,
        tone = tone,
        selected = selected,
        enabled = enabled,
        focused = focused,
        pressed = pressed,
    )
    val duration = if (pressed) {
        KaloscopeMotion.PressMillis
    } else {
        KaloscopeMotion.FocusMillis
    }
    val animatedBaseColor by animateColorAsState(
        targetValue = state.baseMaterial.color(),
        animationSpec = tween(
            durationMillis = duration,
            easing = LinearOutSlowInEasing,
        ),
        label = "control-base",
    )
    val animatedFocusColor by animateColorAsState(
        targetValue = state.focusMaterial.color(),
        animationSpec = tween(
            durationMillis = duration,
            easing = LinearOutSlowInEasing,
        ),
        label = "control-focus",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = state.contentColor,
        animationSpec = tween(
            durationMillis = duration,
            easing = LinearOutSlowInEasing,
        ),
        label = "control-content",
    )
    val animatedPressedShade by animateColorAsState(
        targetValue = if (state.showPressedShade) {
            KaloscopeControlTokens.PressedShade
        } else {
            Color.Transparent
        },
        animationSpec = tween(
            durationMillis = duration,
            easing = LinearOutSlowInEasing,
        ),
        label = "control-pressed-shade",
    )
    val elevation by animateDpAsState(
        targetValue = state.elevation,
        animationSpec = tween(
            durationMillis = duration,
            easing = LinearOutSlowInEasing,
        ),
        label = "control-elevation",
    )
    val scale by animateFloatAsState(
        targetValue = state.scale,
        animationSpec = tween(
            durationMillis = duration,
            easing = LinearOutSlowInEasing,
        ),
        label = "control-scale",
    )
    return KaloscopeControlVisuals(
        interactionSource = interactionSource,
        state = state,
        animatedBaseColor = animatedBaseColor,
        animatedFocusColor = animatedFocusColor,
        animatedContentColor = animatedContentColor,
        animatedPressedShade = animatedPressedShade,
        animatedElevation = elevation,
        animatedScale = scale,
    )
}

private fun KaloscopeControlBaseMaterial.color(): Color =
    when (this) {
        KaloscopeControlBaseMaterial.Ghost -> Color.Transparent
        KaloscopeControlBaseMaterial.Filled -> PanelElevated
        KaloscopeControlBaseMaterial.Selected -> KaloscopeControlTokens.SelectedSurface
    }

private fun KaloscopeControlFocusMaterial.color(): Color =
    when (this) {
        KaloscopeControlFocusMaterial.None -> Color.Transparent
        KaloscopeControlFocusMaterial.Focused -> KaloscopeControlTokens.FocusedSurface
    }

internal fun Modifier.kaloscopeControlVisuals(
    visuals: KaloscopeControlVisuals,
    selected: Boolean,
    shape: Shape,
): Modifier {
    return this
        .graphicsLayer {
            scaleX = visuals.animatedScale
            scaleY = visuals.animatedScale
            alpha = visuals.state.alpha
            this.shape = shape
            clip = false
            shadowElevation = visuals.animatedElevation.toPx()
            ambientShadowColor = KaloscopeControlTokens.FocusShadow
            spotShadowColor = KaloscopeControlTokens.FocusShadow
        }
        .background(
            color = visuals.animatedBaseColor,
            shape = shape,
        )
        .background(
            color = visuals.animatedFocusColor,
            shape = shape,
        )
        .background(
            color = visuals.animatedPressedShade,
            shape = shape,
        )
        .semantics {
            this.selected = selected
        }
}

@Composable
fun KaloscopeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    variant: KaloscopeControlVariant = KaloscopeControlVariant.Filled,
    size: KaloscopeControlSize = KaloscopeControlSize.Compact,
    tone: KaloscopeControlTone = KaloscopeControlTone.Default,
    shape: Shape = CircleShape,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val visuals = rememberKaloscopeControlVisuals(
        variant = variant,
        size = size,
        tone = tone,
        selected = selected,
        enabled = enabled,
    )
    val colors = ButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = visuals.animatedContentColor,
        focusedContainerColor = Color.Transparent,
        focusedContentColor = visuals.animatedContentColor,
        pressedContainerColor = Color.Transparent,
        pressedContentColor = visuals.animatedContentColor,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = visuals.animatedContentColor,
    )
    Button(
        onClick = onClick,
        modifier = modifier
            .focusProperties { canFocus = enabled }
            .kaloscopeControlVisuals(visuals, selected, shape),
        enabled = enabled,
        scale = ButtonDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
            pressedScale = 1f,
            disabledScale = 1f,
            focusedDisabledScale = 1f,
        ),
        glow = ButtonDefaults.glow(
            glow = Glow.None,
            focusedGlow = Glow.None,
            pressedGlow = Glow.None,
        ),
        shape = ButtonDefaults.shape(shape),
        colors = colors,
        tonalElevation = 0.dp,
        border = ButtonDefaults.border(
            border = Border.None,
            focusedBorder = Border.None,
            pressedBorder = Border.None,
            disabledBorder = Border.None,
            focusedDisabledBorder = Border.None,
        ),
        contentPadding = contentPadding,
        interactionSource = visuals.interactionSource,
        content = content,
    )
}
