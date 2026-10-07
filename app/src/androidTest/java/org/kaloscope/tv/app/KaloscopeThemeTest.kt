package org.kaloscope.tv.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        var accentColor by mutableStateOf(AccentColor.Blue)
        var snapshot: ThemeSnapshot? = null
        composeRule.setContent {
            KaloscopeTheme(accentColor = accentColor) {
                snapshot = currentThemeSnapshot()
            }
        }

        for (accent in AccentColor.entries) {
            composeRule.runOnIdle { accentColor = accent }
            composeRule.waitForIdle()
            val current = requireNotNull(snapshot)
            val palette = accent.accentPalette()
            assertEquals(palette, current.accent)
            assertEquals(palette.primary, current.materialPrimary)
            assertEquals(palette.controlSelected, current.primaryContainer)
            assertEquals(palette.soft, current.secondary)
            assertEquals(palette.panelSelected, current.secondaryContainer)
            assertEquals(Background, current.background)
            assertEquals(Panel, current.surface)
            assertEquals(OnBackground, current.onBackground)
            assertEquals(ControlFocused, current.focusedSurface)
            assertEquals(Danger, current.danger)
        }
    }

    private fun captureTheme(
        accentColor: AccentColor? = null,
    ): ThemeSnapshot {
        var snapshot: ThemeSnapshot? = null
        composeRule.setContent {
            val content: @Composable () -> Unit = {
                snapshot = currentThemeSnapshot()
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

@Composable
private fun currentThemeSnapshot() = ThemeSnapshot(
    accent = LocalAccentPalette.current,
    materialPrimary = MaterialTheme.colorScheme.primary,
    primaryContainer = MaterialTheme.colorScheme.primaryContainer,
    secondary = MaterialTheme.colorScheme.secondary,
    secondaryContainer = MaterialTheme.colorScheme.secondaryContainer,
    background = MaterialTheme.colorScheme.background,
    surface = MaterialTheme.colorScheme.surface,
    onBackground = MaterialTheme.colorScheme.onBackground,
    focusedSurface = ControlFocused,
    danger = MaterialTheme.colorScheme.error,
)

private data class ThemeSnapshot(
    val accent: AccentPalette,
    val materialPrimary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val focusedSurface: Color,
    val danger: Color,
)
