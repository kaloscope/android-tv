package org.kaloscope.tv.feature.player

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.MediaProbe
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.data.media.MediaRepository

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    data object MissingRequest : PlayerUiState

    data class Content(
        val request: PlaybackRequest,
        val subtitles: List<SubtitleTrack>,
        val danmakus: List<DanmakuComment>,
        val mediaProbe: MediaProbe? = null,
        val extraFailures: Map<PlayerExtra, AppError>,
        val progressError: AppError? = null,
        val switchingItem: Boolean = false,
        val switchError: AppError? = null,
    ) : PlayerUiState {
        val extraErrors: Set<PlayerExtra>
            get() = extraFailures.keys
    }
}

enum class PlayerExtra {
    Subtitles,
    Danmakus,
    MediaProbe,
}

class PlayerCoordinator(
    private val requestStore: PlaybackRequestStore,
    private val mediaRepository: MediaRepository,
) {
    private val mutableState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)

    val state: StateFlow<PlayerUiState> = mutableState.asStateFlow()

    suspend fun load(
        session: Session,
        requestId: String,
    ) {
        mutableState.value = PlayerUiState.Loading
        val request = requestStore.get(requestId)
        if (request == null || request.serverId != session.server.id) {
            mutableState.value = PlayerUiState.MissingRequest
            return
        }
        if (request is PlaybackRequest.NetworkVideo) {
            mutableState.value = PlayerUiState.Content(
                request = request,
                subtitles = emptyList(),
                danmakus = request.source.danmakus,
                extraFailures = emptyMap(),
            )
            return
        }
        val localRequest = request as PlaybackRequest.LocalMedia
        val extras = loadLocalExtras(session, localRequest.path)
        mutableState.value = PlayerUiState.Content(
            request = request,
            subtitles = extras.subtitles,
            danmakus = extras.danmakus,
            mediaProbe = extras.mediaProbe,
            extraFailures = extras.failures,
        )
    }

    fun beginItemSwitch() {
        val content = mutableState.value as? PlayerUiState.Content ?: return
        mutableState.value = content.copy(switchingItem = true, switchError = null)
    }

    suspend fun replaceRequest(
        session: Session,
        request: PlaybackRequest,
    ) {
        requestStore.put(request)
        val current = mutableState.value as? PlayerUiState.Content
        if (request is PlaybackRequest.NetworkVideo) {
            mutableState.value = PlayerUiState.Content(
                request = request,
                subtitles = emptyList(),
                danmakus = request.source.danmakus,
                extraFailures = emptyMap(),
                progressError = current?.progressError,
            )
            return
        }
        val localRequest = request as PlaybackRequest.LocalMedia
        val extras = loadLocalExtras(session, localRequest.path)
        mutableState.value = PlayerUiState.Content(
            request = request,
            subtitles = extras.subtitles,
            danmakus = extras.danmakus,
            mediaProbe = extras.mediaProbe,
            extraFailures = extras.failures,
            progressError = current?.progressError,
        )
    }

    private suspend fun loadLocalExtras(
        session: Session,
        path: String,
    ): LocalExtras {
        // Independent supplementary requests share startup latency and degrade separately.
        val (subtitleResult, danmakuResult, probeResult) = coroutineScope {
            val subtitles = async { mediaRepository.getSubtitleTracks(session, path) }
            val danmakus = async { mediaRepository.getDanmakus(session, path) }
            val probe = async { mediaRepository.getMediaProbe(session, path) }
            Triple(subtitles.await(), danmakus.await(), probe.await())
        }
        return LocalExtras(
            subtitles = (subtitleResult as? AppResult.Success)?.value.orEmpty(),
            danmakus = (danmakuResult as? AppResult.Success)?.value.orEmpty(),
            mediaProbe = (probeResult as? AppResult.Success)?.value,
            failures = buildMap {
                if (subtitleResult is AppResult.Failure) {
                    put(PlayerExtra.Subtitles, subtitleResult.error)
                }
                if (danmakuResult is AppResult.Failure) {
                    put(PlayerExtra.Danmakus, danmakuResult.error)
                }
                if (probeResult is AppResult.Failure) {
                    put(PlayerExtra.MediaProbe, probeResult.error)
                }
            },
        )
    }

    fun reportItemSwitchFailure(error: AppError) {
        val content = mutableState.value as? PlayerUiState.Content ?: return
        mutableState.value = content.copy(switchingItem = false, switchError = error)
    }

    fun reportProgressFailure(error: AppError) {
        val content = mutableState.value as? PlayerUiState.Content ?: return
        mutableState.value = content.copy(progressError = error)
    }
}

private data class LocalExtras(
    val subtitles: List<SubtitleTrack>,
    val danmakus: List<DanmakuComment>,
    val mediaProbe: MediaProbe?,
    val failures: Map<PlayerExtra, AppError>,
)
