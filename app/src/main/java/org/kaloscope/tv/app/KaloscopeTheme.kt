package org.kaloscope.tv.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.accentPalette
import org.kaloscope.tv.core.model.AccentColor

@Composable
internal fun KaloscopeTheme(
    accentColor: AccentColor = AccentColor.Blue,
    content: @Composable () -> Unit,
) {
    val accentPalette = accentColor.accentPalette()
    CompositionLocalProvider(LocalAccentPalette provides accentPalette) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = accentPalette.primary,
                background = Background,
                surface = Panel,
                surfaceVariant = PanelElevated,
                onPrimary = Background,
                onBackground = OnBackground,
                onSurface = OnBackground,
            ),
            content = content,
        )
    }
}
