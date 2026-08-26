package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.kaloscope.tv.R
import org.kaloscope.tv.core.player.PlaybackPreparationStage

@Composable
fun KaloscopePlaybackLoadingLayout(
    stage: PlaybackPreparationStage,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val message = stringResource(
        when (stage) {
            PlaybackPreparationStage.Resource -> R.string.playback_preparation_resource
            PlaybackPreparationStage.Danmaku -> R.string.playback_preparation_danmaku
            PlaybackPreparationStage.Playback -> R.string.playback_preparation_playback
        },
    )
    KaloscopeLoadingLayout(
        testTag = testTag,
        modifier = modifier,
        message = message,
        blockInteraction = true,
    )
}
