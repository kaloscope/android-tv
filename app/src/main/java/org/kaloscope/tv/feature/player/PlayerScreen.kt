package org.kaloscope.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.NetworkDefinition
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.player.DanmakuFrame
import org.kaloscope.tv.core.player.DanmakuMode
import org.kaloscope.tv.core.player.DanmakuTimeline
import org.kaloscope.tv.core.player.PlaybackController
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackFailure
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestNavigator
import org.kaloscope.tv.core.player.PlaybackSettingsPolicy
import org.kaloscope.tv.core.player.PlaybackSourceKind
import org.kaloscope.tv.core.player.ProgressReason
import org.kaloscope.tv.core.player.TranscodeResolution
import kotlin.math.roundToInt

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
    var danmakusEnabled by remember(state.request.requestId) {
        mutableStateOf(state.request.danmakuEnabled)
    }
    var definitionDrawerOpen by remember { mutableStateOf(false) }
    var restoreDefinitionFocus by remember { mutableStateOf(false) }
    var interactionVersion by remember { mutableLongStateOf(0) }
    val playerFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val definitionFocus = remember { FocusRequester() }
    val hasNext = PlaybackRequestNavigator.hasNext(state.request)

    BackHandler {
        if (definitionDrawerOpen) {
            definitionDrawerOpen = false
            restoreDefinitionFocus = true
        } else {
            onBack()
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
    ) {
        if (
            controlsVisible &&
            !definitionDrawerOpen &&
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
    LaunchedEffect(controlsVisible, definitionDrawerOpen) {
        if (controlsVisible && !definitionDrawerOpen && !restoreDefinitionFocus) {
            playFocus.requestFocus()
        } else if (!controlsVisible && !definitionDrawerOpen) {
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
    LaunchedEffect(status.failure) {
        if (status.failure != null) {
            definitionDrawerOpen = false
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
            .focusable(enabled = !controlsVisible && !definitionDrawerOpen)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                if (controlsVisible || definitionDrawerOpen) {
                    interactionVersion += 1
                    return@onPreviewKeyEvent false
                }
                // Hidden controls reserve D-pad playback shortcuts at the page root.
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        controller.togglePlayPause()
                        controlsVisible = true
                        true
                    }

                    Key.DirectionLeft -> {
                        controller.seekBy(-10_000)
                        controlsVisible = true
                        true
                    }

                    Key.DirectionRight -> {
                        controller.seekBy(10_000)
                        controlsVisible = true
                        true
                    }

                    Key.DirectionUp, Key.DirectionDown -> {
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
        if (danmakusEnabled) {
            DanmakuOverlay(
                comments = state.danmakus,
                positionMillis = positionMillis,
                isPlaying = status.isPlaying,
            )
        }
        if (
            controlsVisible &&
            status.failure == null &&
            !definitionDrawerOpen &&
            !state.switchingItem
        ) {
            PlayerControls(
                title = state.request.title,
                isPlaying = status.isPlaying,
                positionMillis = positionMillis,
                durationMillis = controller.player.duration,
                subtitlesAvailable = state.subtitles.isNotEmpty(),
                subtitlesEnabled = subtitlesEnabled,
                danmakusAvailable = state.danmakus.isNotEmpty(),
                danmakusEnabled = danmakusEnabled,
                extraErrors = state.extraErrors,
                progressSaveFailed = state.progressError != null,
                playbackMode = (state.request as? PlaybackRequest.LocalMedia)?.playbackMode,
                sourceKind = status.sourceKind,
                transcodeResolution =
                    (state.request as? PlaybackRequest.LocalMedia)?.transcodeResolution,
                fallbackInProgress = status.fallbackInProgress,
                hasPrevious = PlaybackRequestNavigator.hasPrevious(state.request),
                hasNext = hasNext,
                definitions = (state.request as? PlaybackRequest.NetworkVideo)
                    ?.source
                    ?.definitions
                    .orEmpty(),
                definitionFocus = definitionFocus,
                switchingItem = state.switchingItem,
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
                    danmakusEnabled = !danmakusEnabled
                },
                onOpenDefinitions = {
                    interactionVersion += 1
                    definitionDrawerOpen = true
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
internal fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    positionMillis: Long,
    durationMillis: Long,
    subtitlesAvailable: Boolean,
    subtitlesEnabled: Boolean,
    danmakusAvailable: Boolean,
    danmakusEnabled: Boolean,
    extraErrors: Set<PlayerExtra>,
    progressSaveFailed: Boolean,
    playbackMode: PlaybackMode?,
    sourceKind: PlaybackSourceKind,
    transcodeResolution: TranscodeResolution?,
    fallbackInProgress: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    definitions: List<NetworkDefinition>,
    switchingItem: Boolean,
    playFocus: FocusRequester,
    definitionFocus: FocusRequester,
    onPrevious: () -> Unit,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleDanmakus: () -> Unit,
    onOpenDefinitions: () -> Unit,
) {
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
                text = title,
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = playbackModeLabel(
                        mode = playbackMode,
                        sourceKind = sourceKind,
                        resolution = transcodeResolution,
                    ),
                    color = OnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (fallbackInProgress) {
                    Text(
                        text = stringResource(R.string.switching_to_transcode),
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        if (progressSaveFailed) {
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
                text = formatDuration(positionMillis),
                color = OnBackground,
                fontSize = 14.sp,
            )
            Spacer(Modifier.width(14.dp))
            PlayerProgress(
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = formatDuration(durationMillis),
                color = OnBackground,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasPrevious) {
                PlayerButton(
                    text = stringResource(R.string.previous_episode),
                    onClick = onPrevious,
                    enabled = !switchingItem,
                )
            }
            PlayerButton(
                text = stringResource(R.string.rewind_seconds),
                onClick = onRewind,
            )
            PlayerButton(
                text = if (isPlaying) {
                    stringResource(R.string.pause)
                } else {
                    stringResource(R.string.play)
                },
                onClick = onPlayPause,
                modifier = Modifier.focusRequester(playFocus),
                primary = true,
            )
            PlayerButton(
                text = stringResource(R.string.forward_seconds),
                onClick = onForward,
            )
            if (hasNext) {
                PlayerButton(
                    text = stringResource(R.string.next_episode),
                    onClick = onNext,
                    enabled = !switchingItem,
                )
            }
            Spacer(Modifier.weight(1f))
            PlayerButton(
                text = subtitleButtonText(
                    available = subtitlesAvailable,
                    enabled = subtitlesEnabled,
                    failed = PlayerExtra.Subtitles in extraErrors,
                ),
                onClick = onToggleSubtitles,
                enabled = subtitlesAvailable,
                active = subtitlesEnabled,
            )
            PlayerButton(
                text = danmakuButtonText(
                    available = danmakusAvailable,
                    enabled = danmakusEnabled,
                    failed = PlayerExtra.Danmakus in extraErrors,
                ),
                onClick = onToggleDanmakus,
                enabled = danmakusAvailable,
                active = danmakusEnabled,
            )
            if (definitions.isNotEmpty()) {
                PlayerButton(
                    text = stringResource(R.string.playback_quality),
                    onClick = onOpenDefinitions,
                    modifier = Modifier.focusRequester(definitionFocus),
                )
            }
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
private fun PlayerProgress(
    positionMillis: Long,
    durationMillis: Long,
    modifier: Modifier = Modifier,
) {
    val progress = if (durationMillis > 0) {
        (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        modifier = modifier
            .height(6.dp)
            .background(Color(0xFF4A5060), RoundedCornerShape(6.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(Primary, RoundedCornerShape(6.dp)),
        )
    }
}

@Composable
internal fun DanmakuOverlay(
    comments: List<DanmakuComment>,
    positionMillis: Long,
    isPlaying: Boolean,
) {
    val timeline = remember(comments) {
        DanmakuTimeline.create(comments)
    }
    var renderPositionMillis by remember(timeline) {
        mutableLongStateOf(positionMillis)
    }
    LaunchedEffect(timeline, positionMillis, isPlaying) {
        renderPositionMillis = positionMillis
        if (!isPlaying) {
            return@LaunchedEffect
        }
        val anchorNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameNanos ->
                renderPositionMillis = positionMillis +
                    ((frameNanos - anchorNanos) / 1_000_000)
            }
        }
    }
    val frames = timeline.framesAt(renderPositionMillis)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, bottom = 128.dp)
            .clipToBounds(),
    ) {
        val containerWidthPixels = constraints.maxWidth
        frames.forEach { frame ->
            key(
                frame.comment.id,
                frame.comment.startMillis,
                frame.mode,
                frame.lane,
            ) {
                DanmakuText(
                    frame = frame,
                    containerWidthPixels = containerWidthPixels,
                    modifier = Modifier.align(
                        when (frame.mode) {
                            DanmakuMode.Scroll, DanmakuMode.Top -> Alignment.TopStart
                            DanmakuMode.Bottom -> Alignment.BottomStart
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun DanmakuText(
    frame: DanmakuFrame,
    containerWidthPixels: Int,
    modifier: Modifier = Modifier,
) {
    var textWidthPixels by remember(
        frame.comment.id,
        frame.comment.startMillis,
        frame.mode,
        frame.lane,
    ) {
        mutableIntStateOf(0)
    }
    val laneOffset = 36.dp * frame.lane
    val horizontalOffset = when (frame.mode) {
        DanmakuMode.Scroll -> (
            containerWidthPixels -
                (containerWidthPixels + textWidthPixels) * frame.progress
            ).roundToInt()

        DanmakuMode.Top, DanmakuMode.Bottom ->
            ((containerWidthPixels - textWidthPixels) / 2).coerceAtLeast(0)
    }
    val verticalOffset = when (frame.mode) {
        DanmakuMode.Scroll -> laneOffset + 76.dp
        DanmakuMode.Top -> laneOffset
        DanmakuMode.Bottom -> -laneOffset
    }
    Text(
        text = frame.comment.text,
        color = parseDanmakuColor(frame.comment.color),
        maxLines = 1,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black,
                offset = Offset(1.5f, 1.5f),
                blurRadius = 3f,
            ),
        ),
        modifier = modifier
            .offset {
                IntOffset(
                    x = horizontalOffset,
                    y = verticalOffset.roundToPx(),
                )
            }
            .onSizeChanged { textWidthPixels = it.width }
            .testTag(
                "danmaku-comment-" +
                    (frame.comment.id ?: frame.comment.startMillis.toString()),
            ),
    )
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

@Composable
private fun subtitleButtonText(
    available: Boolean,
    enabled: Boolean,
    failed: Boolean,
): String =
    when {
        failed -> stringResource(R.string.subtitle_unavailable)
        !available -> stringResource(R.string.no_subtitles)
        enabled -> stringResource(R.string.subtitles_on)
        else -> stringResource(R.string.subtitles_off)
    }

@Composable
private fun danmakuButtonText(
    available: Boolean,
    enabled: Boolean,
    failed: Boolean,
): String =
    when {
        failed -> stringResource(R.string.danmaku_unavailable)
        !available -> stringResource(R.string.no_danmaku)
        enabled -> stringResource(R.string.danmaku_on)
        else -> stringResource(R.string.danmaku_off)
    }

private fun formatDuration(milliseconds: Long): String {
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

private fun parseDanmakuColor(rawColor: String?): Color =
    runCatching {
        Color(android.graphics.Color.parseColor(rawColor ?: "#FFFFFF"))
    }.getOrDefault(Color.White)

private fun PlaybackRequest.playbackIdentity(): String =
    when (this) {
        is PlaybackRequest.LocalMedia -> "$requestId:local:$mediaId"
        is PlaybackRequest.NetworkVideo ->
            "$requestId:network:${source.resourceId}:${source.selectedChapterIndex}:${source.url}"
    }
