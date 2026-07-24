package org.kaloscope.tv.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.player.PlaybackOrigin
import org.kaloscope.tv.core.player.PlaybackProgressRecorder
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.core.player.ProgressReason
import org.kaloscope.tv.data.history.HistoryRepository
import org.kaloscope.tv.data.media.MediaRepository

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val requestStore: PlaybackRequestStore,
    mediaRepository: MediaRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    private val coordinator = PlayerCoordinator(requestStore, mediaRepository)
    private var currentRequestId: String? = null
    private var loadJob: Job? = null
    private var progressRecorder = PlaybackProgressRecorder()

    val uiState: StateFlow<PlayerUiState> = coordinator.state

    fun createFromHistory(
        session: Session,
        item: WatchHistoryItem,
    ): String? =
        createLocalRequest(
            session = session,
            mediaId = item.mediaId,
            path = item.path,
            title = item.title,
            resumePositionSeconds = item.positionSeconds,
            origin = PlaybackOrigin.Home,
        )

    fun createFromDetail(
        session: Session,
        detail: MediaDetail,
        resumePositionSeconds: Long?,
    ): String? =
        createLocalRequest(
            session = session,
            mediaId = detail.id,
            path = detail.path,
            title = detail.title,
            resumePositionSeconds = resumePositionSeconds,
            origin = PlaybackOrigin.MediaDetail,
        )

    fun load(
        session: Session,
        requestId: String,
    ) {
        if (currentRequestId == requestId) {
            return
        }
        currentRequestId = requestId
        progressRecorder = PlaybackProgressRecorder()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            coordinator.load(session, requestId)
        }
    }

    fun recordProgress(
        session: Session,
        positionMillis: Long,
        durationMillis: Long,
        reason: ProgressReason,
    ) {
        val request = (uiState.value as? PlayerUiState.Content)?.request
            as? PlaybackRequest.LocalMedia
            ?: return
        if (!progressRecorder.shouldRecord(
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                nowMillis = android.os.SystemClock.elapsedRealtime(),
                reason = reason,
            )
        ) {
            return
        }
        val safePosition = positionMillis.coerceIn(0, durationMillis)
        val positionSeconds = safePosition / 1_000
        val percentage = ((safePosition * 100) / durationMillis).toInt().coerceIn(0, 100)
        viewModelScope.launch {
            val result = historyRepository.recordVideoProgress(
                session = session,
                mediaId = request.mediaId,
                positionSeconds = positionSeconds,
                percentage = percentage,
            )
            if (result is AppResult.Failure) {
                coordinator.reportProgressFailure(result.error)
            }
        }
    }

    fun close(requestId: String) {
        requestStore.remove(requestId)
        if (currentRequestId == requestId) {
            loadJob?.cancel()
            loadJob = null
            currentRequestId = null
        }
    }

    fun clearServer(serverId: String) {
        loadJob?.cancel()
        loadJob = null
        currentRequestId = null
        requestStore.clearServer(serverId)
    }

    private fun createLocalRequest(
        session: Session,
        mediaId: Long,
        path: String,
        title: String,
        resumePositionSeconds: Long?,
        origin: PlaybackOrigin,
    ): String? {
        if (mediaId <= 0 || path.isBlank() || title.isBlank()) {
            return null
        }
        val request = PlaybackRequest.LocalMedia(
            requestId = UUID.randomUUID().toString(),
            serverId = session.server.id,
            mediaId = mediaId,
            path = path,
            title = title,
            resumePositionSeconds = resumePositionSeconds?.coerceAtLeast(0),
            origin = origin,
        )
        requestStore.put(request)
        return request.requestId
    }
}
