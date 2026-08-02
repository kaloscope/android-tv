package org.kaloscope.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import org.kaloscope.tv.core.player.SubtitleSelectionPolicy

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
    val status by controller.status.collectAsStateWithLifecycle()
    val feedback = PlaybackFeedbackPolicy.resolve(
        playbackState = status.playbackState,
        hasBeenReady = status.hasBeenReady,
        fallbackInProgress = status.fallbackInProgress,
        switchingItem = state.switchingItem,
        failure = status.failure,
        playWhenReady = status.playWhenReady,
    )
    var positionMillis by remember(playbackIdentity) { mutableLongStateOf(0) }
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
    var playbackToggleFeedbackId by remember(playbackIdentity) { mutableLongStateOf(0) }
    var playbackToggleFeedback by remember(playbackIdentity) {
        mutableStateOf<PlayerPlaybackToggleEvent?>(null)
    }
    val playerFocus = remember { FocusRequester() }
    val progressFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val definitionFocus = remember { FocusRequester() }
    val danmakuSettingsFocus = remember { FocusRequester() }
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
            subtitleDrawerOpen -> PlayerBackContext.SubtitleDrawer
            speedDrawerOpen -> PlayerBackContext.SpeedDrawer
            danmakuDrawerOpen -> PlayerBackContext.DanmakuDrawer
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

            PlayerControlCommand.HideControls -> controlLayer = PlayerControlLayer.Hidden
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
        controlLayer,
        interactionVersion,
        status.failure,
        status.fallbackInProgress,
        definitionDrawerOpen,
        danmakuDrawerOpen,
        subtitleDrawerOpen,
        speedDrawerOpen,
        feedback,
    ) {
        val autoHideDelayMillis = PlayerControlLayerPolicy.autoHideDelayMillis(controlLayer)
        if (
            autoHideDelayMillis != null &&
            !definitionDrawerOpen &&
            !danmakuDrawerOpen &&
            !subtitleDrawerOpen &&
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
        danmakuDrawerOpen,
        subtitleDrawerOpen,
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
            !danmakuDrawerOpen &&
            !subtitleDrawerOpen &&
            !speedDrawerOpen &&
            !restoreDefinitionFocus &&
            !restoreDanmakuSettingsFocus &&
            !restoreSubtitleFocus &&
            !restoreSpeedFocus
        ) {
            when (requestedControlsFocus) {
                PlayerControlFocusTarget.Progress -> progressFocus.requestFocus()
                PlayerControlFocusTarget.PlayPause -> playFocus.requestFocus()
            }
        } else if (
            controlLayer != PlayerControlLayer.Controls &&
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
                    controlLayer != PlayerControlLayer.Controls &&
                        !definitionDrawerOpen &&
                        !danmakuDrawerOpen &&
                        !subtitleDrawerOpen &&
                        !speedDrawerOpen,
            )
            .onPreviewKeyEvent { event ->
                if (
                    controlLayer == PlayerControlLayer.Controls ||
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
                        controller.seekBy(command.offsetMillis)
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
        val displayedControlLayer = if (
            controlLayer != PlayerControlLayer.Hidden &&
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
            val qualityLabel = selectedDefinitionLabel
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
            val subtitlesFailed = PlayerExtra.Subtitles in state.extraErrors
            val danmakusFailed = PlayerExtra.Danmakus in state.extraErrors
            val danmakusAvailable =
                state.danmakus.isNotEmpty() && danmakuRuntimeAvailable && !danmakusFailed
            val controlsState = PlayerControlsUiState(
                title = parentTitle ?: state.request.title,
                secondaryTitle = secondaryTitle,
                playWhenReady = status.playWhenReady,
                positionMillis = positionMillis,
                durationMillis = status.effectiveDurationMillis,
                playbackModeLabel = qualityLabel,
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
                            togglePlaybackWithFeedback()
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
