package org.kaloscope.tv.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class KaloscopeControlVariant {
    Ghost,
    Filled,
}

enum class KaloscopeControlSize {
    Compact,
    Row,
}

enum class KaloscopeControlTone {
    Default,
    Danger,
}

object KaloscopeControlTokens {
    val SelectedGradientTop = ControlSelectedTop
    val SelectedGradientBottom = ControlSelectedBottom
    val NeutralGlassTop = Color(0x38FFFFFF)
    val NeutralGlassBottom = Color(0x18FFFFFF)
    val SelectedFocusLift = Color(0x14FFFFFF)
    val PressedShade = Color(0x14000000)
    val FocusShadow = Color(0x52000000)
    val CompactFocusElevation = 8.dp
    val RowFocusElevation = 6.dp
    val PressedElevation = 2.dp
    const val DisabledAlpha = 0.42f
    const val CompactFocusedScale = 1.04f
    const val RowFocusedScale = 1.02f
    const val RestingScale = 1f
}

internal enum class KaloscopeControlBaseMaterial {
    Ghost,
    Filled,
    Selected,
}

internal enum class KaloscopeControlFocusMaterial {
    None,
    NeutralGlass,
    SelectedLift,
}

internal data class KaloscopeResolvedControlState(
    val baseMaterial: KaloscopeControlBaseMaterial,
    val focusMaterial: KaloscopeControlFocusMaterial,
    val showPressedShade: Boolean,
    val elevation: Dp,
    val scale: Float,
    val contentColor: Color,
    val alpha: Float,
)

internal fun resolveKaloscopeControlState(
    variant: KaloscopeControlVariant,
    size: KaloscopeControlSize,
    tone: KaloscopeControlTone,
    selected: Boolean,
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
): KaloscopeResolvedControlState {
    val effectivelyFocused = enabled && focused
    val baseMaterial = when {
        selected -> KaloscopeControlBaseMaterial.Selected
        variant == KaloscopeControlVariant.Ghost -> KaloscopeControlBaseMaterial.Ghost
        else -> KaloscopeControlBaseMaterial.Filled
    }
    val focusMaterial = when {
        !effectivelyFocused -> KaloscopeControlFocusMaterial.None
        selected -> KaloscopeControlFocusMaterial.SelectedLift
        else -> KaloscopeControlFocusMaterial.NeutralGlass
    }
    val scale = when {
        !effectivelyFocused || pressed -> KaloscopeControlTokens.RestingScale
        size == KaloscopeControlSize.Compact ->
            KaloscopeControlTokens.CompactFocusedScale

        else -> KaloscopeControlTokens.RowFocusedScale
    }
    val elevation = when {
        !effectivelyFocused -> 0.dp
        pressed -> KaloscopeControlTokens.PressedElevation
        size == KaloscopeControlSize.Compact ->
            KaloscopeControlTokens.CompactFocusElevation

        else -> KaloscopeControlTokens.RowFocusElevation
    }
    return KaloscopeResolvedControlState(
        baseMaterial = baseMaterial,
        focusMaterial = focusMaterial,
        showPressedShade = effectivelyFocused && pressed,
        elevation = elevation,
        scale = scale,
        contentColor = if (tone == KaloscopeControlTone.Danger) Danger else OnBackground,
        alpha = if (enabled) 1f else KaloscopeControlTokens.DisabledAlpha,
    )
}
