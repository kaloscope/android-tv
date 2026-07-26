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
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.player.PlaybackOrigin
import org.kaloscope.tv.core.player.PlaybackProgressRecorder
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.PlaybackRequestNavigator
import org.kaloscope.tv.core.player.PlaybackRequestStore
import org.kaloscope.tv.core.player.LocalEpisodeRef
import org.kaloscope.tv.core.player.ProgressReason
import org.kaloscope.tv.data.history.HistoryRepository
import org.kaloscope.tv.data.media.MediaRepository
import org.kaloscope.tv.data.search.SearchRepository

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val requestStore: PlaybackRequestStore,
    mediaRepository: MediaRepository,
    private val historyRepository: HistoryRepository,
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val coordinator = PlayerCoordinator(requestStore, mediaRepository)
    private var currentRequestId: String? = null
    private var loadJob: Job? = null
    private val progressRecorders = mutableMapOf<Long, PlaybackProgressRecorder>()
    private val progressJobs = mutableMapOf<Long, Job>()
    private val extraRetryJobs = mutableMapOf<PlayerExtra, Job>()

    val uiState: StateFlow<PlayerUiState> = coordinator.state

    fun createFromHistory(
        session: Session,
        item: WatchHistoryItem,
        settings: TvSettings = TvSettings(),
    ): String? =
        createLocalRequest(
            session = session,
            mediaId = item.mediaId,
            path = item.path,
            title = item.title,
            resumePositionSeconds = item.positionSeconds,
            origin = PlaybackOrigin.Home,
            settings = settings,
        )

    fun createFromDetail(
        session: Session,
        detail: MediaDetail,
        siblings: List<MediaSummary>,
        resumePositionSeconds: Long?,
        settings: TvSettings = TvSettings(),
    ): String? =
        createLocalRequest(
            session = session,
            mediaId = detail.id,
            path = detail.path,
            title = detail.title,
            resumePositionSeconds = resumePositionSeconds,
            origin = PlaybackOrigin.MediaDetail,
            siblings = siblings,
            settings = settings,
        )

    fun load(
        session: Session,
        requestId: String,
    ) {
        if (currentRequestId == requestId) {
            return
        }
        cancelExtraRetries()
        currentRequestId = requestId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            coordinator.load(session, requestId)
        }
    }

    fun recordProgress(
        session: Session,
        request: PlaybackRequest,
        positionMillis: Long,
        durationMillis: Long,
        reason: ProgressReason,
        nowMillis: Long = android.os.SystemClock.elapsedRealtime(),
        onSaved: () -> Unit = {},
    ) {
        // A retiring controller must not write its position to the newly selected episode.
        val localRequest = request as? PlaybackRequest.LocalMedia ?: return
        val recorder = progressRecorders.getOrPut(localRequest.mediaId) {
            PlaybackProgressRecorder()
        }
        if (!recorder.shouldRecord(
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                nowMillis = nowMillis,
                reason = reason,
            )
        ) {
            return
        }
        // Preserve elapsed time when a stream has not exposed its duration yet.
        val safePosition = if (durationMillis > 0) {
            positionMillis.coerceIn(0, durationMillis)
        } else {
            positionMillis.coerceAtLeast(0)
        }
        val positionSeconds = safePosition / 1_000
        val percentage = if (durationMillis > 0) {
            ((safePosition * 100) / durationMillis).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val previousWrite = progressJobs[localRequest.mediaId]
        progressJobs[localRequest.mediaId] = viewModelScope.launch {
            // Preserve playback order so a slow older request cannot overwrite newer progress.
            previousWrite?.join()
            val result = historyRepository.recordVideoProgress(
                session = session,
                mediaId = localRequest.mediaId,
                positionSeconds = positionSeconds,
                percentage = percentage,
            )
            when (result) {
                is AppResult.Failure -> coordinator.reportProgressFailure(result.error)
                is AppResult.Success -> onSaved()
            }
        }
    }

    fun selectDefinition(
        session: Session,
        definitionIndex: Int,
        positionMillis: Long,
    ) {
        val request = (uiState.value as? PlayerUiState.Content)?.request
            as? PlaybackRequest.NetworkVideo
            ?: return
        val selected = PlaybackRequestNavigator.selectDefinition(
            request,
            definitionIndex,
            positionMillis,
        ) ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            coordinator.replaceRequest(session, selected)
        }
    }

    fun switchAdjacent(
        session: Session,
        offset: Int,
    ) {
        val request = (uiState.value as? PlayerUiState.Content)?.request ?: return
        if (request is PlaybackRequest.LocalMedia) {
            val selected = PlaybackRequestNavigator.selectLocalAdjacent(request, offset) ?: return
            cancelExtraRetries()
            coordinator.beginItemSwitch()
            loadJob?.cancel()
            loadJob = viewModelScope.launch {
                coordinator.replaceRequest(session, selected)
            }
            return
        }
        val networkRequest = request as PlaybackRequest.NetworkVideo
        val chapterIndex = PlaybackRequestNavigator.adjacentNetworkChapter(
            networkRequest,
            offset,
        ) ?: return
        cancelExtraRetries()
        coordinator.beginItemSwitch()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            when (
                val result = searchRepository.resolveChapter(
                    session = session,
                    source = networkRequest.source,
                    chapterIndex = chapterIndex,
                    preferredDefinition = networkRequest.preferredDefinition,
                )
            ) {
                is AppResult.Success -> coordinator.replaceRequest(
                    session,
                    networkRequest.copy(
                        title = result.value.title,
                        source = result.value,
                        resumePositionMillis = 0,
                    ),
                )

                is AppResult.Failure -> coordinator.reportItemSwitchFailure(result.error)
            }
        }
    }

    fun retryExtra(
        session: Session,
        extra: PlayerExtra,
    ) {
        if (extra == PlayerExtra.MediaProbe) {
            return
        }
        extraRetryJobs.remove(extra)?.cancel()
        extraRetryJobs[extra] = viewModelScope.launch {
            coordinator.retryExtra(session, extra)
        }
    }

    fun close(requestId: String) {
        requestStore.remove(requestId)
        if (currentRequestId == requestId) {
            cancelExtraRetries()
            loadJob?.cancel()
            loadJob = null
            currentRequestId = null
            progressRecorders.clear()
            progressJobs.entries.removeAll { it.value.isCompleted }
        }
    }

    fun clearServer(serverId: String) {
        cancelExtraRetries()
        loadJob?.cancel()
        loadJob = null
        currentRequestId = null
        progressRecorders.clear()
        progressJobs.values.forEach(Job::cancel)
        progressJobs.clear()
        requestStore.clearServer(serverId)
    }

    private fun cancelExtraRetries() {
        extraRetryJobs.values.forEach(Job::cancel)
        extraRetryJobs.clear()
    }

    private fun createLocalRequest(
        session: Session,
        mediaId: Long,
        path: String,
        title: String,
        resumePositionSeconds: Long?,
        origin: PlaybackOrigin,
        siblings: List<MediaSummary> = emptyList(),
        settings: TvSettings = TvSettings(),
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
            playbackMode = settings.playbackMode,
            transcodeResolution = settings.transcodeResolution,
            siblings = siblings.mapNotNull { item ->
                LocalEpisodeRef(
                    mediaId = item.id,
                    path = item.path,
                    title = item.title,
                ).takeIf { it.mediaId > 0 && it.path.isNotBlank() && it.title.isNotBlank() }
            },
            autoplayNext = settings.autoplayNext,
            danmakuSettings = settings.danmaku,
            subtitleSettings = settings.subtitle,
        )
        requestStore.put(request)
        return request.requestId
    }
}
