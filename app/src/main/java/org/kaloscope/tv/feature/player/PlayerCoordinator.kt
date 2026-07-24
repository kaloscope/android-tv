package org.kaloscope.tv.feature.player

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.data.media.MediaRepository

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    data object MissingRequest : PlayerUiState

    data class Content(
        val request: PlaybackRequest.LocalMedia,
        val subtitles: List<SubtitleTrack>,
        val danmakus: List<DanmakuComment>,
        val extraFailures: Map<PlayerExtra, AppError>,
        val progressError: AppError? = null,
    ) : PlayerUiState {
        val extraErrors: Set<PlayerExtra>
            get() = extraFailures.keys
    }
}

enum class PlayerExtra {
    Subtitles,
    Danmakus,
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
        val request = requestStore.get(requestId) as? PlaybackRequest.LocalMedia
        if (request == null || request.serverId != session.server.id) {
            mutableState.value = PlayerUiState.MissingRequest
            return
        }
        // Supplementary endpoints run together so neither doubles playback startup latency.
        val (subtitleResult, danmakuResult) = coroutineScope {
            val subtitles = async {
                mediaRepository.getSubtitleTracks(session, request.path)
            }
            val danmakus = async {
                mediaRepository.getDanmakus(session, request.path)
            }
            subtitles.await() to danmakus.await()
        }
        val failures = buildMap {
            if (subtitleResult is AppResult.Failure) {
                put(PlayerExtra.Subtitles, subtitleResult.error)
            }
            if (danmakuResult is AppResult.Failure) {
                put(PlayerExtra.Danmakus, danmakuResult.error)
            }
        }
        mutableState.value = PlayerUiState.Content(
            request = request,
            subtitles = (subtitleResult as? AppResult.Success)?.value.orEmpty(),
            danmakus = (danmakuResult as? AppResult.Success)?.value.orEmpty(),
            extraFailures = failures,
        )
    }

    fun reportProgressFailure(error: AppError) {
        val content = mutableState.value as? PlayerUiState.Content ?: return
        mutableState.value = content.copy(progressError = error)
    }
}
