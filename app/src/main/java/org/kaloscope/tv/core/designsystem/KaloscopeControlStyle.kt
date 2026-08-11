package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class KaloscopeControlVariant {
    Ghost,
    Filled,
    Sidebar,
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
    val SidebarSelectedSurface = SidebarSelected
    val FocusedSurface = ControlFocused
    val PressedShade = Color(0x14000000)
    val FocusShadow = Color(0x52000000)
    val CompactFocusElevation = 8.dp
    val RowFocusElevation = 6.dp
    val PressedElevation = 2.dp
    const val DisabledAlpha = 0.42f
    const val AdjustmentArrowDisabledAlpha = 0.24f
    const val CompactFocusedScale = 1.04f
    const val RowFocusedScale = 1.02f
    const val RestingScale = 1f
}

internal val LocalKaloscopeControlRestingContentColor =
    staticCompositionLocalOf { OnBackground }

internal fun resolveKaloscopeControlBaseColor(
    material: KaloscopeControlBaseMaterial,
    accentPalette: AccentPalette,
): Color = when (material) {
    KaloscopeControlBaseMaterial.Ghost -> Color.Transparent
    KaloscopeControlBaseMaterial.Filled -> PanelElevated
    KaloscopeControlBaseMaterial.Selected -> accentPalette.controlSelected
    KaloscopeControlBaseMaterial.SidebarSelected ->
        KaloscopeControlTokens.SidebarSelectedSurface
}

internal fun resolveKaloscopeControlFocusColor(
    material: KaloscopeControlFocusMaterial,
): Color = when (material) {
    KaloscopeControlFocusMaterial.None -> Color.Transparent
    KaloscopeControlFocusMaterial.Focused,
    KaloscopeControlFocusMaterial.SelectedFocused,
    -> KaloscopeControlTokens.FocusedSurface

    KaloscopeControlFocusMaterial.DangerFocused -> DangerFocusedSurface
}

internal enum class KaloscopeControlBaseMaterial {
    Ghost,
    Filled,
    Selected,
    SidebarSelected,
}

internal enum class KaloscopeControlFocusMaterial {
    None,
    Focused,
    SelectedFocused,
    DangerFocused,
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
    scaleOnFocus: Boolean = true,
    preserveSelectionOnFocus: Boolean = false,
    restingContentColor: Color = OnBackground,
): KaloscopeResolvedControlState {
    val effectivelyFocused = enabled && focused
    val baseMaterial = when {
        selected && variant == KaloscopeControlVariant.Sidebar ->
            KaloscopeControlBaseMaterial.SidebarSelected

        selected -> KaloscopeControlBaseMaterial.Selected
        variant == KaloscopeControlVariant.Ghost ||
            variant == KaloscopeControlVariant.Sidebar ->
            KaloscopeControlBaseMaterial.Ghost

        else -> KaloscopeControlBaseMaterial.Filled
    }
    val focusMaterial = when {
        !effectivelyFocused -> KaloscopeControlFocusMaterial.None
        tone == KaloscopeControlTone.Danger ->
            KaloscopeControlFocusMaterial.DangerFocused

        selected && preserveSelectionOnFocus ->
            KaloscopeControlFocusMaterial.None

        selected && variant != KaloscopeControlVariant.Sidebar ->
            KaloscopeControlFocusMaterial.SelectedFocused

        else -> KaloscopeControlFocusMaterial.Focused
    }
    val scale = when {
        !effectivelyFocused || pressed || !scaleOnFocus ->
            KaloscopeControlTokens.RestingScale

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
            OnDangerFocused

        effectivelyFocused && selected && preserveSelectionOnFocus ->
            OnBackground

        effectivelyFocused -> OnControlFocused
        tone == KaloscopeControlTone.Danger -> Danger
        selected -> OnBackground
        else -> restingContentColor
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
