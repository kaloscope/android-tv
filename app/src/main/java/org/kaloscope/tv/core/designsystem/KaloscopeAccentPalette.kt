package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.kaloscope.tv.core.model.AccentColor

data class AccentPalette(
    val primary: Color,
    val soft: Color,
    val panelSelected: Color,
    val controlSelected: Color,
    val backgroundGlow: Color,
)

val LocalAccentPalette = staticCompositionLocalOf {
    AccentColor.Blue.accentPalette()
}

fun AccentColor.accentPalette(): AccentPalette =
    when (this) {
        AccentColor.Blue -> AccentPalette(
            primary = Color(0xFF7F96FF),
            soft = Color(0xFFA9B9FF),
            panelSelected = Color(0xFF202B47),
            controlSelected = Color(0xFF28355F),
            backgroundGlow = Color(0x383D5BD9),
        )

        AccentColor.Purple -> AccentPalette(
            primary = Color(0xFFB58CFF),
            soft = Color(0xFFD3BAFF),
            panelSelected = Color(0xFF2E2645),
            controlSelected = Color(0xFF3A2E59),
            backgroundGlow = Color(0x38975EED),
        )

        AccentColor.Orange -> AccentPalette(
            primary = Color(0xFFFF9468),
            soft = Color(0xFFFFB59A),
            panelSelected = Color(0xFF422A22),
            controlSelected = Color(0xFF553326),
            backgroundGlow = Color(0x33DB5B2B),
        )

        AccentColor.Yellow -> AccentPalette(
            primary = Color(0xFFF4CC58),
            soft = Color(0xFFFFE08B),
            panelSelected = Color(0xFF3F3821),
            controlSelected = Color(0xFF514522),
            backgroundGlow = Color(0x2ECEA224),
        )

        AccentColor.Green -> AccentPalette(
            primary = Color(0xFF4DD990),
            soft = Color(0xFF84E9B5),
            panelSelected = Color(0xFF1B3B30),
            controlSelected = Color(0xFF214B3A),
            backgroundGlow = Color(0x3023B26C),
        )
    }
