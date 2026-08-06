package org.kaloscope.tv.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.designsystem.AccentPalette
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.ControlFocused
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.accentPalette
import org.kaloscope.tv.core.model.AccentColor

class KaloscopeThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultThemeProvidesBlueAccent() {
        val snapshot = captureTheme()

        assertEquals(AccentColor.Blue.accentPalette(), snapshot.accent)
        assertEquals(AccentColor.Blue.accentPalette().primary, snapshot.materialPrimary)
    }

    @Test
    fun changingAccentKeepsNeutralAndFunctionalColorsFixed() {
        val purple = captureTheme(AccentColor.Purple)

        assertEquals(AccentColor.Purple.accentPalette(), purple.accent)
        assertEquals(AccentColor.Purple.accentPalette().primary, purple.materialPrimary)
        assertEquals(Background, purple.background)
        assertEquals(Panel, purple.surface)
        assertEquals(OnBackground, purple.onBackground)
        assertEquals(ControlFocused, purple.focusedSurface)
        assertEquals(Danger, purple.danger)
    }

    private fun captureTheme(
        accentColor: AccentColor? = null,
    ): ThemeSnapshot {
        var snapshot: ThemeSnapshot? = null
        composeRule.setContent {
            val content: @Composable () -> Unit = {
                snapshot = ThemeSnapshot(
                    accent = LocalAccentPalette.current,
                    materialPrimary = MaterialTheme.colorScheme.primary,
                    background = MaterialTheme.colorScheme.background,
                    surface = MaterialTheme.colorScheme.surface,
                    onBackground = MaterialTheme.colorScheme.onBackground,
                    focusedSurface = ControlFocused,
                    danger = Danger,
                )
            }
            if (accentColor == null) {
                KaloscopeTheme(content = content)
            } else {
                KaloscopeTheme(accentColor = accentColor, content = content)
            }
        }
        composeRule.waitForIdle()
        return requireNotNull(snapshot)
    }
}

private data class ThemeSnapshot(
    val accent: AccentPalette,
    val materialPrimary: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val focusedSurface: Color,
    val danger: Color,
)
