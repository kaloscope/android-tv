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
import org.kaloscope.tv.core.player.PlaybackPreparationStage
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.data.media.MediaRepository

sealed interface PlayerUiState {
    data class Loading(
        val stage: PlaybackPreparationStage = PlaybackPreparationStage.Resource,
    ) : PlayerUiState

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
    private val mutableState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading())

    val state: StateFlow<PlayerUiState> = mutableState.asStateFlow()

    fun beginLoad() {
        mutableState.value = PlayerUiState.Loading()
    }

    suspend fun load(
        session: Session,
        requestId: String,
    ) {
        beginLoad()
        val request = requestStore.get(requestId)
        if (request == null || request.serverId != session.server.id) {
            mutableState.value = PlayerUiState.MissingRequest
            return
        }
        mutableState.value = buildContent(
            session = session,
            request = request,
            onPreparationStage = { stage ->
                mutableState.value = PlayerUiState.Loading(stage)
            },
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
        val progressError = (mutableState.value as? PlayerUiState.Content)?.progressError
        mutableState.value = buildContent(session, request, progressError)
    }

    private suspend fun buildContent(
        session: Session,
        request: PlaybackRequest,
        progressError: AppError? = null,
        onPreparationStage: (PlaybackPreparationStage) -> Unit = {},
    ): PlayerUiState.Content =
        when (request) {
            is PlaybackRequest.NetworkVideo -> PlayerUiState.Content(
                request = request,
                subtitles = emptyList(),
                danmakus = request.source.danmakus,
                extraFailures = emptyMap(),
                progressError = progressError,
            )

            is PlaybackRequest.LocalMedia -> loadLocalContent(
                session = session,
                request = request,
                progressError = progressError,
                onPreparationStage = onPreparationStage,
            )
        }

    private suspend fun loadLocalContent(
        session: Session,
        request: PlaybackRequest.LocalMedia,
        progressError: AppError?,
        onPreparationStage: (PlaybackPreparationStage) -> Unit,
    ): PlayerUiState.Content {
        // Independent supplementary requests share startup latency and degrade separately.
        val (subtitleResult, danmakuResult, probeResult) = coroutineScope {
            val subtitles = async { mediaRepository.getSubtitleTracks(session, request.path) }
            val danmakus = async { mediaRepository.getDanmakus(session, request.path) }
            val probe = async { mediaRepository.getMediaProbe(session, request.path) }
            val subtitleResult = subtitles.await()
            val probeResult = probe.await()
            if (!danmakus.isCompleted) {
                onPreparationStage(PlaybackPreparationStage.Danmaku)
            }
            Triple(subtitleResult, danmakus.await(), probeResult)
        }
        return PlayerUiState.Content(
            request = request,
            subtitles = (subtitleResult as? AppResult.Success)?.value.orEmpty(),
            danmakus = (danmakuResult as? AppResult.Success)?.value.orEmpty(),
            mediaProbe = (probeResult as? AppResult.Success)?.value,
            extraFailures = buildMap {
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
            progressError = progressError,
        )
    }

    suspend fun retryExtra(
        session: Session,
        extra: PlayerExtra,
    ) {
        val original = mutableState.value as? PlayerUiState.Content ?: return
        val request = original.request as? PlaybackRequest.LocalMedia ?: return
        when (extra) {
            PlayerExtra.Subtitles -> applyExtraResult(
                request,
                extra,
                mediaRepository.getSubtitleTracks(session, request.path),
            ) { subtitles ->
                copy(subtitles = subtitles, extraFailures = extraFailures - extra)
            }

            PlayerExtra.Danmakus -> applyExtraResult(
                request,
                extra,
                mediaRepository.getDanmakus(session, request.path),
            ) { danmakus ->
                copy(danmakus = danmakus, extraFailures = extraFailures - extra)
            }

            PlayerExtra.MediaProbe -> Unit
        }
    }

    private fun <T> applyExtraResult(
        request: PlaybackRequest.LocalMedia,
        extra: PlayerExtra,
        result: AppResult<T>,
        onSuccess: PlayerUiState.Content.(T) -> PlayerUiState.Content,
    ) {
        // Merge into the state after the request completes, preserving other in-flight updates.
        val latest = mutableState.value as? PlayerUiState.Content ?: return
        // Episode switches reuse request IDs, so compare the full playback request.
        if (latest.request != request) return
        mutableState.value = when (result) {
            is AppResult.Success -> latest.onSuccess(result.value)
            is AppResult.Failure -> latest.copy(
                extraFailures = latest.extraFailures + (extra to result.error),
            )
        }
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
