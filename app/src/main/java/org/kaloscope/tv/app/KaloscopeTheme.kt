package org.kaloscope.tv.app

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.Primary

@Composable
internal fun KaloscopeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Primary,
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
