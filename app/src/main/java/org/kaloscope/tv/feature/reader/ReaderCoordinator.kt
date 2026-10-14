package org.kaloscope.tv.feature.reader

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderSettingsPolicy
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.ReaderSource
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.core.reader.ReaderRequestStore
import org.kaloscope.tv.data.reader.ReaderContentLoader
import org.kaloscope.tv.data.reader.isContentChanged

sealed interface ReaderUiState {
    data object Idle : ReaderUiState

    data class Loading(val requestId: String, val serverId: String) : ReaderUiState

    data class Error(
        val requestId: String,
        val error: AppError,
        val retryable: Boolean = false,
    ) : ReaderUiState

    sealed interface Active : ReaderUiState {
        val requestId: String
        val serverId: String
        val content: ReaderContent
        val chapterOrder: ReaderChapterOrder
        val contentRevision: Long
        val isChapterLoading: Boolean
        val chapterError: AppError?
        val progressError: AppError?
    }

    data class Image(
        override val requestId: String,
        override val serverId: String,
        override val content: ReaderImageContent,
        val settings: ImageReaderSettings,
        override val chapterOrder: ReaderChapterOrder,
        override val contentRevision: Long = 0,
        override val isChapterLoading: Boolean = false,
        override val chapterError: AppError? = null,
        override val progressError: AppError? = null,
        val isLoadingMore: Boolean = false,
        val pageError: AppError? = null,
        val loadingPrevious: Boolean = false,
        val imagesExhausted: Boolean = content.imageOffset + content.images.size >= content.imageCount,
    ) : Active

    data class Text(
        override val requestId: String,
        override val serverId: String,
        override val content: ReaderTextContent,
        val settings: TextReaderSettings,
        override val chapterOrder: ReaderChapterOrder,
        override val contentRevision: Long = 0,
        override val isChapterLoading: Boolean = false,
        override val chapterError: AppError? = null,
        override val progressError: AppError? = null,
    ) : Active
}

class ReaderCoordinator(
    private val requestStore: ReaderRequestStore,
    private val contentLoader: ReaderContentLoader,
) {
    private val generation = AtomicLong(0)
    private val mutableState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)

    val state: StateFlow<ReaderUiState> = mutableState.asStateFlow()

    fun load(
        requestId: String,
        session: Session,
    ) {
        generation.incrementAndGet()
        val request = requestStore.get(requestId)
        if (request == null) {
            mutableState.value = ReaderUiState.Error(
                requestId = requestId,
                error = AppError.InvalidData("reader_request"),
            )
            return
        }
        if (request.serverId != session.server.id) {
            mutableState.value = ReaderUiState.Error(
                requestId = requestId,
                error = AppError.InvalidData("reader_server"),
            )
            return
        }
        mutableState.value = request.toUiState()
    }

    suspend fun loadLocal(session: Session) {
        val loading = mutableState.value as? ReaderUiState.Loading ?: return
        val request = requestStore.get(loading.requestId) as? ReaderRequest.Local ?: return
        if (request.serverId != session.server.id) return
        val requestGeneration = generation.get()
        val result = contentLoader.openLocal(session, request)
        currentCoroutineContext().ensureActive()
        if (generation.get() != requestGeneration) return
        mutableState.value = when (result) {
            is AppResult.Failure -> ReaderUiState.Error(request.requestId, result.error, retryable = true)
            is AppResult.Success -> when (val content = result.value) {
                is ReaderImageContent -> ReaderUiState.Image(
                    request.requestId, request.serverId, content, request.imageSettings, request.chapterOrder,
                )
                is ReaderTextContent -> ReaderUiState.Text(
                    request.requestId, request.serverId, content,
                    ReaderSettingsPolicy.sanitize(request.textSettings), request.chapterOrder,
                )
            }
        }
    }

    suspend fun refreshLocal(session: Session) {
        val current = mutableState.value as? ReaderUiState.Active ?: return
        if (current.serverId != session.server.id || current.content.source !is ReaderSource.Local ||
            current.hasUnauthorizedError()
        ) return
        val requestGeneration = generation.incrementAndGet()
        mutableState.value = current.startChapterLoading()
        val result = try {
            contentLoader.refreshLocal(session, current.content).also { currentCoroutineContext().ensureActive() }
        } catch (error: CancellationException) {
            if (generation.get() == requestGeneration) updateActive { finishChapterLoading() }
            throw error
        }
        if (generation.get() != requestGeneration) return
        when (result) {
            is AppResult.Success -> if (result.value.source == current.content.source) {
                updateActive { finishChapterLoading() }
            } else replaceChapterContent(result.value)
            is AppResult.Failure -> updateActive { finishChapterLoading(result.error) }
        }
    }

    suspend fun recordProgress(session: Session, content: ReaderContent, position: ReaderPosition): AppResult<Unit> {
        if (content.source !is ReaderSource.Local) return AppResult.Success(Unit)
        val result = contentLoader.recordProgress(session, content, position)
        currentCoroutineContext().ensureActive()
        updateActive {
            if (serverId != session.server.id || this.content.source != content.source) this
            else withProgressError((result as? AppResult.Failure)?.error)
        }
        return result
    }

    fun dismissProgressError() = updateActive {
        if (progressError == AppError.Unauthorized) this else withProgressError(null)
    }

    private fun ReaderUiState.Active.withProgressError(error: AppError?): ReaderUiState.Active = when (this) {
        is ReaderUiState.Image -> copy(progressError = progressError?.takeIf { it == AppError.Unauthorized } ?: error)
        is ReaderUiState.Text -> copy(progressError = progressError?.takeIf { it == AppError.Unauthorized } ?: error)
    }

    suspend fun selectChapter(
        session: Session,
        chapterIndex: Int,
    ) {
        val current = mutableState.value as? ReaderUiState.Active ?: return
        if (current.hasUnauthorizedError()) return
        val currentContent = current.content
        if (
            chapterIndex !in currentContent.chapters.indices ||
            currentContent.selectedChapterIndex == chapterIndex
        ) {
            return
        }
        // A chapter change invalidates pending image pages as well as older chapter loads.
        val requestGeneration = generation.incrementAndGet()
        mutableState.value = current.startChapterLoading()
        val result = try {
            contentLoader.resolveChapter(session, currentContent, chapterIndex).also {
                // A queued HTTP error can return as a result after cancellation.
                currentCoroutineContext().ensureActive()
            }
        } catch (error: CancellationException) {
            if (generation.get() == requestGeneration) {
                updateActive { finishChapterLoading() }
            }
            throw error
        }
        if (generation.get() != requestGeneration) return
        when (result) {
            is AppResult.Failure -> updateActive {
                finishChapterLoading(error = result.error)
            }

            is AppResult.Success -> replaceChapterContent(result.value)
        }
    }

    suspend fun loadMoreImages(session: Session, previous: Boolean = false) {
        val current = mutableState.value as? ReaderUiState.Image ?: return
        if (current.hasUnauthorizedError()) return
        // Displayed content still belongs to the old chapter while its replacement loads.
        if (current.isChapterLoading || current.isLoadingMore) return
        if (if (previous) current.content.imageOffset == 0 else current.imagesExhausted) return
        val requestGeneration = generation.get()
        mutableState.value = current.copy(isLoadingMore = true, pageError = null, loadingPrevious = previous)
        val result = try {
            contentLoader.loadImagePage(session, current.content, previous).also {
                currentCoroutineContext().ensureActive()
            }
        } catch (error: CancellationException) {
            if (generation.get() == requestGeneration) {
                updateImage { copy(isLoadingMore = false) }
            }
            throw error
        }
        if (generation.get() != requestGeneration) return
        if (result.isContentChanged() && current.content.source is ReaderSource.Local) {
            refreshLocal(session)
            return
        }
        when (result) {
            is AppResult.Failure -> updateImage {
                copy(isLoadingMore = false, pageError = result.error)
            }

            is AppResult.Success -> updateImage {
                val page = result.value
                val images = if (previous) (page.images + content.images).distinct()
                    else (content.images + page.images).distinct()
                val offset = if (previous) page.imageOffset ?: content.imageOffset else content.imageOffset
                copy(
                    content = content.copy(
                        images = images,
                        imageCount = page.imageCount,
                        imageOffset = offset,
                        nextOffset = if (previous) content.nextOffset else page.nextOffset,
                        initialPosition = if (previous) ReaderPosition((page.images.size - 1).coerceAtLeast(0))
                            else content.initialPosition,
                    ),
                    contentRevision = if (previous) contentRevision + 1 else contentRevision,
                    isLoadingMore = false,
                    pageError = null,
                    imagesExhausted = if (previous) imagesExhausted else page.exhausted ||
                        offset + images.size >= page.imageCount,
                )
            }
        }
    }

    fun updateImageSettings(settings: ImageReaderSettings) {
        updateImage { copy(settings = settings) }
    }

    fun updateTextSettings(settings: TextReaderSettings) {
        val sanitized = ReaderSettingsPolicy.sanitize(settings)
        val current = mutableState.value as? ReaderUiState.Text ?: return
        mutableState.value = current.copy(settings = sanitized)
    }

    fun updateChapterOrder(order: ReaderChapterOrder) {
        updateActive {
            when (this) {
                is ReaderUiState.Image -> copy(chapterOrder = order)
                is ReaderUiState.Text -> copy(chapterOrder = order)
            }
        }
    }

    fun dismissChapterError() {
        updateActive {
            // A late dismissal must not finish a newer load that already cleared the error.
            when (chapterError) {
                null, AppError.Unauthorized -> this
                else -> finishChapterLoading()
            }
        }
    }

    fun dismissPageError() {
        updateImage {
            if (pageError == AppError.Unauthorized) this else copy(pageError = null)
        }
    }

    fun close(requestId: String) {
        requestStore.remove(requestId)
        // Deferred cleanup of an outgoing reader must not invalidate a newer request.
        val currentRequestId = when (val current = mutableState.value) {
            is ReaderUiState.Active -> current.requestId
            is ReaderUiState.Error -> current.requestId
            is ReaderUiState.Loading -> current.requestId
            ReaderUiState.Idle -> null
        }
        if (currentRequestId != requestId) return
        generation.incrementAndGet()
        mutableState.value = ReaderUiState.Idle
    }

    fun clearServer(serverId: String) {
        generation.incrementAndGet()
        val current = mutableState.value
        val ownsCurrent = when (current) {
            is ReaderUiState.Active -> current.serverId == serverId
            is ReaderUiState.Loading -> current.serverId == serverId
            is ReaderUiState.Error -> requestStore.get(current.requestId)?.serverId == serverId
            ReaderUiState.Idle -> false
        }
        requestStore.clearServer(serverId)
        if (ownsCurrent) mutableState.value = ReaderUiState.Idle
    }

    private fun ReaderRequest.toUiState(): ReaderUiState =
        when (this) {
            is ReaderRequest.Local -> ReaderUiState.Loading(requestId, serverId)
            is ReaderRequest.Image -> ReaderUiState.Image(
                requestId = requestId,
                serverId = serverId,
                content = content,
                settings = settings,
                chapterOrder = chapterOrder,
            )

            is ReaderRequest.Text -> ReaderUiState.Text(
                requestId = requestId,
                serverId = serverId,
                content = content,
                settings = ReaderSettingsPolicy.sanitize(settings),
                chapterOrder = chapterOrder,
            )
        }

    // Keep authentication failures visible until root session handling clears the reader.
    private fun ReaderUiState.Active.hasUnauthorizedError(): Boolean =
        chapterError == AppError.Unauthorized ||
            progressError == AppError.Unauthorized ||
            (this is ReaderUiState.Image && pageError == AppError.Unauthorized)

    private fun replaceChapterContent(content: ReaderContent) {
        val current = mutableState.value as? ReaderUiState.Active ?: return
        mutableState.value = when {
            current is ReaderUiState.Image && content is ReaderImageContent -> current.copy(
                content = content,
                contentRevision = current.contentRevision + 1,
                isChapterLoading = false,
                chapterError = null,
                isLoadingMore = false,
                loadingPrevious = false,
                pageError = null,
                imagesExhausted = content.imageOffset + content.images.size >= content.imageCount,
            )

            current is ReaderUiState.Text && content is ReaderTextContent -> current.copy(
                content = content,
                contentRevision = current.contentRevision + 1,
                isChapterLoading = false,
                chapterError = null,
            )

            else -> current.finishChapterLoading(
                error = AppError.InvalidData("reader_content_type"),
            )
        }
    }

    private fun ReaderUiState.Active.finishChapterLoading(
        error: AppError? = null,
    ): ReaderUiState.Active =
        when (this) {
            is ReaderUiState.Image -> copy(
                isChapterLoading = false,
                chapterError = error,
            )

            is ReaderUiState.Text -> copy(
                isChapterLoading = false,
                chapterError = error,
            )
        }

    private fun ReaderUiState.Active.startChapterLoading(): ReaderUiState.Active =
        when (this) {
            is ReaderUiState.Image -> copy(
                isChapterLoading = true,
                chapterError = null,
                isLoadingMore = false,
                loadingPrevious = false,
                pageError = null,
            )

            is ReaderUiState.Text -> copy(
                isChapterLoading = true,
                chapterError = null,
            )
        }

    private inline fun updateActive(
        transform: ReaderUiState.Active.() -> ReaderUiState.Active,
    ) {
        val current = mutableState.value as? ReaderUiState.Active ?: return
        mutableState.value = current.transform()
    }

    private inline fun updateImage(
        transform: ReaderUiState.Image.() -> ReaderUiState.Image,
    ) {
        val current = mutableState.value as? ReaderUiState.Image ?: return
        mutableState.value = current.transform()
    }
}
