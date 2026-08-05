package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.util.Locale
import org.kaloscope.tv.R
import org.kaloscope.tv.core.model.SubtitleDisplayMode

@Composable
internal fun subtitleDisplayModeLabel(mode: SubtitleDisplayMode): String =
    when (mode) {
        SubtitleDisplayMode.Stroke -> stringResource(R.string.subtitle_display_mode_stroke)
        SubtitleDisplayMode.Background ->
            stringResource(R.string.subtitle_display_mode_background)
    }

internal fun formatSubtitleOffset(value: Float): String =
    if (value == 0f) {
        "0.0s"
    } else {
        String.format(Locale.US, "%+.1fs", value)
    }
