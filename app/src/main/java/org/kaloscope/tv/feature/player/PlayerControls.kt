package org.kaloscope.tv.feature.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.model.MediaChapter
import org.kaloscope.tv.core.player.ChapterTimelinePolicy
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Primary
import kotlinx.coroutines.delay

internal data class PlayerActionUiState(
    val enabled: Boolean,
    val active: Boolean = false,
    val error: Boolean = false,
)

internal data class PlayerControlsUiState(
    val title: String,
    val isPlaying: Boolean,
    val positionMillis: Long,
    val durationMillis: Long,
    val playbackModeLabel: String,
    val playbackSpeed: Float,
    val fallbackInProgress: Boolean,
    val progressSaveFailed: Boolean,
    val previousEnabled: Boolean,
    val nextEnabled: Boolean,
    val subtitles: PlayerActionUiState,
    val danmakus: PlayerActionUiState,
    val danmakuSettings: PlayerActionUiState,
    val quality: PlayerActionUiState,
    val subtitleLabel: String? = null,
    val chapters: List<MediaChapter> = emptyList(),
)

@Composable
internal fun PlayerInfoPreview(state: PlayerControlsUiState) {
    val progress = if (state.durationMillis > 0) {
        (state.positionMillis.toFloat() / state.durationMillis).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(playerControlScrim())
            .testTag("player-info-preview")
            .padding(horizontal = 50.dp),
    ) {
        Spacer(Modifier.weight(1f))
        PlayerPlaybackSummary(state)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatPlayerDuration(state.positionMillis),
                color = Muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatRemainingDuration(state.positionMillis, state.durationMillis),
                color = Muted,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFF4A5060), RoundedCornerShape(9.dp))
                .testTag("player-preview-progress-track"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Primary, RoundedCornerShape(9.dp)),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PlayerPlaybackSummary(state: PlayerControlsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.fallbackInProgress) {
                Text(
                    text = stringResource(R.string.switching_to_transcode),
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        PlayerStatusChip(state.playbackModeLabel)
        Spacer(Modifier.width(6.dp))
        PlayerStatusChip(formatPlaybackSpeed(state.playbackSpeed))
    }
}

@Composable
private fun PlayerStatusChip(label: String) {
    Text(
        text = label,
        color = OnBackground,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .background(Color(0xD9293040), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private fun playerControlScrim(): Brush =
    Brush.verticalGradient(
        listOf(
            Color.Transparent,
            Color(0x14050810),
            Color(0xF2050810),
        ),
    )

private fun formatRemainingDuration(positionMillis: Long, durationMillis: Long): String =
    "−${formatPlayerDuration((durationMillis - positionMillis).coerceAtLeast(0))}"

@Composable
internal fun PlayerControls(
    state: PlayerControlsUiState,
    playFocus: FocusRequester,
    progressFocus: FocusRequester? = null,
    definitionFocus: FocusRequester,
    danmakuSettingsFocus: FocusRequester,
    subtitleFocus: FocusRequester,
    speedFocus: FocusRequester,
    onPrevious: () -> Unit,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenSpeed: () -> Unit,
    onToggleDanmakus: () -> Unit,
    onOpenDanmakuSettings: () -> Unit,
    onOpenDefinitions: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onHideControls: () -> Unit,
    onInteraction: () -> Unit,
    onRetrySubtitles: () -> Unit = {},
    onRetryDanmakus: () -> Unit = {},
) {
    val defaultProgressFocus = remember { FocusRequester() }
    val resolvedProgressFocus = progressFocus ?: defaultProgressFocus
    val forwardFocus = remember { FocusRequester() }
    val nextFocus = remember { FocusRequester() }
    val episodeGroupEndFocus = if (state.nextEnabled) nextFocus else forwardFocus
    val supplementaryGroupStartFocus = if (state.subtitles.enabled) subtitleFocus else speedFocus
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(playerControlScrim())
            .testTag("player-control-layer")
            .padding(horizontal = 50.dp, vertical = 32.dp),
    ) {
        Spacer(Modifier.weight(1f))
        PlayerPlaybackSummary(state)
        if (state.progressSaveFailed) {
            Text(
                text = stringResource(R.string.progress_save_failed),
                color = Danger,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        SeekablePlayerProgress(
            positionMillis = state.positionMillis,
            durationMillis = state.durationMillis,
            chapters = state.chapters,
            progressFocus = resolvedProgressFocus,
            playFocus = playFocus,
            onSeekTo = onSeekTo,
            onHideControls = onHideControls,
            onInteraction = onInteraction,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = formatPlayerDuration(state.positionMillis),
                color = Muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatRemainingDuration(state.positionMillis, state.durationMillis),
                color = Muted,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            PlayerIconButton(
                label = stringResource(R.string.previous_episode),
                iconRes = R.drawable.ic_player_previous,
                action = PlayerActionUiState(enabled = state.previousEnabled),
                onClick = onPrevious,
                upFocus = resolvedProgressFocus,
                downFocus = supplementaryGroupStartFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.rewind_seconds),
                iconRes = R.drawable.ic_player_replay_10,
                action = PlayerActionUiState(enabled = true),
                onClick = onRewind,
                upFocus = resolvedProgressFocus,
                downFocus = supplementaryGroupStartFocus,
            )
            PlayerIconButton(
                label = if (state.isPlaying) {
                    stringResource(R.string.pause)
                } else {
                    stringResource(R.string.play)
                },
                iconRes = if (state.isPlaying) {
                    R.drawable.ic_player_pause
                } else {
                    R.drawable.ic_player_play
                },
                action = PlayerActionUiState(enabled = true),
                onClick = onPlayPause,
                modifier = Modifier.focusRequester(playFocus),
                primary = true,
                upFocus = resolvedProgressFocus,
                downFocus = supplementaryGroupStartFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.forward_seconds),
                iconRes = R.drawable.ic_player_forward_10,
                action = PlayerActionUiState(enabled = true),
                onClick = onForward,
                modifier = Modifier.focusRequester(forwardFocus),
                upFocus = resolvedProgressFocus,
                downFocus = supplementaryGroupStartFocus,
                rightFocus = if (state.nextEnabled) nextFocus else supplementaryGroupStartFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.next_episode),
                iconRes = R.drawable.ic_player_next,
                action = PlayerActionUiState(enabled = state.nextEnabled),
                onClick = onNext,
                modifier = Modifier.focusRequester(nextFocus),
                upFocus = resolvedProgressFocus,
                downFocus = supplementaryGroupStartFocus,
                rightFocus = supplementaryGroupStartFocus,
            )
            Spacer(Modifier.weight(1f))
            PlayerIconButton(
                label = subtitleButtonLabel(state.subtitles, state.subtitleLabel),
                iconRes = R.drawable.ic_player_subtitles,
                action = state.subtitles,
                onClick = if (state.subtitles.error) {
                    onRetrySubtitles
                } else {
                    onOpenSubtitles
                },
                modifier = Modifier.focusRequester(subtitleFocus),
                upFocus = playFocus,
                leftFocus = episodeGroupEndFocus,
            )
            PlayerIconButton(
                label = formatPlaybackSpeed(state.playbackSpeed),
                iconRes = R.drawable.ic_player_speed,
                action = PlayerActionUiState(enabled = true),
                onClick = onOpenSpeed,
                modifier = Modifier.focusRequester(speedFocus),
                upFocus = playFocus,
                leftFocus = if (state.subtitles.enabled) subtitleFocus else episodeGroupEndFocus,
            )
            PlayerIconButton(
                label = danmakuButtonLabel(state.danmakus),
                iconRes = R.drawable.ic_player_danmaku,
                action = state.danmakus,
                onClick = if (state.danmakus.error) {
                    onRetryDanmakus
                } else {
                    onToggleDanmakus
                },
                upFocus = playFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.player_danmaku_settings_button),
                iconRes = R.drawable.ic_player_tune,
                action = state.danmakuSettings,
                onClick = onOpenDanmakuSettings,
                modifier = Modifier.focusRequester(danmakuSettingsFocus),
                upFocus = playFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.playback_quality),
                iconRes = R.drawable.ic_player_quality,
                action = state.quality,
                onClick = onOpenDefinitions,
                modifier = Modifier.focusRequester(definitionFocus),
                upFocus = playFocus,
            )
        }
    }
}

@Composable
private fun PlayerIconButton(
    label: String,
    @DrawableRes iconRes: Int,
    action: PlayerActionUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    upFocus: FocusRequester,
    downFocus: FocusRequester? = null,
    leftFocus: FocusRequester? = null,
    rightFocus: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            KaloscopeIconButton(
                onClick = onClick,
                enabled = action.enabled,
                selected = action.active,
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
                modifier = modifier
                    .size(if (primary) 48.dp else 42.dp)
                    .focusProperties {
                        up = upFocus
                        downFocus?.let { down = it }
                        leftFocus?.let { left = it }
                        rightFocus?.let { right = it }
                    }
                    .onFocusChanged { focused = it.isFocused }
                    .semantics {
                        contentDescription = label
                        role = Role.Button
                        if (action.error) {
                            error(label)
                        }
                    },
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (primary) 24.dp else 22.dp),
                )
            }
            if (action.error) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(Danger, CircleShape),
                )
            }
        }
        Box(
            modifier = Modifier.height(20.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (focused) {
                Text(
                    text = label,
                    color = OnBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun subtitleButtonLabel(
    action: PlayerActionUiState,
    selectedLabel: String?,
): String =
    when {
        action.error -> stringResource(R.string.retry_subtitles)
        !action.enabled -> stringResource(R.string.no_subtitles)
        action.active && !selectedLabel.isNullOrBlank() -> selectedLabel
        action.active -> stringResource(R.string.subtitles_on)
        else -> stringResource(R.string.subtitles_off)
    }

@Composable
private fun danmakuButtonLabel(action: PlayerActionUiState): String =
    when {
        action.error -> stringResource(R.string.retry_danmakus)
        !action.enabled -> stringResource(R.string.no_danmaku)
        action.active -> stringResource(R.string.danmaku_on)
        else -> stringResource(R.string.danmaku_off)
    }

@Composable
private fun SeekablePlayerProgress(
    positionMillis: Long,
    durationMillis: Long,
    chapters: List<MediaChapter>,
    progressFocus: FocusRequester,
    playFocus: FocusRequester,
    onSeekTo: (Long) -> Unit,
    onHideControls: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = durationMillis > 0
    val progressDescription = stringResource(R.string.player_progress)
    var focused by remember { mutableStateOf(false) }
    var previewMillis by remember { mutableStateOf<Long?>(null) }
    val displayPosition = previewMillis ?: positionMillis
    val progress = if (enabled) {
        (displayPosition.toFloat() / durationMillis).coerceIn(0f, 1f)
    } else {
        0f
    }
    val chapterMarkers = ChapterTimelinePolicy.markers(chapters, durationMillis)
    val currentChapterTitle = ChapterTimelinePolicy.currentTitle(chapters, displayPosition)

    LaunchedEffect(positionMillis, durationMillis, focused) {
        if (!focused) {
            previewMillis = null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .height(50.dp)
            .focusRequester(progressFocus)
            .focusProperties { down = playFocus }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                val command = PlayerControlKeyPolicy.command(
                    context = PlayerControlContext.Progress,
                    key = event.key.toPlayerRemoteKey() ?: return@onPreviewKeyEvent false,
                    phase = when (event.type) {
                        KeyEventType.KeyDown -> PlayerKeyPhase.Down
                        KeyEventType.KeyUp -> PlayerKeyPhase.Up
                        else -> return@onPreviewKeyEvent false
                    },
                ) ?: return@onPreviewKeyEvent false
                when (command) {
                    is PlayerControlCommand.PreviewSeek -> {
                        PlayerControlKeyPolicy.previewTarget(
                            currentTargetMillis = previewMillis ?: positionMillis,
                            durationMillis = durationMillis,
                            offsetMillis = command.offsetMillis,
                        )?.let { previewMillis = it }
                    }

                    PlayerControlCommand.SubmitSeekPreview -> {
                        previewMillis?.let(onSeekTo)
                        previewMillis = null
                    }

                    PlayerControlCommand.FocusPlayPause -> playFocus.requestFocus()
                    PlayerControlCommand.HideControls -> onHideControls()
                    else -> return@onPreviewKeyEvent false
                }
                onInteraction()
                true
            }
            .semantics {
                contentDescription = progressDescription
                if (!enabled) {
                    disabled()
                }
            }
            .focusable(enabled = enabled)
            .testTag("player-progress"),
        contentAlignment = Alignment.BottomStart,
    ) {
        val progressWidth = maxWidth
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(if (focused) 9.dp else 6.dp)
                .background(Color(0xFF4A5060), RoundedCornerShape(9.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Primary, RoundedCornerShape(9.dp)),
            )
            if (chapterMarkers.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("player-chapter-markers"),
                ) {
                    for (marker in chapterMarkers) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(
                                    x = (progressWidth * marker - 1.dp)
                                        .coerceIn(0.dp, progressWidth - 2.dp),
                                )
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color.White.copy(alpha = 0.8f)),
                        )
                    }
                }
            }
        }
        if (focused) {
            currentChapterTitle?.let { title ->
                Text(
                    text = title,
                    color = OnBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .testTag("player-current-chapter"),
                )
            }
            val targetWidth = 54.dp
            val targetOffset =
                (maxWidth * progress - targetWidth / 2)
                    .coerceIn(0.dp, maxWidth - targetWidth)
            Text(
                text = formatPlayerDuration(displayPosition),
                color = OnBackground,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(targetWidth)
                    .offset(x = targetOffset, y = 18.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = (maxWidth * progress - 9.dp)
                            .coerceIn(0.dp, maxWidth - 18.dp),
                    )
                    .size(18.dp)
                    .background(Primary.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White, CircleShape),
                )
            }
        }
    }
}

internal fun Key.toPlayerRemoteKey(): PlayerRemoteKey? =
    when (this) {
        Key.DirectionCenter,
        Key.Enter,
        Key.NumPadEnter,
        -> PlayerRemoteKey.Center

        Key.DirectionLeft -> PlayerRemoteKey.Left
        Key.DirectionRight -> PlayerRemoteKey.Right
        Key.DirectionUp -> PlayerRemoteKey.Up
        Key.DirectionDown -> PlayerRemoteKey.Down
        Key.Back -> PlayerRemoteKey.Back
        else -> null
    }

@Composable
internal fun PlayerBufferingIndicator(isRebuffering: Boolean) {
    var delayElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(isRebuffering) {
        delayElapsed = false
        if (isRebuffering) {
            delay(500)
            delayElapsed = true
        }
    }
    if (!isRebuffering || !delayElapsed) {
        return
    }

    val transition = rememberInfiniteTransition(label = "player-buffering")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "player-buffering-rotation",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("player-buffering-indicator"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xB3000000), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation),
            ) {
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.player_buffering),
                color = OnBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

internal fun formatPlayerDuration(milliseconds: Long): String {
    if (milliseconds <= 0) {
        return "00:00"
    }
    val totalSeconds = milliseconds / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
