package org.kaloscope.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.compose.ContentFrame
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.player.PlaybackController
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackFeedback
import org.kaloscope.tv.core.player.PlaybackFeedbackPolicy
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestNavigator
import org.kaloscope.tv.core.player.PlaybackSettingsPolicy
import org.kaloscope.tv.core.player.ProgressReason

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
            KaloscopeLoadingLayout("player-loading")
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
    var requestSessionState by remember(state.request.requestId) {
        mutableStateOf(
            PlayerRequestSessionState.initial(
                request = state.request,
                tracks = state.subtitles,
            ),
        )
    }
    LaunchedEffect(state.request.requestId, state.subtitles) {
        requestSessionState = requestSessionState.updateRequest(
            request = state.request,
            tracks = state.subtitles,
        )
    }
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
        KaloscopeLoadingLayout("player-loading")
        return
    }
    val sessionSettings = requestSessionState.sessionSettings
    val playbackSpeed = requestSessionState.playbackSpeed
    val status by controller.status.collectAsStateWithLifecycle()
    val feedback = PlaybackFeedbackPolicy.resolve(
        playbackState = status.playbackState,
        hasBeenReady = status.hasBeenReady,
        fallbackInProgress = status.fallbackInProgress,
        switchingItem = state.switchingItem,
        failure = status.failure,
        playWhenReady = status.playWhenReady,
    )
    val seekScope = rememberCoroutineScope()
    val seekCoordinator = remember(playbackIdentity, controller) {
        PlayerSeekCoordinator(
            scope = seekScope,
            onSeek = controller::seekTo,
        )
    }
    DisposableEffect(seekCoordinator) {
        onDispose(seekCoordinator::cancelPendingInteraction)
    }
    val seekState by seekCoordinator.state.collectAsStateWithLifecycle()
    val initialControlTransition = PlayerControlLayerPolicy.initialTransition()
    var controlLayer by remember(playbackIdentity) {
        mutableStateOf(initialControlTransition.layer)
    }
    var requestedControlsFocus by remember(playbackIdentity) {
        mutableStateOf(
            initialControlTransition.focusTarget ?: PlayerControlFocusTarget.Progress,
        )
    }
    var actionRowVisible by remember(playbackIdentity) {
        mutableStateOf(initialControlTransition.actionRowVisible == true)
    }
    var danmakuRuntimeAvailable by remember(playbackIdentity) {
        mutableStateOf(true)
    }
    var definitionDrawerOpen by remember { mutableStateOf(false) }
    var restoreDefinitionFocus by remember { mutableStateOf(false) }
    var settingsDrawerOpen by remember { mutableStateOf(false) }
    var restoreSettingsFocus by remember { mutableStateOf(false) }
    var speedDrawerOpen by remember { mutableStateOf(false) }
    var restoreSpeedFocus by remember { mutableStateOf(false) }
    var interactionVersion by remember { mutableLongStateOf(0) }
    var playbackToggleFeedbackId by remember(playbackIdentity) { mutableLongStateOf(0) }
    var playbackToggleFeedback by remember(playbackIdentity) {
        mutableStateOf<PlayerPlaybackToggleEvent?>(null)
    }
    val playerFocus = remember { FocusRequester() }
    val progressFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val definitionFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val subtitleFocus = remember { FocusRequester() }
    val speedFocus = remember { FocusRequester() }
    val hasNext = PlaybackRequestNavigator.hasNext(state.request)
    val togglePlaybackWithFeedback = {
        val playWhenReady = controller.togglePlayPause()
        playbackToggleFeedbackId += 1
        playbackToggleFeedback = PlayerPlaybackToggleEvent(
            id = playbackToggleFeedbackId,
            playWhenReady = playWhenReady,
        )
    }

    BackHandler {
        val context = when {
            settingsDrawerOpen -> PlayerBackContext.SettingsDrawer
            speedDrawerOpen -> PlayerBackContext.SpeedDrawer
            definitionDrawerOpen -> PlayerBackContext.DefinitionDrawer
            controlLayer != PlayerControlLayer.Hidden &&
                feedback in setOf(
                    PlaybackFeedback.Ready,
                    PlaybackFeedback.Rebuffering,
                    PlaybackFeedback.FallingBack,
                ) ->
                PlayerBackContext.Controls

            else -> PlayerBackContext.Player
        }
        when (PlayerControlKeyPolicy.backCommand(context)) {
            PlayerControlCommand.CloseSettingsDrawer -> {
                settingsDrawerOpen = false
                restoreSettingsFocus = true
            }

            PlayerControlCommand.CloseSpeedDrawer -> {
                speedDrawerOpen = false
                restoreSpeedFocus = true
            }

            PlayerControlCommand.CloseDefinitionDrawer -> {
                definitionDrawerOpen = false
                restoreDefinitionFocus = true
            }

            PlayerControlCommand.HideControls -> controlLayer = PlayerControlLayer.Hidden
            PlayerControlCommand.ExitPlayer -> onBack()
            else -> Unit
        }
    }

    LaunchedEffect(controller, sessionSettings.selectedSubtitleTrackId) {
        controller.selectSubtitle(sessionSettings.selectedSubtitleTrackId)
    }
    LaunchedEffect(controller, sessionSettings.subtitleSettings.timeOffsetSeconds) {
        controller.setSubtitleTimeOffset(
            sessionSettings.subtitleSettings.timeOffsetSeconds,
        )
    }
    LaunchedEffect(controller, playbackSpeed) {
        controller.setPlaybackSpeed(playbackSpeed)
    }
    LaunchedEffect(controller, seekCoordinator) {
        while (true) {
            seekCoordinator.reportPlayerPosition(controller.player.currentPosition)
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
        controlLayer,
        actionRowVisible,
        interactionVersion,
        status.failure,
        status.fallbackInProgress,
        definitionDrawerOpen,
        settingsDrawerOpen,
        speedDrawerOpen,
        feedback,
    ) {
        val autoHideDelayMillis = PlayerControlLayerPolicy.autoHideDelayMillis(
            layer = controlLayer,
            actionRowVisible = actionRowVisible,
        )
        if (
            autoHideDelayMillis != null &&
            !definitionDrawerOpen &&
            !settingsDrawerOpen &&
            !speedDrawerOpen &&
            feedback == PlaybackFeedback.Ready
        ) {
            delay(autoHideDelayMillis)
            controlLayer = PlayerControlLayer.Hidden
        }
    }
    LaunchedEffect(status.fallbackInProgress) {
        if (status.fallbackInProgress) {
            controlLayer = PlayerControlLayer.Controls
            requestedControlsFocus = PlayerControlFocusTarget.Progress
            actionRowVisible = false
        }
    }
    LaunchedEffect(
        controlLayer,
        requestedControlsFocus,
        definitionDrawerOpen,
        settingsDrawerOpen,
        speedDrawerOpen,
        feedback,
    ) {
        if (
            controlLayer == PlayerControlLayer.Controls &&
            feedback in setOf(
                PlaybackFeedback.Ready,
                PlaybackFeedback.Rebuffering,
                PlaybackFeedback.FallingBack,
            ) &&
            !definitionDrawerOpen &&
            !settingsDrawerOpen &&
            !speedDrawerOpen &&
            !restoreDefinitionFocus &&
            !restoreSettingsFocus &&
            !restoreSpeedFocus
        ) {
            when (requestedControlsFocus) {
                PlayerControlFocusTarget.Progress -> progressFocus.requestFocus()
                PlayerControlFocusTarget.PlayPause -> playFocus.requestFocus()
            }
        } else if (
            controlLayer != PlayerControlLayer.Controls &&
            !definitionDrawerOpen &&
            !settingsDrawerOpen &&
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
    LaunchedEffect(settingsDrawerOpen, restoreSettingsFocus) {
        if (!settingsDrawerOpen && restoreSettingsFocus) {
            withFrameNanos { }
            settingsFocus.requestFocus()
            restoreSettingsFocus = false
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
            settingsDrawerOpen = false
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
    val subtitlesFailed = PlayerExtra.Subtitles in state.extraErrors
    val danmakusFailed = PlayerExtra.Danmakus in state.extraErrors
    val danmakusAvailable =
        state.danmakus.isNotEmpty() && danmakuRuntimeAvailable && !danmakusFailed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocus)
            .focusable(
                enabled =
                    controlLayer != PlayerControlLayer.Controls &&
                        !definitionDrawerOpen &&
                        !settingsDrawerOpen &&
                        !speedDrawerOpen,
            )
            .onPreviewKeyEvent { event ->
                if (
                    controlLayer == PlayerControlLayer.Controls ||
                    definitionDrawerOpen ||
                    settingsDrawerOpen ||
                    speedDrawerOpen
                ) {
                    if (event.type == KeyEventType.KeyDown) {
                        interactionVersion += 1
                    }
                    return@onPreviewKeyEvent false
                }
                // Immersive and preview layers reserve D-pad playback shortcuts at the page root.
                val command = PlayerControlKeyPolicy.command(
                    context = when (controlLayer) {
                        PlayerControlLayer.Hidden -> PlayerControlContext.HiddenControls
                        PlayerControlLayer.Preview -> PlayerControlContext.Preview
                        PlayerControlLayer.Controls -> return@onPreviewKeyEvent false
                    },
                    key = event.key.toPlayerRemoteKey() ?: return@onPreviewKeyEvent false,
                    phase = when (event.type) {
                        KeyEventType.KeyDown -> PlayerKeyPhase.Down
                        KeyEventType.KeyUp -> PlayerKeyPhase.Up
                        else -> return@onPreviewKeyEvent false
                    },
                ) ?: return@onPreviewKeyEvent false
                val handled = when (command) {
                    PlayerControlCommand.TogglePlaybackAndShowControls -> {
                        togglePlaybackWithFeedback()
                        true
                    }

                    is PlayerControlCommand.SeekAndShowPreview -> {
                        seekCoordinator.adjustBy(
                            durationMillis = status.effectiveDurationMillis,
                            offsetMillis = command.offsetMillis,
                        )
                        true
                    }

                    PlayerControlCommand.SubmitSeekPreview -> {
                        seekCoordinator.release()
                        true
                    }

                    PlayerControlCommand.ShowPreview,
                    is PlayerControlCommand.ShowFullControls,
                    PlayerControlCommand.HideControls,
                    -> true

                    else -> false
                }
                if (handled) {
                    PlayerControlLayerPolicy.transition(command)?.let { transition ->
                        controlLayer = transition.layer
                        transition.focusTarget?.let { requestedControlsFocus = it }
                        transition.actionRowVisible?.let { actionRowVisible = it }
                    }
                    if (event.type == KeyEventType.KeyDown) {
                        interactionVersion += 1
                    }
                }
                handled
            },
    ) {
        ContentFrame(
            player = controller.player,
            modifier = Modifier.fillMaxSize(),
        )
        if (sessionSettings.selectedSubtitleTrackId != null && status.cues.isNotEmpty()) {
            AndroidView(
                factory = { context ->
                    SubtitleView(context).apply {
                        applySubtitleStyle(sessionSettings.subtitleSettings)
                    }
                },
                update = { subtitleView ->
                    subtitleView.applySubtitleStyle(sessionSettings.subtitleSettings)
                    subtitleView.setCues(status.cues)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (state.danmakus.isNotEmpty()) {
            AkDanmakuOverlay(
                player = controller.player,
                comments = state.danmakus,
                settings = sessionSettings.danmakuSettings,
                onRuntimeAvailable = { danmakuRuntimeAvailable = it },
            )
        }
        val displayedControlLayer = if (
            controlLayer != PlayerControlLayer.Hidden &&
            feedback in setOf(
                PlaybackFeedback.Ready,
                PlaybackFeedback.Rebuffering,
                PlaybackFeedback.FallingBack,
            ) &&
            !definitionDrawerOpen &&
            !settingsDrawerOpen &&
            !speedDrawerOpen &&
            !state.switchingItem
        ) {
            controlLayer
        } else {
            PlayerControlLayer.Hidden
        }
        run {
            val networkRequest = state.request as? PlaybackRequest.NetworkVideo
            val localRequest = state.request as? PlaybackRequest.LocalMedia
            val definitions = networkRequest?.source?.definitions.orEmpty()
            val playbackMode = playbackModeLabel(
                mode = localRequest?.playbackMode,
                sourceKind = status.sourceKind,
                resolution = localRequest?.transcodeResolution,
            )
            val selectedDefinitionLabel = networkRequest?.source?.let { source ->
                source.selectedDefinition?.label
                    ?: source.definitions.firstOrNull { it.url == source.url }?.label
                    ?: source.definitions.singleOrNull()?.label
            }
            val qualityControlLabel = playerQualityControlLabel(
                playbackModeLabel = playbackMode,
                selectedDefinitionLabel = selectedDefinitionLabel,
            )
            val playbackStatusLabel = selectedDefinitionLabel
                ?.takeIf(String::isNotBlank)
                ?.let { definitionLabel ->
                    stringResource(
                        R.string.playback_status_with_definition,
                        playbackMode,
                        definitionLabel,
                    )
                }
                ?: playbackMode
            val parentTitle = localRequest?.parentTitle?.takeIf(String::isNotBlank)
            val secondaryTitle = parentTitle?.let {
                localRequest.episodeNumber?.let { episodeNumber ->
                    stringResource(
                        R.string.player_episode_summary,
                        episodeNumber,
                        localRequest.title,
                    )
                } ?: localRequest.title
            }
            val controlsState = PlayerControlsUiState(
                title = parentTitle ?: state.request.title,
                secondaryTitle = secondaryTitle,
                playWhenReady = status.playWhenReady,
                positionMillis = seekState.displayPositionMillis,
                durationMillis = status.effectiveDurationMillis,
                playbackModeLabel = playbackStatusLabel,
                qualityControlLabel = qualityControlLabel,
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
                            sessionSettings.selectedSubtitleTrackId != null,
                    error = subtitlesFailed,
                ),
                danmakus = PlayerActionUiState(
                    enabled = danmakusFailed || danmakusAvailable,
                    active = danmakusAvailable && sessionSettings.danmakuSettings.enabled,
                    error = danmakusFailed,
                ),
                settings = PlayerActionUiState(
                    enabled = state.subtitles.isNotEmpty() || danmakusAvailable,
                ),
                quality = PlayerActionUiState(enabled = definitions.size > 1),
                chapters = state.mediaProbe?.chapters.orEmpty(),
            )
            AnimatedPlayerControlLayer(layer = displayedControlLayer) { renderedLayer ->
                when (renderedLayer) {
                    PlayerControlLayer.Hidden -> Unit
                    PlayerControlLayer.Preview -> PlayerInfoPreview(controlsState)
                    PlayerControlLayer.Controls -> PlayerControls(
                        state = controlsState,
                        actionRowVisible = actionRowVisible,
                        onActionRowVisibilityChange = { actionRowVisible = it },
                        progressFocus = progressFocus,
                        definitionFocus = definitionFocus,
                        settingsFocus = settingsFocus,
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
                            seekCoordinator.stepBy(
                                durationMillis = status.effectiveDurationMillis,
                                offsetMillis = -10_000L,
                            )
                        },
                        onPlayPause = {
                            interactionVersion += 1
                            togglePlaybackWithFeedback()
                        },
                        onForward = {
                            interactionVersion += 1
                            seekCoordinator.stepBy(
                                durationMillis = status.effectiveDurationMillis,
                                offsetMillis = 10_000L,
                            )
                        },
                        onNext = {
                            interactionVersion += 1
                            controller.recordItemSwitchProgress()
                            onNext()
                        },
                        onToggleSubtitles = {
                            interactionVersion += 1
                            requestSessionState = requestSessionState.copy(
                                sessionSettings = PlayerSessionSettingsPolicy.toggleSubtitles(
                                    state = requestSessionState.sessionSettings,
                                    tracks = state.subtitles,
                                ),
                            )
                        },
                        onOpenSpeed = {
                            interactionVersion += 1
                            speedDrawerOpen = true
                            settingsDrawerOpen = false
                            definitionDrawerOpen = false
                        },
                        onToggleDanmakus = {
                            interactionVersion += 1
                            requestSessionState = requestSessionState.copy(
                                sessionSettings = PlayerSessionSettingsPolicy.toggleDanmakus(
                                    requestSessionState.sessionSettings,
                                ),
                            )
                        },
                        onOpenSettings = {
                            interactionVersion += 1
                            settingsDrawerOpen = true
                            definitionDrawerOpen = false
                            speedDrawerOpen = false
                        },
                        onOpenDefinitions = {
                            interactionVersion += 1
                            definitionDrawerOpen = true
                            settingsDrawerOpen = false
                            speedDrawerOpen = false
                        },
                        onSeekPreviewBy = { offsetMillis ->
                            seekCoordinator.adjustBy(
                                durationMillis = status.effectiveDurationMillis,
                                offsetMillis = offsetMillis,
                            )
                        },
                        onSeekPreviewFinished = seekCoordinator::release,
                        onHideControls = {
                            controlLayer = PlayerControlLayer.Hidden
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
            }
        }
        PlayerPlaybackToggleFeedback(
            event = playbackToggleFeedback,
            onFinished = { finishedId ->
                if (playbackToggleFeedback?.id == finishedId) {
                    playbackToggleFeedback = null
                }
            },
        )
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
        if (settingsDrawerOpen) {
            PlayerSettingsDrawer(
                subtitleTracks = state.subtitles,
                selectedSubtitleTrackId = sessionSettings.rememberedSubtitleTrackId,
                subtitleSettings = sessionSettings.subtitleSettings,
                danmakuSettings = sessionSettings.danmakuSettings.takeIf {
                    danmakusAvailable
                },
                onSelectSubtitleTrack = { trackId ->
                    requestSessionState = requestSessionState.copy(
                        sessionSettings = PlayerSessionSettingsPolicy.selectSubtitleTrack(
                            state = requestSessionState.sessionSettings,
                            tracks = state.subtitles,
                            trackId = trackId,
                        ),
                    )
                },
                onChangeSubtitleSettings = { value ->
                    requestSessionState = requestSessionState.copy(
                        sessionSettings = requestSessionState.sessionSettings.copy(
                            subtitleSettings = value.copy(
                                enabled = requestSessionState.sessionSettings
                                    .selectedSubtitleTrackId != null,
                            ),
                        ),
                    )
                },
                onChangeDanmakuSettings = { value ->
                    requestSessionState = requestSessionState.copy(
                        sessionSettings = requestSessionState.sessionSettings.copy(
                            danmakuSettings = value.copy(
                                enabled = requestSessionState.sessionSettings
                                    .danmakuSettings.enabled,
                            ),
                        ),
                    )
                },
                onDismiss = {
                    settingsDrawerOpen = false
                    restoreSettingsFocus = true
                },
            )
        }
        if (speedDrawerOpen) {
            PlayerSpeedDrawer(
                speed = playbackSpeed,
                onSelect = { selectedSpeed ->
                    requestSessionState = requestSessionState.copy(
                        playbackSpeed = selectedSpeed,
                    )
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
            feedback = if (
                playbackToggleFeedback != null &&
                feedback == PlaybackFeedback.Rebuffering
            ) {
                PlaybackFeedback.Ready
            } else {
                feedback
            },
            failure = status.failure,
            sourceKind = status.sourceKind,
            onRetry = {
                controlLayer = PlayerControlLayer.Controls
                requestedControlsFocus = PlayerControlFocusTarget.Progress
                actionRowVisible = false
                interactionVersion += 1
                controller.retry()
            },
        )
    }
}
