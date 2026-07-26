package org.kaloscope.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.compose.ContentFrame
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.PanelSelected
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.model.NetworkDefinition
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.player.PlaybackController
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackFeedback
import org.kaloscope.tv.core.player.PlaybackFeedbackPolicy
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestNavigator
import org.kaloscope.tv.core.player.PlaybackSettingsPolicy
import org.kaloscope.tv.core.player.PlaybackSourceKind
import org.kaloscope.tv.core.player.ProgressReason
import org.kaloscope.tv.core.player.SubtitleSelectionPolicy
import org.kaloscope.tv.core.player.TranscodeResolution

@Composable
fun PlayerScreen(
    session: Session,
    state: PlayerUiState,
    controllerFactory: PlaybackControllerFactory,
    onProgress: (PlaybackRequest, Long, Long, ProgressReason) -> Unit,
    onSelectDefinition: (Int, Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetryExtra: (PlayerExtra) -> Unit,
    onBack: () -> Unit,
) {
    when (state) {
        PlayerUiState.Loading -> {
            BackHandler(onBack = onBack)
            PlayerMessage(
                title = stringResource(R.string.preparing_playback),
                description = stringResource(R.string.preparing_playback_description),
            )
        }

        PlayerUiState.MissingRequest -> {
            BackHandler(onBack = onBack)
            PlayerMessage(
                title = stringResource(R.string.playback_request_missing),
                description = stringResource(R.string.playback_request_missing_description),
                onBack = onBack,
            )
        }

        is PlayerUiState.Content -> PlayerContent(
            session = session,
            state = state,
            controllerFactory = controllerFactory,
            onProgress = onProgress,
            onSelectDefinition = onSelectDefinition,
            onPrevious = onPrevious,
            onNext = onNext,
            onRetryExtra = onRetryExtra,
            onBack = onBack,
        )
    }
}

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
private fun PlayerContent(
    session: Session,
    state: PlayerUiState.Content,
    controllerFactory: PlaybackControllerFactory,
    onProgress: (PlaybackRequest, Long, Long, ProgressReason) -> Unit,
    onSelectDefinition: (Int, Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetryExtra: (PlayerExtra) -> Unit,
    onBack: () -> Unit,
) {
    val playbackIdentity = state.request.playbackIdentity()
    var activeController by remember(playbackIdentity) {
        mutableStateOf<PlaybackController?>(null)
    }
    // API 23 may skip onStop, while newer Android versions support multi-window playback.
    if (android.os.Build.VERSION.SDK_INT > 23) {
        LifecycleStartEffect(playbackIdentity) {
            activeController = controllerFactory.create(
                session = session,
                request = state.request,
                subtitles = state.subtitles,
                probeDurationMillis = state.mediaProbe?.durationMillis ?: 0L,
                onProgress = onProgress,
            )
            onStopOrDispose {
                activeController?.release()
                activeController = null
            }
        }
    } else {
        LifecycleResumeEffect(playbackIdentity) {
            activeController = controllerFactory.create(
                session = session,
                request = state.request,
                subtitles = state.subtitles,
                probeDurationMillis = state.mediaProbe?.durationMillis ?: 0L,
                onProgress = onProgress,
            )
            onPauseOrDispose {
                activeController?.release()
                activeController = null
            }
        }
    }
    val controller = activeController
    if (controller == null) {
        PlayerMessage(
            title = stringResource(R.string.preparing_playback),
            description = stringResource(R.string.preparing_playback_description),
        )
        return
    }
    val status by controller.status.collectAsStateWithLifecycle()
    val feedback = PlaybackFeedbackPolicy.resolve(
        playbackState = status.playbackState,
        hasBeenReady = status.hasBeenReady,
        fallbackInProgress = status.fallbackInProgress,
        switchingItem = state.switchingItem,
        failure = status.failure,
    )
    var positionMillis by remember(playbackIdentity) { mutableLongStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    var sessionSubtitleSettings by remember(state.request.requestId) {
        mutableStateOf(state.request.subtitleSettings)
    }
    var selectedSubtitleTrackId by remember(state.request.requestId) {
        mutableStateOf(
            SubtitleSelectionPolicy.preferredTrackId(
                state.subtitles,
                state.request.subtitleSettings,
            ),
        )
    }
    var playbackSpeed by remember(state.request.requestId) {
        mutableFloatStateOf(1f)
    }
    var sessionDanmakuSettings by remember(state.request.requestId) {
        mutableStateOf(state.request.danmakuSettings)
    }
    var danmakuRuntimeAvailable by remember(playbackIdentity) {
        mutableStateOf(true)
    }
    var definitionDrawerOpen by remember { mutableStateOf(false) }
    var restoreDefinitionFocus by remember { mutableStateOf(false) }
    var danmakuDrawerOpen by remember { mutableStateOf(false) }
    var restoreDanmakuSettingsFocus by remember { mutableStateOf(false) }
    var subtitleDrawerOpen by remember { mutableStateOf(false) }
    var restoreSubtitleFocus by remember { mutableStateOf(false) }
    var speedDrawerOpen by remember { mutableStateOf(false) }
    var restoreSpeedFocus by remember { mutableStateOf(false) }
    var interactionVersion by remember { mutableLongStateOf(0) }
    val playerFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val definitionFocus = remember { FocusRequester() }
    val danmakuSettingsFocus = remember { FocusRequester() }
    val subtitleFocus = remember { FocusRequester() }
    val speedFocus = remember { FocusRequester() }
    val hasNext = PlaybackRequestNavigator.hasNext(state.request)

    BackHandler {
        val context = when {
            subtitleDrawerOpen -> PlayerBackContext.SubtitleDrawer
            speedDrawerOpen -> PlayerBackContext.SpeedDrawer
            danmakuDrawerOpen -> PlayerBackContext.DanmakuDrawer
            definitionDrawerOpen -> PlayerBackContext.DefinitionDrawer
            controlsVisible &&
                feedback in setOf(
                    PlaybackFeedback.Ready,
                    PlaybackFeedback.Rebuffering,
                    PlaybackFeedback.FallingBack,
                ) ->
                PlayerBackContext.Controls

            else -> PlayerBackContext.Player
        }
        when (PlayerControlKeyPolicy.backCommand(context)) {
            PlayerControlCommand.CloseSubtitleDrawer -> {
                subtitleDrawerOpen = false
                restoreSubtitleFocus = true
            }

            PlayerControlCommand.CloseSpeedDrawer -> {
                speedDrawerOpen = false
                restoreSpeedFocus = true
            }

            PlayerControlCommand.CloseDanmakuDrawer -> {
                danmakuDrawerOpen = false
                restoreDanmakuSettingsFocus = true
            }

            PlayerControlCommand.CloseDefinitionDrawer -> {
                definitionDrawerOpen = false
                restoreDefinitionFocus = true
            }

            PlayerControlCommand.HideControls -> controlsVisible = false
            PlayerControlCommand.ExitPlayer -> onBack()
            else -> Unit
        }
    }

    LaunchedEffect(playbackIdentity, state.subtitles) {
        selectedSubtitleTrackId = if (!sessionSubtitleSettings.enabled) {
            null
        } else {
            selectedSubtitleTrackId
                ?.takeIf { selected -> state.subtitles.any { it.id == selected } }
                ?: SubtitleSelectionPolicy.preferredTrackId(
                    state.subtitles,
                    sessionSubtitleSettings,
                )
        }
    }
    LaunchedEffect(controller, selectedSubtitleTrackId) {
        controller.selectSubtitle(selectedSubtitleTrackId)
    }
    LaunchedEffect(controller, sessionSubtitleSettings.timeOffsetSeconds) {
        controller.setSubtitleTimeOffset(sessionSubtitleSettings.timeOffsetSeconds)
    }
    LaunchedEffect(controller, playbackSpeed) {
        controller.setPlaybackSpeed(playbackSpeed)
    }
    LaunchedEffect(controller) {
        while (true) {
            positionMillis = controller.player.currentPosition.coerceAtLeast(0)
            delay(500)
        }
    }
    LaunchedEffect(controller) {
        while (true) {
            delay(15_000)
            controller.recordPeriodicProgress()
        }
    }
    LaunchedEffect(
        controlsVisible,
        interactionVersion,
        status.failure,
        status.fallbackInProgress,
        definitionDrawerOpen,
        danmakuDrawerOpen,
        subtitleDrawerOpen,
        speedDrawerOpen,
        feedback,
    ) {
        if (
            controlsVisible &&
            !definitionDrawerOpen &&
            !danmakuDrawerOpen &&
            !subtitleDrawerOpen &&
            !speedDrawerOpen &&
            feedback == PlaybackFeedback.Ready
        ) {
            delay(4_000)
            controlsVisible = false
        }
    }
    LaunchedEffect(status.fallbackInProgress) {
        if (status.fallbackInProgress) {
            controlsVisible = true
        }
    }
    LaunchedEffect(
        controlsVisible,
        definitionDrawerOpen,
        danmakuDrawerOpen,
        subtitleDrawerOpen,
        speedDrawerOpen,
        feedback,
    ) {
        if (
            controlsVisible &&
            feedback in setOf(
                PlaybackFeedback.Ready,
                PlaybackFeedback.Rebuffering,
                PlaybackFeedback.FallingBack,
            ) &&
            !definitionDrawerOpen &&
            !danmakuDrawerOpen &&
            !subtitleDrawerOpen &&
            !speedDrawerOpen &&
            !restoreDefinitionFocus &&
            !restoreDanmakuSettingsFocus &&
            !restoreSubtitleFocus &&
            !restoreSpeedFocus
        ) {
            playFocus.requestFocus()
        } else if (
            !controlsVisible &&
            !definitionDrawerOpen &&
            !danmakuDrawerOpen &&
            !subtitleDrawerOpen &&
            !speedDrawerOpen
        ) {
            playerFocus.requestFocus()
        }
    }
    LaunchedEffect(definitionDrawerOpen, restoreDefinitionFocus) {
        if (!definitionDrawerOpen && restoreDefinitionFocus) {
            withFrameNanos { }
            definitionFocus.requestFocus()
            restoreDefinitionFocus = false
        }
    }
    LaunchedEffect(danmakuDrawerOpen, restoreDanmakuSettingsFocus) {
        if (!danmakuDrawerOpen && restoreDanmakuSettingsFocus) {
            withFrameNanos { }
            danmakuSettingsFocus.requestFocus()
            restoreDanmakuSettingsFocus = false
        }
    }
    LaunchedEffect(subtitleDrawerOpen, restoreSubtitleFocus) {
        if (!subtitleDrawerOpen && restoreSubtitleFocus) {
            withFrameNanos { }
            subtitleFocus.requestFocus()
            restoreSubtitleFocus = false
        }
    }
    LaunchedEffect(speedDrawerOpen, restoreSpeedFocus) {
        if (!speedDrawerOpen && restoreSpeedFocus) {
            withFrameNanos { }
            speedFocus.requestFocus()
            restoreSpeedFocus = false
        }
    }
    LaunchedEffect(status.failure) {
        if (status.failure != null) {
            definitionDrawerOpen = false
            danmakuDrawerOpen = false
            subtitleDrawerOpen = false
            speedDrawerOpen = false
        }
    }
    LaunchedEffect(playbackIdentity, status.playbackState) {
        if (
            PlaybackSettingsPolicy.shouldAutoAdvance(
                playbackState = status.playbackState,
                autoplayNext = state.request.autoplayNext,
                hasNext = hasNext,
            )
        ) {
            controller.recordItemSwitchProgress()
            onNext()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocus)
            .focusable(
                enabled =
                    !controlsVisible &&
                        !definitionDrawerOpen &&
                        !danmakuDrawerOpen &&
                        !subtitleDrawerOpen &&
                        !speedDrawerOpen,
            )
            .onPreviewKeyEvent { event ->
                if (
                    controlsVisible ||
                    definitionDrawerOpen ||
                    danmakuDrawerOpen ||
                    subtitleDrawerOpen ||
                    speedDrawerOpen
                ) {
                    if (event.type == KeyEventType.KeyDown) {
                        interactionVersion += 1
                    }
                    return@onPreviewKeyEvent false
                }
                // Hidden controls reserve D-pad playback shortcuts at the page root.
                val command = PlayerControlKeyPolicy.command(
                    context = PlayerControlContext.HiddenControls,
                    key = event.key.toPlayerRemoteKey() ?: return@onPreviewKeyEvent false,
                    phase = when (event.type) {
                        KeyEventType.KeyDown -> PlayerKeyPhase.Down
                        KeyEventType.KeyUp -> PlayerKeyPhase.Up
                        else -> return@onPreviewKeyEvent false
                    },
                ) ?: return@onPreviewKeyEvent false
                when (command) {
                    PlayerControlCommand.TogglePlaybackAndShowControls -> {
                        controller.togglePlayPause()
                        controlsVisible = true
                        true
                    }

                    is PlayerControlCommand.SeekAndShowControls -> {
                        controller.seekBy(command.offsetMillis)
                        controlsVisible = true
                        true
                    }

                    PlayerControlCommand.ShowControls -> {
                        controlsVisible = true
                        true
                    }

                    else -> false
                }
            },
    ) {
        ContentFrame(
            player = controller.player,
            modifier = Modifier.fillMaxSize(),
        )
        if (selectedSubtitleTrackId != null && status.cues.isNotEmpty()) {
            AndroidView(
                factory = { context ->
                    SubtitleView(context).apply {
                        applySubtitleStyle(sessionSubtitleSettings)
                    }
                },
                update = { subtitleView ->
                    subtitleView.applySubtitleStyle(sessionSubtitleSettings)
                    subtitleView.setCues(status.cues)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (state.danmakus.isNotEmpty()) {
            AkDanmakuOverlay(
                player = controller.player,
                comments = state.danmakus,
                settings = sessionDanmakuSettings,
                onRuntimeAvailable = { danmakuRuntimeAvailable = it },
            )
        }
        if (
            controlsVisible &&
            feedback in setOf(
                PlaybackFeedback.Ready,
                PlaybackFeedback.Rebuffering,
                PlaybackFeedback.FallingBack,
            ) &&
            !definitionDrawerOpen &&
            !danmakuDrawerOpen &&
            !subtitleDrawerOpen &&
            !speedDrawerOpen &&
            !state.switchingItem
        ) {
            val definitions = (state.request as? PlaybackRequest.NetworkVideo)
                ?.source
                ?.definitions
                .orEmpty()
            val subtitlesFailed = PlayerExtra.Subtitles in state.extraErrors
            val danmakusFailed = PlayerExtra.Danmakus in state.extraErrors
            val danmakusAvailable =
                state.danmakus.isNotEmpty() && danmakuRuntimeAvailable && !danmakusFailed
            PlayerControls(
                state = PlayerControlsUiState(
                    title = state.request.title,
                    isPlaying = status.isPlaying,
                    positionMillis = positionMillis,
                    durationMillis = status.effectiveDurationMillis,
                    playbackModeLabel = playbackModeLabel(
                        mode = (state.request as? PlaybackRequest.LocalMedia)?.playbackMode,
                        sourceKind = status.sourceKind,
                        resolution =
                            (state.request as? PlaybackRequest.LocalMedia)?.transcodeResolution,
                    ),
                    playbackSpeed = playbackSpeed,
                    fallbackInProgress = status.fallbackInProgress,
                    progressSaveFailed = state.progressError != null,
                    previousEnabled =
                        PlaybackRequestNavigator.hasPrevious(state.request) &&
                            !state.switchingItem,
                    nextEnabled = hasNext && !state.switchingItem,
                    subtitles = PlayerActionUiState(
                        enabled = subtitlesFailed || state.subtitles.isNotEmpty(),
                        active =
                            state.subtitles.isNotEmpty() &&
                                !subtitlesFailed &&
                                selectedSubtitleTrackId != null,
                        error = subtitlesFailed,
                    ),
                    danmakus = PlayerActionUiState(
                        enabled = danmakusFailed || danmakusAvailable,
                        active = danmakusAvailable && sessionDanmakuSettings.enabled,
                        error = danmakusFailed,
                    ),
                    danmakuSettings = PlayerActionUiState(
                        enabled = danmakusAvailable,
                        error = danmakusFailed,
                    ),
                    quality = PlayerActionUiState(enabled = definitions.size > 1),
                    subtitleLabel = state.subtitles
                        .firstOrNull { it.id == selectedSubtitleTrackId }
                        ?.label,
                    chapters = state.mediaProbe?.chapters.orEmpty(),
                ),
                definitionFocus = definitionFocus,
                danmakuSettingsFocus = danmakuSettingsFocus,
                subtitleFocus = subtitleFocus,
                speedFocus = speedFocus,
                playFocus = playFocus,
                onPrevious = {
                    interactionVersion += 1
                    controller.recordItemSwitchProgress()
                    onPrevious()
                },
                onRewind = {
                    interactionVersion += 1
                    controller.seekBy(-10_000)
                },
                onPlayPause = {
                    interactionVersion += 1
                    controller.togglePlayPause()
                },
                onForward = {
                    interactionVersion += 1
                    controller.seekBy(10_000)
                },
                onNext = {
                    interactionVersion += 1
                    controller.recordItemSwitchProgress()
                    onNext()
                },
                onOpenSubtitles = {
                    interactionVersion += 1
                    subtitleDrawerOpen = true
                    speedDrawerOpen = false
                    definitionDrawerOpen = false
                    danmakuDrawerOpen = false
                },
                onOpenSpeed = {
                    interactionVersion += 1
                    speedDrawerOpen = true
                    subtitleDrawerOpen = false
                    definitionDrawerOpen = false
                    danmakuDrawerOpen = false
                },
                onToggleDanmakus = {
                    interactionVersion += 1
                    sessionDanmakuSettings = sessionDanmakuSettings.copy(
                        enabled = !sessionDanmakuSettings.enabled,
                    )
                },
                onOpenDanmakuSettings = {
                    interactionVersion += 1
                    danmakuDrawerOpen = true
                    definitionDrawerOpen = false
                    subtitleDrawerOpen = false
                    speedDrawerOpen = false
                },
                onOpenDefinitions = {
                    interactionVersion += 1
                    definitionDrawerOpen = true
                    danmakuDrawerOpen = false
                    subtitleDrawerOpen = false
                    speedDrawerOpen = false
                },
                onSeekTo = { position ->
                    interactionVersion += 1
                    controller.seekTo(position)
                },
                onHideControls = {
                    controlsVisible = false
                },
                onInteraction = {
                    interactionVersion += 1
                },
                onRetrySubtitles = {
                    interactionVersion += 1
                    onRetryExtra(PlayerExtra.Subtitles)
                },
                onRetryDanmakus = {
                    interactionVersion += 1
                    onRetryExtra(PlayerExtra.Danmakus)
                },
            )
        }
        if (definitionDrawerOpen) {
            PlayerDefinitionDrawer(
                definitions = (state.request as? PlaybackRequest.NetworkVideo)
                    ?.source
                    ?.definitions
                    .orEmpty(),
                selectedIndex = (state.request as? PlaybackRequest.NetworkVideo)
                    ?.source
                    ?.selectedDefinitionIndex,
                onSelect = { index ->
                    definitionDrawerOpen = false
                    restoreDefinitionFocus = true
                    onSelectDefinition(index, controller.player.currentPosition)
                },
            )
        }
        if (danmakuDrawerOpen) {
            PlayerDanmakuSettingsDrawer(
                settings = sessionDanmakuSettings,
                onChange = { sessionDanmakuSettings = it },
                onDismiss = {
                    danmakuDrawerOpen = false
                    restoreDanmakuSettingsFocus = true
                },
            )
        }
        if (subtitleDrawerOpen) {
            PlayerSubtitleSettingsDrawer(
                tracks = state.subtitles,
                selectedTrackId = selectedSubtitleTrackId,
                settings = sessionSubtitleSettings,
                onSelectTrack = { trackId ->
                    selectedSubtitleTrackId = trackId
                    sessionSubtitleSettings = sessionSubtitleSettings.copy(
                        enabled = trackId != null,
                    )
                },
                onChangeSettings = { sessionSubtitleSettings = it },
                onDismiss = {
                    subtitleDrawerOpen = false
                    restoreSubtitleFocus = true
                },
            )
        }
        if (speedDrawerOpen) {
            PlayerSpeedDrawer(
                speed = playbackSpeed,
                onSelect = { selectedSpeed ->
                    playbackSpeed = selectedSpeed
                    speedDrawerOpen = false
                    restoreSpeedFocus = true
                },
                onDismiss = {
                    speedDrawerOpen = false
                    restoreSpeedFocus = true
                },
            )
        }
        if (!state.switchingItem && state.switchError != null) {
            Text(
                text = stringResource(R.string.switch_episode_failed),
                color = Danger,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 34.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        PlayerFeedbackOverlay(
            feedback = feedback,
            failure = status.failure,
            sourceKind = status.sourceKind,
            onRetry = {
                controlsVisible = true
                interactionVersion += 1
                controller.retry()
            },
        )
    }
}

@Composable
internal fun PlayerDefinitionDrawer(
    definitions: List<NetworkDefinition>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(definitions, selectedIndex) {
        withFrameNanos { }
        initialFocus.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(390.dp)
                .background(Panel.copy(alpha = 0.96f))
                .padding(horizontal = 34.dp, vertical = 46.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.playback_quality),
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            definitions.forEachIndexed { index, definition ->
                PlayerButton(
                    text = definition.label,
                    onClick = { onSelect(index) },
                    modifier = if (index == selectedIndex || selectedIndex == null && index == 0) {
                        Modifier.focusRequester(initialFocus)
                    } else {
                        Modifier
                    },
                    active = index == selectedIndex,
                )
            }
        }
    }
}

@Composable
private fun PlayerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    active: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(if (primary) 58.dp else 48.dp),
        colors = ButtonDefaults.colors(
            containerColor = if (active) PanelSelected else PanelElevated,
            focusedContainerColor = if (primary) Color.White else Primary,
            contentColor = OnBackground,
            focusedContentColor = if (primary) Background else Color.White,
        ),
    ) {
        Text(text)
    }
}

@Composable
private fun playbackModeLabel(
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
private fun PlayerMessage(
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
                    Button(
                        onClick = it,
                        colors = ButtonDefaults.colors(focusedContainerColor = Primary),
                    ) {
                        Text(stringResource(R.string.back))
                    }
                }
            }
        }
    }
}

private fun PlaybackRequest.playbackIdentity(): String =
    when (this) {
        is PlaybackRequest.LocalMedia -> "$requestId:local:$mediaId"
        is PlaybackRequest.NetworkVideo ->
            "$requestId:network:${source.resourceId}:${source.selectedChapterIndex}:${source.url}"
    }

@androidx.annotation.OptIn(UnstableApi::class)
private fun SubtitleView.applySubtitleStyle(settings: SubtitleSettings) {
    setApplyEmbeddedStyles(false)
    setApplyEmbeddedFontSizes(false)
    setFractionalTextSize(
        SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * settings.fontScalePercent / 100f,
    )
    setBottomPaddingFraction(settings.verticalPositionPercent / 100f)
    setStyle(
        when (settings.displayMode) {
            SubtitleDisplayMode.Stroke -> CaptionStyleCompat(
                android.graphics.Color.WHITE,
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                android.graphics.Color.BLACK,
                null,
            )

            SubtitleDisplayMode.Background -> CaptionStyleCompat(
                android.graphics.Color.WHITE,
                0xB3000000.toInt(),
                android.graphics.Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_NONE,
                android.graphics.Color.TRANSPARENT,
                null,
            )
        },
    )
}
