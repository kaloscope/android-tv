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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.PanelElevated
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
    val fallbackInProgress: Boolean,
    val progressSaveFailed: Boolean,
    val previousEnabled: Boolean,
    val nextEnabled: Boolean,
    val subtitles: PlayerActionUiState,
    val danmakus: PlayerActionUiState,
    val danmakuSettings: PlayerActionUiState,
    val quality: PlayerActionUiState,
)

@Composable
internal fun PlayerControls(
    state: PlayerControlsUiState,
    playFocus: FocusRequester,
    definitionFocus: FocusRequester,
    danmakuSettingsFocus: FocusRequester,
    onPrevious: () -> Unit,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleDanmakus: () -> Unit,
    onOpenDanmakuSettings: () -> Unit,
    onOpenDefinitions: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onHideControls: () -> Unit,
    onInteraction: () -> Unit,
) {
    val progressFocus = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xA6000000), Color.Transparent, Color(0xF0050810)),
                ),
            )
            .testTag("player-control-layer")
            .padding(horizontal = 50.dp, vertical = 36.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = state.title,
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = state.playbackModeLabel,
                    color = OnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.fallbackInProgress) {
                    Text(
                        text = stringResource(R.string.switching_to_transcode),
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        if (state.progressSaveFailed) {
            Text(
                text = stringResource(R.string.progress_save_failed),
                color = Danger,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlayerDuration(state.positionMillis),
                color = OnBackground,
                fontSize = 14.sp,
            )
            Spacer(Modifier.width(14.dp))
            SeekablePlayerProgress(
                positionMillis = state.positionMillis,
                durationMillis = state.durationMillis,
                progressFocus = progressFocus,
                playFocus = playFocus,
                onSeekTo = onSeekTo,
                onHideControls = onHideControls,
                onInteraction = onInteraction,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = formatPlayerDuration(state.durationMillis),
                color = OnBackground,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            PlayerIconButton(
                label = stringResource(R.string.previous_episode),
                iconRes = R.drawable.ic_player_previous,
                action = PlayerActionUiState(enabled = state.previousEnabled),
                onClick = onPrevious,
                upFocus = progressFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.rewind_seconds),
                iconRes = R.drawable.ic_player_replay_10,
                action = PlayerActionUiState(enabled = true),
                onClick = onRewind,
                upFocus = progressFocus,
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
                upFocus = progressFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.forward_seconds),
                iconRes = R.drawable.ic_player_forward_10,
                action = PlayerActionUiState(enabled = true),
                onClick = onForward,
                upFocus = progressFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.next_episode),
                iconRes = R.drawable.ic_player_next,
                action = PlayerActionUiState(enabled = state.nextEnabled),
                onClick = onNext,
                upFocus = progressFocus,
            )
            Spacer(Modifier.weight(1f))
            PlayerIconButton(
                label = subtitleButtonLabel(state.subtitles),
                iconRes = R.drawable.ic_player_subtitles,
                action = state.subtitles,
                onClick = onToggleSubtitles,
                upFocus = progressFocus,
            )
            PlayerIconButton(
                label = danmakuButtonLabel(state.danmakus),
                iconRes = R.drawable.ic_player_danmaku,
                action = state.danmakus,
                onClick = onToggleDanmakus,
                upFocus = progressFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.player_danmaku_settings_button),
                iconRes = R.drawable.ic_player_tune,
                action = state.danmakuSettings,
                onClick = onOpenDanmakuSettings,
                modifier = Modifier.focusRequester(danmakuSettingsFocus),
                upFocus = progressFocus,
            )
            PlayerIconButton(
                label = stringResource(R.string.playback_quality),
                iconRes = R.drawable.ic_player_quality,
                action = state.quality,
                onClick = onOpenDefinitions,
                modifier = Modifier.focusRequester(definitionFocus),
                upFocus = progressFocus,
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
) {
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(
                onClick = onClick,
                enabled = action.enabled,
                modifier = modifier
                    .size(if (primary) 54.dp else 50.dp)
                    .focusProperties { up = upFocus }
                    .onFocusChanged { focused = it.isFocused }
                    .semantics {
                        contentDescription = label
                        role = Role.Button
                        selected = action.active
                        if (action.error) {
                            error(label)
                        }
                    }
                    .alpha(if (action.enabled) 1f else 0.48f),
                shape = IconButtonDefaults.shape(shape = CircleShape),
                colors = IconButtonDefaults.colors(
                    containerColor = when {
                        action.active -> Primary
                        else -> PanelElevated
                    },
                    focusedContainerColor = Color.White,
                    contentColor = OnBackground,
                    focusedContentColor = Background,
                    disabledContainerColor = PanelElevated,
                    disabledContentColor = Muted,
                ),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (primary) 29.dp else 26.dp),
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
            modifier = Modifier.height(22.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (focused) {
                Text(
                    text = label,
                    color = OnBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun subtitleButtonLabel(action: PlayerActionUiState): String =
    when {
        action.error -> stringResource(R.string.subtitle_unavailable)
        !action.enabled -> stringResource(R.string.no_subtitles)
        action.active -> stringResource(R.string.subtitles_on)
        else -> stringResource(R.string.subtitles_off)
    }

@Composable
private fun danmakuButtonLabel(action: PlayerActionUiState): String =
    when {
        action.error -> stringResource(R.string.danmaku_unavailable)
        !action.enabled -> stringResource(R.string.no_danmaku)
        action.active -> stringResource(R.string.danmaku_on)
        else -> stringResource(R.string.danmaku_off)
    }

@Composable
private fun SeekablePlayerProgress(
    positionMillis: Long,
    durationMillis: Long,
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

    LaunchedEffect(positionMillis, durationMillis, focused) {
        if (!focused) {
            previewMillis = null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .height(34.dp)
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
        Box(
            modifier = Modifier
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
        }
        if (focused) {
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
                    .offset(x = targetOffset),
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
