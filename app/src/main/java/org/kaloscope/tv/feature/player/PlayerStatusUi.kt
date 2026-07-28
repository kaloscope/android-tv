package org.kaloscope.tv.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.PlaybackSourceKind
import org.kaloscope.tv.core.player.TranscodeResolution

@Composable
internal fun playbackModeLabel(
    mode: PlaybackMode?,
    sourceKind: PlaybackSourceKind,
    resolution: TranscodeResolution?,
): String {
    val resolutionLabel = when (resolution) {
        TranscodeResolution.Original -> stringResource(R.string.resolution_original)
        TranscodeResolution.P1080 -> "1080P"
        TranscodeResolution.P720 -> "720P"
        TranscodeResolution.P480 -> "480P"
        null -> ""
    }
    return when {
        sourceKind == PlaybackSourceKind.Network ->
            stringResource(R.string.playback_network)

        mode == PlaybackMode.Auto && sourceKind == PlaybackSourceKind.Direct ->
            stringResource(R.string.playback_auto_direct)

        mode == PlaybackMode.Auto ->
            stringResource(R.string.playback_auto_transcode, resolutionLabel)

        mode == PlaybackMode.Direct -> stringResource(R.string.playback_direct)
        else -> stringResource(R.string.playback_transcode, resolutionLabel)
    }
}

@Composable
internal fun PlayerMessage(
    title: String,
    description: String,
    onBack: (() -> Unit)? = null,
) {
    KaloscopeBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = OnBackground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    color = Muted,
                    fontSize = 16.sp,
                )
                onBack?.let {
                    Spacer(Modifier.height(18.dp))
                    KaloscopeButton(
                        onClick = it,
                        size = KaloscopeControlSize.Compact,
                    ) {
                        Text(stringResource(R.string.back))
                    }
                }
            }
        }
    }
}
