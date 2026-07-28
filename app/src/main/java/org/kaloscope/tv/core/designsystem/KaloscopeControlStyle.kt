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
    val SelectedSurface = ControlSelected
    val FocusedSurface = ControlFocused
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
    Focused,
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
    val focusMaterial = if (effectivelyFocused) {
        KaloscopeControlFocusMaterial.Focused
    } else {
        KaloscopeControlFocusMaterial.None
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
    val contentColor = when {
        effectivelyFocused && tone == KaloscopeControlTone.Danger ->
            OnControlFocusedDanger

        effectivelyFocused -> OnControlFocused
        tone == KaloscopeControlTone.Danger -> Danger
        else -> OnBackground
    }
    return KaloscopeResolvedControlState(
        baseMaterial = baseMaterial,
        focusMaterial = focusMaterial,
        showPressedShade = effectivelyFocused && pressed,
        elevation = elevation,
        scale = scale,
        contentColor = contentColor,
        alpha = if (enabled) 1f else KaloscopeControlTokens.DisabledAlpha,
    )
}
