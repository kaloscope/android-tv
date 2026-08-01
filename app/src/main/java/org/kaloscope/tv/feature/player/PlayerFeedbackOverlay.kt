package org.kaloscope.tv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.player.PlaybackFailure
import org.kaloscope.tv.core.player.PlaybackFeedback
import org.kaloscope.tv.core.player.PlaybackSourceKind

@Composable
internal fun PlayerFeedbackOverlay(
    feedback: PlaybackFeedback,
    failure: PlaybackFailure?,
    sourceKind: PlaybackSourceKind,
    onRetry: () -> Unit,
) {
    when (feedback) {
        PlaybackFeedback.Preparing ->
            KaloscopeLoadingLayout("player-loading")

        PlaybackFeedback.SwitchingItem ->
            BlockingFeedbackMessage(stringResource(R.string.switching_episode))

        PlaybackFeedback.FallingBack -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = stringResource(R.string.player_fallback_banner),
                    color = OnBackground,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(top = 34.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }

        PlaybackFeedback.Rebuffering ->
            PlayerBufferingIndicator(isRebuffering = true)

        PlaybackFeedback.Failed ->
            FeedbackError(
                failure = failure ?: PlaybackFailure.Unknown,
                sourceKind = sourceKind,
                onRetry = onRetry,
            )

        PlaybackFeedback.Ready -> Unit
    }
}

@Composable
private fun BlockingFeedbackMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = OnBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FeedbackError(
    failure: PlaybackFailure,
    sourceKind: PlaybackSourceKind,
    onRetry: () -> Unit,
) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        retryFocus.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = feedbackErrorText(failure, sourceKind),
                color = Danger,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            KaloscopeButton(
                onClick = onRetry,
                modifier = Modifier.focusRequester(retryFocus),
                size = KaloscopeControlSize.Compact,
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun feedbackErrorText(
    failure: PlaybackFailure,
    sourceKind: PlaybackSourceKind,
): String =
    when (failure) {
        PlaybackFailure.Network -> stringResource(R.string.playback_network_failed)
        PlaybackFailure.Unauthorized -> stringResource(R.string.playback_unauthorized)
        PlaybackFailure.Forbidden -> stringResource(R.string.error_forbidden)
        PlaybackFailure.MissingMedia -> stringResource(R.string.playback_media_missing)
        PlaybackFailure.Source,
        PlaybackFailure.Decoder,
        PlaybackFailure.Unknown,
        -> if (sourceKind == PlaybackSourceKind.Network) {
            stringResource(R.string.network_source_playback_failed)
        } else if (sourceKind == PlaybackSourceKind.HlsTranscode) {
            stringResource(R.string.transcode_playback_failed)
        } else {
            stringResource(R.string.direct_playback_failed)
        }
    }
