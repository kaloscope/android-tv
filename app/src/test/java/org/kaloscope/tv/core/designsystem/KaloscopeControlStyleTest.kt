package org.kaloscope.tv.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KaloscopeControlStyleTest {
    @Test
    fun restingVariantsUseTheirBaseMaterialWithoutFocusDepth() {
        val ghost = resolveKaloscopeControlState(
            variant = KaloscopeControlVariant.Ghost,
            size = KaloscopeControlSize.Compact,
            tone = KaloscopeControlTone.Default,
            selected = false,
            enabled = true,
            focused = false,
            pressed = false,
        )
        val filled = resolveKaloscopeControlState(
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Row,
            tone = KaloscopeControlTone.Default,
            selected = false,
            enabled = true,
            focused = false,
            pressed = false,
        )

        assertEquals(KaloscopeControlBaseMaterial.Ghost, ghost.baseMaterial)
        assertEquals(KaloscopeControlBaseMaterial.Filled, filled.baseMaterial)
        assertEquals(KaloscopeControlFocusMaterial.None, ghost.focusMaterial)
        assertEquals(KaloscopeControlFocusMaterial.None, filled.focusMaterial)
        assertEquals(0.dp, ghost.elevation)
        assertEquals(0.dp, filled.elevation)
        assertEquals(1f, ghost.scale)
    }

    @Test
    fun focusedSelectionUsesDarkContentAndKeepsDepth() {
        val state = resolveKaloscopeControlState(
            variant = KaloscopeControlVariant.Ghost,
            size = KaloscopeControlSize.Compact,
            tone = KaloscopeControlTone.Default,
            selected = true,
            enabled = true,
            focused = true,
            pressed = false,
        )

        assertEquals(KaloscopeControlBaseMaterial.Selected, state.baseMaterial)
        assertEquals(
            KaloscopeControlFocusMaterial.Focused,
            state.focusMaterial,
        )
        assertFalse(state.showPressedShade)
        assertEquals(8.dp, state.elevation)
        assertEquals(1.04f, state.scale)
        assertEquals(Color(0xFF101725), state.contentColor)
    }

    @Test
    fun rowFocusUsesDarkContentAndTheSmallerDepthTier() {
        val state = resolveKaloscopeControlState(
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Row,
            tone = KaloscopeControlTone.Default,
            selected = false,
            enabled = true,
            focused = true,
            pressed = false,
        )

        assertEquals(KaloscopeControlBaseMaterial.Filled, state.baseMaterial)
        assertEquals(
            KaloscopeControlFocusMaterial.Focused,
            state.focusMaterial,
        )
        assertEquals(6.dp, state.elevation)
        assertEquals(1.02f, state.scale)
        assertEquals(Color(0xFF101725), state.contentColor)
    }

    @Test
    fun focusedPressSettlesScaleAndDepthButKeepsTheFocusFamily() {
        val state = resolveKaloscopeControlState(
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
            tone = KaloscopeControlTone.Default,
            selected = false,
            enabled = true,
            focused = true,
            pressed = true,
        )

        assertEquals(
            KaloscopeControlFocusMaterial.Focused,
            state.focusMaterial,
        )
        assertTrue(state.showPressedShade)
        assertEquals(2.dp, state.elevation)
        assertEquals(1f, state.scale)
        assertEquals(Color(0xFF101725), state.contentColor)
    }

    @Test
    fun focusedDangerUsesDarkRedSurfaceAndLightContent() {
        val state = resolveKaloscopeControlState(
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Row,
            tone = KaloscopeControlTone.Danger,
            selected = false,
            enabled = true,
            focused = true,
            pressed = false,
        )

        assertEquals(
            KaloscopeControlFocusMaterial.DangerFocused,
            state.focusMaterial,
        )
        assertEquals(OnBackground, state.contentColor)
    }

    @Test
    fun focusScaleCanBeDisabledWithoutLosingFocusDepth() {
        val state = resolveKaloscopeControlState(
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
            tone = KaloscopeControlTone.Danger,
            selected = false,
            enabled = true,
            focused = true,
            pressed = false,
            scaleOnFocus = false,
        )

        assertEquals(
            KaloscopeControlFocusMaterial.DangerFocused,
            state.focusMaterial,
        )
        assertEquals(8.dp, state.elevation)
        assertEquals(1f, state.scale)
    }

    @Test
    fun disabledIgnoresFocusPressAndDepth() {
        val state = resolveKaloscopeControlState(
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
            tone = KaloscopeControlTone.Default,
            selected = false,
            enabled = false,
            focused = true,
            pressed = true,
        )

        assertEquals(KaloscopeControlBaseMaterial.Filled, state.baseMaterial)
        assertEquals(KaloscopeControlFocusMaterial.None, state.focusMaterial)
        assertFalse(state.showPressedShade)
        assertEquals(0.dp, state.elevation)
        assertEquals(1f, state.scale)
        assertEquals(0.42f, state.alpha)
    }

    @Test
    fun disabledSelectionPreservesBaseAndDangerContent() {
        val state = resolveKaloscopeControlState(
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Row,
            tone = KaloscopeControlTone.Danger,
            selected = true,
            enabled = false,
            focused = true,
            pressed = false,
        )

        assertEquals(KaloscopeControlBaseMaterial.Selected, state.baseMaterial)
        assertEquals(KaloscopeControlFocusMaterial.None, state.focusMaterial)
        assertEquals(Danger, state.contentColor)
        assertEquals(0.42f, state.alpha)
        assertEquals(0.dp, state.elevation)
    }
}
