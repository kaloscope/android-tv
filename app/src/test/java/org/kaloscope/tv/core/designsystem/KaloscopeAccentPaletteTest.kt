package org.kaloscope.tv.core.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.AccentColor

class KaloscopeAccentPaletteTest {
    @Test
    fun `accent colors keep the approved option order`() {
        assertEquals(
            listOf(
                AccentColor.Blue,
                AccentColor.Purple,
                AccentColor.Orange,
                AccentColor.Yellow,
                AccentColor.Green,
            ),
            AccentColor.entries,
        )
    }

    @Test
    fun `accent colors resolve to the approved palettes`() {
        val expected = mapOf(
            AccentColor.Blue to AccentPalette(
                primary = Color(0xFF7F96FF),
                soft = Color(0xFFA9B9FF),
                panelSelected = Color(0xFF202B47),
                controlSelected = Color(0xFF28355F),
                backgroundGlow = Color(0x383D5BD9),
            ),
            AccentColor.Purple to AccentPalette(
                primary = Color(0xFFB58CFF),
                soft = Color(0xFFD3BAFF),
                panelSelected = Color(0xFF2E2645),
                controlSelected = Color(0xFF3A2E59),
                backgroundGlow = Color(0x38975EED),
            ),
            AccentColor.Orange to AccentPalette(
                primary = Color(0xFFFF9468),
                soft = Color(0xFFFFB59A),
                panelSelected = Color(0xFF422A22),
                controlSelected = Color(0xFF553326),
                backgroundGlow = Color(0x33DB5B2B),
            ),
            AccentColor.Yellow to AccentPalette(
                primary = Color(0xFFF4CC58),
                soft = Color(0xFFFFE08B),
                panelSelected = Color(0xFF3F3821),
                controlSelected = Color(0xFF514522),
                backgroundGlow = Color(0x2ECEA224),
            ),
            AccentColor.Green to AccentPalette(
                primary = Color(0xFF4DD990),
                soft = Color(0xFF84E9B5),
                panelSelected = Color(0xFF1B3B30),
                controlSelected = Color(0xFF214B3A),
                backgroundGlow = Color(0x3023B26C),
            ),
        )

        assertEquals(expected, AccentColor.entries.associateWith(AccentColor::accentPalette))
    }

    @Test
    fun `accent palettes keep readable contrast on dark surfaces`() {
        for (accent in AccentColor.entries) {
            val palette = accent.accentPalette()

            assertTrue(
                "$accent primary contrast",
                contrastRatio(palette.primary, Background) >= MinimumContrast,
            )
            assertTrue(
                "$accent soft contrast",
                contrastRatio(palette.soft, Background) >= MinimumContrast,
            )
            assertTrue(
                "$accent panel selection contrast",
                contrastRatio(OnBackground, palette.panelSelected) >= MinimumContrast,
            )
            assertTrue(
                "$accent control selection contrast",
                contrastRatio(OnBackground, palette.controlSelected) >= MinimumContrast,
            )
        }
    }

    @Test
    fun `accent palettes provide distinct active colors`() {
        val palettes = AccentColor.entries.map(AccentColor::accentPalette)

        assertEquals(palettes.size, palettes.map(AccentPalette::primary).toSet().size)
        assertEquals(palettes.size, palettes.map(AccentPalette::panelSelected).toSet().size)
        assertEquals(palettes.size, palettes.map(AccentPalette::controlSelected).toSet().size)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = first.relativeLuminance()
        val secondLuminance = second.relativeLuminance()
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun Color.relativeLuminance(): Double =
        0.2126 * red.toLinear() + 0.7152 * green.toLinear() + 0.0722 * blue.toLinear()

    private fun Float.toLinear(): Double =
        if (this <= 0.04045f) {
            this / 12.92
        } else {
            ((this + 0.055) / 1.055).pow(2.4)
        }

    private companion object {
        const val MinimumContrast = 4.5
    }
}
