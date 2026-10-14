package org.kaloscope.tv.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.model.ReaderSource
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.model.isReading
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.core.reader.ReaderRequestStore
import org.kaloscope.tv.data.reader.ReaderContentLoader

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val requestStore: ReaderRequestStore,
    contentLoader: ReaderContentLoader,
) : ViewModel() {
    private val coordinator = ReaderCoordinator(requestStore, contentLoader)
    private var loadedRequestId: String? = null
    private var chapterJob: Job? = null
    private var pageJob: Job? = null
    private var progressTimer: Job? = null
    private var pendingProgress: PendingProgress? = null
    private var lastProgress: PendingProgress? = null
    private var progressScope = createProgressScope()

    val uiState: StateFlow<ReaderUiState> = coordinator.state

    fun createLocal(
        session: Session,
        detail: MediaDetail,
        childId: Long?,
        settings: TvSettings,
    ): String? {
        val type = detail.library?.type?.takeIf { it.isReading } ?: return null
        if (childId != null && detail.children.none { it.id == childId }) return null
        val requestId = UUID.randomUUID().toString()
        requestStore.put(ReaderRequest.Local(
            requestId = requestId,
            serverId = session.server.id,
            title = detail.title,
            mediaId = detail.id,
            libraryType = type,
            chapterId = childId?.let { "item:$it" },
            imageSettings = settings.imageReader,
            textSettings = settings.textReader,
            chapterOrder = settings.readerChapterOrder,
        ))
        return requestId
    }

    fun load(
        requestId: String,
        session: Session,
    ) {
        if (loadedRequestId == requestId) return
        flushProgress()
        lastProgress = null
        cancelContentJobs()
        loadedRequestId = requestId
        coordinator.load(requestId, session)
        chapterJob = viewModelScope.launch { coordinator.loadLocal(session) }
    }

    fun retry(session: Session) {
        val current = uiState.value as? ReaderUiState.Error ?: return
        if (!current.retryable || current.error == org.kaloscope.tv.core.common.AppError.Unauthorized) return
        cancelContentJobs()
        coordinator.load(current.requestId, session)
        chapterJob = viewModelScope.launch { coordinator.loadLocal(session) }
    }

    fun refreshLocal(session: Session) {
        flushProgress()
        cancelContentJobs()
        chapterJob = viewModelScope.launch { coordinator.refreshLocal(session) }
    }

    fun updateProgress(session: Session, content: ReaderContent, position: ReaderPosition) {
        val current = uiState.value as? ReaderUiState.Active ?: return
        if (content.source !is ReaderSource.Local || current.content.source != content.source ||
            current.serverId != session.server.id || current.isChapterLoading
        ) return
        val progress = PendingProgress(session, content, position)
        if (progress == lastProgress) return
        lastProgress = progress
        pendingProgress = progress
        if (progressTimer?.isActive == true) return
        progressTimer = viewModelScope.launch {
            delay(1_000)
            flushProgress()
        }
    }

    fun flushProgress() {
        progressTimer?.cancel()
        progressTimer = null
        val progress = pendingProgress ?: return
        pendingProgress = null
        progressScope.launch {
            val result = coordinator.recordProgress(progress.session, progress.content, progress.position)
            val current = uiState.value as? ReaderUiState.Active
            if (result is AppResult.Failure && result.error != AppError.Unauthorized &&
                lastProgress == progress && pendingProgress == null &&
                current?.serverId == progress.session.server.id && current.content.source == progress.content.source
            ) {
                // Preserve a failed save for the next pause/exit without replacing newer progress.
                pendingProgress = progress
            }
        }
    }

    fun dismissProgressError() {
        lastProgress = null
        coordinator.dismissProgressError()
    }

    fun selectChapter(
        session: Session,
        chapterIndex: Int,
    ) {
        val content = (uiState.value as? ReaderUiState.Active)?.content ?: return
        // No-op selections must not cancel the current page or chapter request.
        if (
            chapterIndex !in content.chapters.indices ||
            content.selectedChapterIndex == chapterIndex
        ) {
            return
        }
        flushProgress()
        chapterJob?.cancel()
        pageJob?.cancel()
        chapterJob = viewModelScope.launch {
            coordinator.selectChapter(session, chapterIndex)
        }
    }

    fun loadMoreImages(session: Session, previous: Boolean = false) {
        if (pageJob?.isActive == true) return
        pageJob = viewModelScope.launch { coordinator.loadMoreImages(session, previous) }
    }

    fun updateImageSettings(settings: ImageReaderSettings) =
        coordinator.updateImageSettings(settings)

    fun updateTextSettings(settings: TextReaderSettings) =
        coordinator.updateTextSettings(settings)

    fun updateChapterOrder(order: ReaderChapterOrder) =
        coordinator.updateChapterOrder(order)

    fun dismissChapterError() = coordinator.dismissChapterError()

    fun dismissPageError() = coordinator.dismissPageError()

    fun close(requestId: String) {
        if (loadedRequestId == requestId) {
            flushProgress()
            lastProgress = null
            cancelContentJobs()
            loadedRequestId = null
        }
        coordinator.close(requestId)
    }

    fun clearServer(serverId: String) {
        progressTimer?.cancel()
        pendingProgress = null
        lastProgress = null
        progressScope.cancel()
        progressScope = createProgressScope()
        cancelContentJobs()
        loadedRequestId = null
        coordinator.clearServer(serverId)
    }

    private fun cancelContentJobs() {
        chapterJob?.cancel()
        chapterJob = null
        pageJob?.cancel()
        pageJob = null
    }

    private fun createProgressScope() = CoroutineScope(
        viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]),
    )

    private data class PendingProgress(
        val session: Session,
        val content: ReaderContent,
        val position: ReaderPosition,
    )
}
