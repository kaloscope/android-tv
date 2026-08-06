package org.kaloscope.tv.feature.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.model.MediaChapter
import org.kaloscope.tv.core.player.ChapterTimelinePolicy
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.KaloscopeMotion
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Subtle
import kotlinx.coroutines.delay

internal data class PlayerActionUiState(
    val enabled: Boolean,
    val active: Boolean = false,
    val error: Boolean = false,
)

internal data class PlayerControlsUiState(
    val title: String,
    val playWhenReady: Boolean,
    val positionMillis: Long,
    val durationMillis: Long,
    val playbackModeLabel: String,
    val qualityControlLabel: String = playbackModeLabel,
    val playbackSpeed: Float,
    val fallbackInProgress: Boolean,
    val progressSaveFailed: Boolean,
    val previousEnabled: Boolean,
    val nextEnabled: Boolean,
    val subtitles: PlayerActionUiState,
    val danmakus: PlayerActionUiState,
    val settings: PlayerActionUiState,
    val quality: PlayerActionUiState,
    val secondaryTitle: String? = null,
    val chapters: List<MediaChapter> = emptyList(),
)

@Composable
internal fun PlayerInfoPreview(state: PlayerControlsUiState) {
    val accentPalette = LocalAccentPalette.current
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
                    .background(
                        playerProgressColor(
                            playWhenReady = state.playWhenReady,
                            activeColor = accentPalette.primary,
                        ),
                        RoundedCornerShape(9.dp),
                    ),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PlayerPlaybackSummary(
    state: PlayerControlsUiState,
) {
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
            state.secondaryTitle?.takeIf(String::isNotBlank)?.let { secondaryTitle ->
                Text(
                    text = secondaryTitle,
                    color = Muted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.fallbackInProgress) {
                Text(
                    text = stringResource(R.string.switching_to_transcode),
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        PlayerStatusChip(
            label = state.playbackModeLabel,
            modifier = Modifier.testTag("player-playback-quality-status"),
        )
        Spacer(Modifier.width(6.dp))
        PlayerStatusChip(
            label = formatPlaybackSpeed(state.playbackSpeed),
            modifier = Modifier.testTag("player-playback-speed-status"),
        )
    }
}

@Composable
private fun PlayerStatusChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .background(Color(0x66293040), PlayerControlPillShape)
            .border(PlayerControlBorder, PlayerControlPillShape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = OnBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private val PlayerControlPillShape = RoundedCornerShape(50)
private val PlayerControlBorder = BorderStroke(1.dp, Color(0x996F7888))

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
    settingsFocus: FocusRequester,
    subtitleFocus: FocusRequester,
    speedFocus: FocusRequester,
    onPrevious: () -> Unit,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onOpenSpeed: () -> Unit,
    onToggleDanmakus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDefinitions: () -> Unit,
    onSeekPreviewBy: (Long) -> Unit,
    onSeekPreviewFinished: () -> Unit,
    onHideControls: () -> Unit,
    onInteraction: () -> Unit,
    actionRowVisible: Boolean = true,
    onActionRowVisibilityChange: (Boolean) -> Unit = {},
    onRetrySubtitles: () -> Unit = {},
    onRetryDanmakus: () -> Unit = {},
) {
    var playFocusRequestVersion by remember { mutableLongStateOf(0) }
    val defaultProgressFocus = remember { FocusRequester() }
    val resolvedProgressFocus = progressFocus ?: defaultProgressFocus
    val forwardFocus = remember { FocusRequester() }
    val nextFocus = remember { FocusRequester() }
    val danmakuFocus = remember { FocusRequester() }
    val episodeGroupEndFocus = if (state.nextEnabled) nextFocus else forwardFocus
    val visibleAuxiliaryControls = PlayerAuxiliaryControlPolicy.visibleControls(
        subtitles = state.subtitles,
        danmakus = state.danmakus,
        quality = state.quality,
        settings = state.settings,
    )
    val auxiliaryFocusRequesters = mapOf(
        PlayerAuxiliaryControl.Subtitle to subtitleFocus,
        PlayerAuxiliaryControl.Danmaku to danmakuFocus,
        PlayerAuxiliaryControl.Speed to speedFocus,
        PlayerAuxiliaryControl.Quality to definitionFocus,
        PlayerAuxiliaryControl.Settings to settingsFocus,
    )
    val supplementaryGroupStartFocus = auxiliaryFocusRequesters.getValue(
        visibleAuxiliaryControls.first(),
    )
    val subtitleLabel = when {
        state.subtitles.error -> stringResource(R.string.retry)
        state.subtitles.active -> stringResource(R.string.subtitles_on)
        else -> stringResource(R.string.subtitles_off)
    }
    val danmakuLabel = when {
        state.danmakus.error -> stringResource(R.string.retry)
        state.danmakus.active -> stringResource(R.string.danmaku_on)
        else -> stringResource(R.string.danmaku_off)
    }
    val subtitleAccessibilityLabel = if (state.subtitles.error) {
        stringResource(R.string.retry_subtitles)
    } else {
        subtitleLabel
    }
    val danmakuAccessibilityLabel = if (state.danmakus.error) {
        stringResource(R.string.retry_danmakus)
    } else {
        danmakuLabel
    }
    LaunchedEffect(playFocusRequestVersion) {
        if (playFocusRequestVersion > 0) {
            withFrameNanos { }
            playFocus.requestFocus()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(playerControlScrim())
            .testTag("player-control-layer")
            .padding(
                start = 50.dp,
                top = 32.dp,
                end = 50.dp,
                bottom = 40.dp,
            ),
    ) {
        Spacer(Modifier.weight(1f))
        PlayerPlaybackSummary(state = state)
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
            playWhenReady = state.playWhenReady,
            onProgressFocused = { onActionRowVisibilityChange(false) },
            onShowActions = {
                onActionRowVisibilityChange(true)
                playFocusRequestVersion += 1
            },
            onPlayPause = onPlayPause,
            onSeekPreviewBy = onSeekPreviewBy,
            onSeekPreviewFinished = onSeekPreviewFinished,
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
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PlayerActionRowVisibility(
                visible = actionRowVisible,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player-control-row"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerCircleButton(
                            label = stringResource(R.string.previous_episode),
                            iconRes = R.drawable.ic_action_previous,
                            action = PlayerActionUiState(enabled = state.previousEnabled),
                            onClick = onPrevious,
                            modifier = Modifier.testTag("player-previous"),
                            upFocus = resolvedProgressFocus,
                            downFocus = supplementaryGroupStartFocus,
                        )
                        PlayerPillButton(
                            visibleLabel = stringResource(R.string.seek_seconds_short),
                            accessibilityLabel = stringResource(R.string.rewind_seconds),
                            iconRes = R.drawable.ic_action_seek_backward,
                            action = PlayerActionUiState(enabled = true),
                            onClick = onRewind,
                            modifier = Modifier.testTag("player-rewind"),
                            upFocus = resolvedProgressFocus,
                            downFocus = supplementaryGroupStartFocus,
                        )
                        val playPauseLabel = if (state.playWhenReady) {
                            stringResource(R.string.pause)
                        } else {
                            stringResource(R.string.play)
                        }
                        PlayerPillButton(
                            visibleLabel = playPauseLabel,
                            accessibilityLabel = playPauseLabel,
                            iconRes = if (state.playWhenReady) {
                                R.drawable.ic_action_pause
                            } else {
                                R.drawable.ic_action_play
                            },
                            action = PlayerActionUiState(enabled = true),
                            onClick = onPlayPause,
                            modifier = Modifier
                                .focusRequester(playFocus)
                                .testTag("player-play-pause"),
                            height = 48.dp,
                            minWidth = 96.dp,
                            primary = true,
                            upFocus = resolvedProgressFocus,
                            downFocus = supplementaryGroupStartFocus,
                        )
                        PlayerPillButton(
                            visibleLabel = stringResource(R.string.seek_seconds_short),
                            accessibilityLabel = stringResource(R.string.forward_seconds),
                            iconRes = R.drawable.ic_action_seek_forward,
                            action = PlayerActionUiState(enabled = true),
                            onClick = onForward,
                            modifier = Modifier
                                .focusRequester(forwardFocus)
                                .testTag("player-forward"),
                            upFocus = resolvedProgressFocus,
                            downFocus = supplementaryGroupStartFocus,
                            rightFocus = if (state.nextEnabled) {
                                nextFocus
                            } else {
                                supplementaryGroupStartFocus
                            },
                        )
                        PlayerCircleButton(
                            label = stringResource(R.string.next_episode),
                            iconRes = R.drawable.ic_action_next,
                            action = PlayerActionUiState(enabled = state.nextEnabled),
                            onClick = onNext,
                            modifier = Modifier
                                .focusRequester(nextFocus)
                                .testTag("player-next"),
                            upFocus = resolvedProgressFocus,
                            downFocus = supplementaryGroupStartFocus,
                            rightFocus = supplementaryGroupStartFocus,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        visibleAuxiliaryControls.forEachIndexed { index, control ->
                            val leftFocus = visibleAuxiliaryControls.getOrNull(index - 1)
                                ?.let(auxiliaryFocusRequesters::getValue)
                                ?: episodeGroupEndFocus
                            val rightFocus = visibleAuxiliaryControls.getOrNull(index + 1)
                                ?.let(auxiliaryFocusRequesters::getValue)
                                ?: FocusRequester.Cancel
                            val focusRequester = auxiliaryFocusRequesters.getValue(control)
                            when (control) {
                                PlayerAuxiliaryControl.Subtitle -> PlayerAuxiliaryButton(
                                    visibleLabel = subtitleLabel,
                                    accessibilityLabel = subtitleAccessibilityLabel,
                                    labelTag = "player-subtitles-label",
                                    iconRes = R.drawable.ic_settings_subtitle,
                                    action = state.subtitles,
                                    onClick = if (state.subtitles.error) {
                                        onRetrySubtitles
                                    } else {
                                        onToggleSubtitles
                                    },
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .testTag("player-subtitles"),
                                    upFocus = playFocus,
                                    leftFocus = leftFocus,
                                    rightFocus = rightFocus,
                                )

                                PlayerAuxiliaryControl.Danmaku -> PlayerAuxiliaryButton(
                                    visibleLabel = danmakuLabel,
                                    accessibilityLabel = danmakuAccessibilityLabel,
                                    labelTag = "player-danmaku-label",
                                    iconRes = R.drawable.ic_settings_danmaku,
                                    action = state.danmakus,
                                    onClick = if (state.danmakus.error) {
                                        onRetryDanmakus
                                    } else {
                                        onToggleDanmakus
                                    },
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .testTag("player-danmaku"),
                                    upFocus = playFocus,
                                    leftFocus = leftFocus,
                                    rightFocus = rightFocus,
                                )

                                PlayerAuxiliaryControl.Speed -> {
                                    val speedLabel = formatPlaybackSpeed(state.playbackSpeed)
                                    PlayerAuxiliaryButton(
                                        visibleLabel = speedLabel,
                                        accessibilityLabel = speedLabel,
                                        labelTag = "player-speed-label",
                                        iconRes = R.drawable.ic_action_playback_speed,
                                        action = PlayerActionUiState(enabled = true),
                                        onClick = onOpenSpeed,
                                        modifier = Modifier
                                            .focusRequester(focusRequester)
                                            .testTag("player-speed"),
                                        upFocus = playFocus,
                                        leftFocus = leftFocus,
                                        rightFocus = rightFocus,
                                    )
                                }

                                PlayerAuxiliaryControl.Quality -> PlayerAuxiliaryButton(
                                    visibleLabel = state.qualityControlLabel,
                                    accessibilityLabel = stringResource(
                                        R.string.playback_quality_with_value,
                                        state.qualityControlLabel,
                                    ),
                                    labelTag = "player-quality-label",
                                    iconRes = R.drawable.ic_player_quality,
                                    action = state.quality,
                                    onClick = onOpenDefinitions,
                                    expandedWidth = 160.dp,
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .testTag("player-quality"),
                                    upFocus = playFocus,
                                    leftFocus = leftFocus,
                                    rightFocus = rightFocus,
                                )

                                PlayerAuxiliaryControl.Settings -> PlayerAuxiliaryButton(
                                    visibleLabel = stringResource(R.string.settings),
                                    accessibilityLabel = stringResource(R.string.settings),
                                    labelTag = "player-settings-label",
                                    iconRes = R.drawable.ic_action_filter,
                                    action = state.settings,
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .testTag("player-settings"),
                                    upFocus = playFocus,
                                    leftFocus = leftFocus,
                                    rightFocus = rightFocus,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerActionRowVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(KaloscopeMotion.ContentMillis)) +
            expandVertically(
                animationSpec = tween(KaloscopeMotion.ContentMillis),
                expandFrom = Alignment.Bottom,
            ),
        exit = fadeOut(tween(KaloscopeMotion.ContentMillis)) +
            shrinkVertically(
                animationSpec = tween(KaloscopeMotion.ContentMillis),
                shrinkTowards = Alignment.Bottom,
            ),
    ) {
        Column {
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PlayerAuxiliaryButton(
    visibleLabel: String,
    accessibilityLabel: String,
    labelTag: String,
    @DrawableRes iconRes: Int,
    action: PlayerActionUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expandedWidth: Dp = 92.dp,
    upFocus: FocusRequester,
    downFocus: FocusRequester? = null,
    leftFocus: FocusRequester? = null,
    rightFocus: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val showLabel = focused && action.enabled
    val width by animateDpAsState(
        targetValue = if (showLabel) expandedWidth else 42.dp,
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "player-auxiliary-width",
    )

    Box(contentAlignment = Alignment.TopEnd) {
        KaloscopeButton(
            onClick = onClick,
            enabled = action.enabled,
            selected = action.active,
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
            shape = PlayerControlPillShape,
            contentPadding = PaddingValues(0.dp),
            modifier = modifier
                .width(width)
                .height(42.dp)
                .onFocusChanged { focused = it.isFocused }
                .playerFocusProperties(upFocus, downFocus, leftFocus, rightFocus)
                .playerControlSemantics(accessibilityLabel, action),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (showLabel) 10.dp else 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                if (showLabel) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = visibleLabel,
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(labelTag),
                    )
                }
            }
        }
        if (action.error) {
            PlayerControlErrorBadge()
        }
    }
}

@Composable
private fun PlayerCircleButton(
    label: String,
    @DrawableRes iconRes: Int,
    action: PlayerActionUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    upFocus: FocusRequester,
    downFocus: FocusRequester? = null,
    leftFocus: FocusRequester? = null,
    rightFocus: FocusRequester? = null,
) {
    Box(contentAlignment = Alignment.TopEnd) {
        KaloscopeIconButton(
            onClick = onClick,
            enabled = action.enabled,
            selected = action.active,
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
            modifier = modifier
                .size(42.dp)
                .playerFocusProperties(upFocus, downFocus, leftFocus, rightFocus)
                .playerControlSemantics(label, action),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
        if (action.error) {
            PlayerControlErrorBadge()
        }
    }
}

@Composable
private fun PlayerPillButton(
    visibleLabel: String,
    accessibilityLabel: String,
    @DrawableRes iconRes: Int,
    action: PlayerActionUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    minWidth: Dp = 76.dp,
    primary: Boolean = false,
    upFocus: FocusRequester,
    downFocus: FocusRequester? = null,
    leftFocus: FocusRequester? = null,
    rightFocus: FocusRequester? = null,
) {
    Box(contentAlignment = Alignment.TopEnd) {
        KaloscopeButton(
            onClick = onClick,
            enabled = action.enabled,
            selected = action.active,
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
            shape = PlayerControlPillShape,
            contentPadding = PaddingValues(0.dp),
            modifier = modifier
                .height(height)
                .widthIn(min = minWidth)
                .playerFocusProperties(upFocus, downFocus, leftFocus, rightFocus)
                .playerControlSemantics(accessibilityLabel, action),
        ) {
            Row(
                modifier = Modifier
                    .height(height)
                    .widthIn(min = minWidth)
                    .padding(horizontal = if (primary) 18.dp else 14.dp)
                    .then(
                        if (primary) {
                            Modifier.testTag("player-play-pause-content")
                        } else {
                            Modifier
                        },
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (primary) 24.dp else 22.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = visibleLabel,
                    fontSize = if (primary) 16.sp else 15.sp,
                    lineHeight = if (primary) 16.sp else 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = if (primary) {
                        Modifier.testTag("player-play-pause-label")
                    } else {
                        Modifier
                    },
                )
            }
        }
        if (action.error) {
            PlayerControlErrorBadge()
        }
    }
}

private fun Modifier.playerFocusProperties(
    upFocus: FocusRequester,
    downFocus: FocusRequester?,
    leftFocus: FocusRequester?,
    rightFocus: FocusRequester?,
): Modifier =
    focusProperties {
        up = upFocus
        downFocus?.let { down = it }
        leftFocus?.let { left = it }
        rightFocus?.let { right = it }
    }

private fun Modifier.playerControlSemantics(
    label: String,
    action: PlayerActionUiState,
): Modifier =
    semantics {
        contentDescription = label
        role = Role.Button
        if (action.error) {
            error(label)
        }
    }

@Composable
private fun PlayerControlErrorBadge() {
    Box(
        modifier = Modifier
            .size(9.dp)
            .background(Danger, CircleShape),
    )
}

@Composable
private fun SeekablePlayerProgress(
    positionMillis: Long,
    durationMillis: Long,
    chapters: List<MediaChapter>,
    progressFocus: FocusRequester,
    playFocus: FocusRequester,
    playWhenReady: Boolean,
    onProgressFocused: () -> Unit,
    onShowActions: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekPreviewBy: (Long) -> Unit,
    onSeekPreviewFinished: () -> Unit,
    onHideControls: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentPalette = LocalAccentPalette.current
    val enabled = durationMillis > 0
    val progressDescription = stringResource(R.string.player_progress)
    var focused by remember { mutableStateOf(false) }
    val displayPosition = positionMillis
    val progress = if (enabled) {
        (displayPosition.toFloat() / durationMillis).coerceIn(0f, 1f)
    } else {
        0f
    }
    val chapterMarkers = ChapterTimelinePolicy.markers(chapters, durationMillis)
    val currentChapterTitle = ChapterTimelinePolicy.currentTitle(chapters, displayPosition)
    val progressColor = playerProgressColor(
        playWhenReady = playWhenReady,
        activeColor = accentPalette.primary,
    )

    BoxWithConstraints(
        modifier = modifier
            .height(50.dp)
            .focusRequester(progressFocus)
            .focusProperties {
                up = FocusRequester.Cancel
                down = playFocus
            }
            .onFocusChanged {
                focused = it.isFocused
                if (focused) {
                    onProgressFocused()
                }
            }
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
                    is PlayerControlCommand.PreviewSeek ->
                        onSeekPreviewBy(command.offsetMillis)

                    PlayerControlCommand.SubmitSeekPreview -> onSeekPreviewFinished()

                    PlayerControlCommand.TogglePlaybackAndShowControls -> onPlayPause()

                    is PlayerControlCommand.ShowFullControls -> {
                        if (command.focusTarget != PlayerControlFocusTarget.PlayPause) {
                            return@onPreviewKeyEvent false
                        }
                        onShowActions()
                    }
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
                .height(18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (focused) 9.dp else 6.dp)
                    .background(Color(0xFF4A5060), RoundedCornerShape(9.dp))
                    .testTag("player-progress-track"),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(progressColor, RoundedCornerShape(9.dp))
                        .testTag("player-progress-played"),
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
            if (enabled) {
                val thumbContainerSize = if (focused) 20.dp else 16.dp
                val thumbRadius = thumbContainerSize / 2
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(
                            x = (progressWidth * progress - thumbRadius)
                                .coerceIn(0.dp, progressWidth - thumbContainerSize),
                        )
                        .size(thumbContainerSize)
                        .background(
                            if (focused) {
                                progressColor.copy(alpha = 0.28f)
                            } else {
                                Color.Transparent
                            },
                            CircleShape,
                        )
                        .testTag("player-progress-thumb"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(progressColor, CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.94f), CircleShape)
                            .testTag("player-progress-thumb-ring"),
                    )
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
        }
    }
}

private fun playerProgressColor(
    playWhenReady: Boolean,
    activeColor: Color,
): Color = if (playWhenReady) activeColor else Subtle

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
