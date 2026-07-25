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
import org.kaloscope.tv.core.player.PlaybackController
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackBufferingPolicy
import org.kaloscope.tv.core.player.PlaybackFailure
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestNavigator
import org.kaloscope.tv.core.player.PlaybackSettingsPolicy
import org.kaloscope.tv.core.player.PlaybackSourceKind
import org.kaloscope.tv.core.player.ProgressReason
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
    var positionMillis by remember(playbackIdentity) { mutableLongStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    var subtitlesEnabled by remember(state.request.requestId) {
        mutableStateOf(state.request.subtitleEnabled)
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
    var interactionVersion by remember { mutableLongStateOf(0) }
    val playerFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val definitionFocus = remember { FocusRequester() }
    val danmakuSettingsFocus = remember { FocusRequester() }
    val hasNext = PlaybackRequestNavigator.hasNext(state.request)

    BackHandler {
        val context = when {
            danmakuDrawerOpen -> PlayerBackContext.DanmakuDrawer
            definitionDrawerOpen -> PlayerBackContext.DefinitionDrawer
            controlsVisible && status.failure == null && !state.switchingItem ->
                PlayerBackContext.Controls

            else -> PlayerBackContext.Player
        }
        when (PlayerControlKeyPolicy.backCommand(context)) {
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

    LaunchedEffect(controller) {
        controller.setSubtitlesEnabled(subtitlesEnabled)
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
    ) {
        if (
            controlsVisible &&
            !definitionDrawerOpen &&
            !danmakuDrawerOpen &&
            status.failure == null &&
            !status.fallbackInProgress
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
    LaunchedEffect(controlsVisible, definitionDrawerOpen, danmakuDrawerOpen) {
        if (
            controlsVisible &&
            !definitionDrawerOpen &&
            !danmakuDrawerOpen &&
            !restoreDefinitionFocus &&
            !restoreDanmakuSettingsFocus
        ) {
            playFocus.requestFocus()
        } else if (!controlsVisible && !definitionDrawerOpen && !danmakuDrawerOpen) {
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
    LaunchedEffect(status.failure) {
        if (status.failure != null) {
            definitionDrawerOpen = false
            danmakuDrawerOpen = false
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
                enabled = !controlsVisible && !definitionDrawerOpen && !danmakuDrawerOpen,
            )
            .onPreviewKeyEvent { event ->
                if (controlsVisible || definitionDrawerOpen || danmakuDrawerOpen) {
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
        if (subtitlesEnabled && status.cues.isNotEmpty()) {
            AndroidView(
                factory = { context ->
                    SubtitleView(context).apply {
                        setUserDefaultStyle()
                        setUserDefaultTextSize()
                    }
                },
                update = { subtitleView ->
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
            status.failure == null &&
            !definitionDrawerOpen &&
            !danmakuDrawerOpen &&
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
                    durationMillis = controller.player.duration,
                    playbackModeLabel = playbackModeLabel(
                        mode = (state.request as? PlaybackRequest.LocalMedia)?.playbackMode,
                        sourceKind = status.sourceKind,
                        resolution =
                            (state.request as? PlaybackRequest.LocalMedia)?.transcodeResolution,
                    ),
                    fallbackInProgress = status.fallbackInProgress,
                    progressSaveFailed = state.progressError != null,
                    previousEnabled =
                        PlaybackRequestNavigator.hasPrevious(state.request) &&
                            !state.switchingItem,
                    nextEnabled = hasNext && !state.switchingItem,
                    subtitles = PlayerActionUiState(
                        enabled = state.subtitles.isNotEmpty() && !subtitlesFailed,
                        active =
                            state.subtitles.isNotEmpty() &&
                                !subtitlesFailed &&
                                subtitlesEnabled,
                        error = subtitlesFailed,
                    ),
                    danmakus = PlayerActionUiState(
                        enabled = danmakusAvailable,
                        active = danmakusAvailable && sessionDanmakuSettings.enabled,
                        error = danmakusFailed,
                    ),
                    danmakuSettings = PlayerActionUiState(
                        enabled = danmakusAvailable,
                        error = danmakusFailed,
                    ),
                    quality = PlayerActionUiState(enabled = definitions.size > 1),
                ),
                definitionFocus = definitionFocus,
                danmakuSettingsFocus = danmakuSettingsFocus,
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
                onToggleSubtitles = {
                    interactionVersion += 1
                    subtitlesEnabled = !subtitlesEnabled
                    controller.setSubtitlesEnabled(subtitlesEnabled)
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
                },
                onOpenDefinitions = {
                    interactionVersion += 1
                    definitionDrawerOpen = true
                    danmakuDrawerOpen = false
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
            )
        }
        PlayerBufferingIndicator(
            isRebuffering =
                status.failure == null &&
                    !status.fallbackInProgress &&
                    PlaybackBufferingPolicy.isRebuffering(
                        hasBeenReady = status.hasBeenReady,
                        playbackState = status.playbackState,
                    ),
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
        if (state.switchingItem) {
            PlayerBusyOverlay(stringResource(R.string.switching_episode))
        } else if (state.switchError != null) {
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
        status.failure?.let { failure ->
            PlaybackErrorOverlay(
                failure = failure,
                sourceKind = status.sourceKind,
                onRetry = {
                    controlsVisible = true
                    interactionVersion += 1
                    controller.retry()
                },
            )
        }
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
private fun PlayerBusyOverlay(message: String) {
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
private fun PlaybackErrorOverlay(
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
                text = playbackErrorText(failure, sourceKind),
                color = Danger,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.focusRequester(retryFocus),
                colors = ButtonDefaults.colors(focusedContainerColor = Primary),
            ) {
                Text(stringResource(R.string.retry))
            }
        }
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
private fun playbackErrorText(
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
