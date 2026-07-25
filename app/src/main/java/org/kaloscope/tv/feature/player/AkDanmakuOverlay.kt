package org.kaloscope.tv.feature.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.kuaishou.akdanmaku.ui.DanmakuView
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.DanmakuSettings

@Composable
internal fun AkDanmakuOverlay(
    player: Player,
    comments: List<DanmakuComment>,
    settings: DanmakuSettings,
    onRuntimeAvailable: (Boolean) -> Unit = {},
) {
    val runtimeResult = remember(player) {
        runCatching { AkDanmakuRuntime() }
    }
    val runtime = runtimeResult.getOrNull()
    val synchronizer = remember(runtime) {
        runtime?.let(::DanmakuPlaybackSynchronizer)
    }
    var runtimeBound by remember(player, comments) { mutableStateOf(false) }
    var setupFailed by remember(player, comments) {
        mutableStateOf(runtimeResult.isFailure)
    }

    LaunchedEffect(runtimeBound, setupFailed) {
        onRuntimeAvailable(runtimeBound && !setupFailed)
    }
    LaunchedEffect(synchronizer, runtimeBound, settings) {
        if (runtimeBound) {
            synchronizer?.onSettingsChanged(settings)
        }
    }
    DisposableEffect(player, synchronizer, runtimeBound) {
        if (!runtimeBound || synchronizer == null) {
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    synchronizer.onIsPlayingChanged(
                        isPlaying = isPlaying,
                        positionMillis = player.currentPosition,
                        playbackSpeed = player.playbackParameters.speed,
                    )
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    synchronizer.onPositionDiscontinuity(player.currentPosition)
                }

                override fun onPlaybackParametersChanged(
                    playbackParameters: PlaybackParameters,
                ) {
                    synchronizer.onPlaybackSpeedChanged(playbackParameters.speed)
                }
            }
            player.addListener(listener)
            synchronizer.onIsPlayingChanged(
                isPlaying = player.isPlaying,
                positionMillis = player.currentPosition,
                playbackSpeed = player.playbackParameters.speed,
            )
            onDispose {
                player.removeListener(listener)
                synchronizer.dispose()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, bottom = 128.dp)
            .testTag("ak-danmaku-overlay"),
    ) {
        if (runtime != null) {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        runCatching {
                            val danmakuView = DanmakuView(context)
                            addView(
                                danmakuView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                ),
                            )
                            runtime.bind(danmakuView)
                            runtime.load(comments)
                        }.onSuccess {
                            runtimeBound = true
                        }.onFailure {
                            setupFailed = true
                            removeAllViews()
                            runtime.release()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { container ->
                    // Detach the view before stopping AkDanmaku's handler thread.
                    container.removeAllViews()
                    runtime.release()
                },
            )
        }
    }
}
