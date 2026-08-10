package org.kaloscope.tv.feature.reader

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
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
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.core.reader.ReaderRequestStore
import org.kaloscope.tv.data.reader.ReaderContentLoader

sealed interface ReaderUiState {
    data object Idle : ReaderUiState

    data class Error(
        val requestId: String,
        val error: AppError,
    ) : ReaderUiState

    sealed interface Active : ReaderUiState {
        val requestId: String
        val serverId: String
        val chapterOrder: ReaderChapterOrder
        val contentRevision: Long
        val isChapterLoading: Boolean
        val chapterError: AppError?
    }

    data class Image(
        override val requestId: String,
        override val serverId: String,
        val content: ReaderImageContent,
        val settings: ImageReaderSettings,
        override val chapterOrder: ReaderChapterOrder,
        override val contentRevision: Long = 0,
        override val isChapterLoading: Boolean = false,
        override val chapterError: AppError? = null,
        val isLoadingMore: Boolean = false,
        val pageError: AppError? = null,
        val imagesExhausted: Boolean = content.images.size >= content.imageCount,
    ) : Active

    data class Text(
        override val requestId: String,
        override val serverId: String,
        val content: ReaderTextContent,
        val settings: TextReaderSettings,
        override val chapterOrder: ReaderChapterOrder,
        override val contentRevision: Long = 0,
        override val isChapterLoading: Boolean = false,
        override val chapterError: AppError? = null,
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

    suspend fun selectChapter(
        session: Session,
        chapterIndex: Int,
    ) {
        val current = mutableState.value as? ReaderUiState.Active ?: return
        val currentContent = current.readerContent()
        if (
            chapterIndex !in currentContent.chapters.indices ||
            currentContent.selectedChapterIndex == chapterIndex
        ) {
            return
        }
        val requestGeneration = generation.incrementAndGet()
        mutableState.value = current.startChapterLoading()
        val result = try {
            contentLoader.resolveChapter(session, currentContent, chapterIndex)
        } catch (error: CancellationException) {
            if (generation.get() == requestGeneration) {
                updateActive { withChapterStatus(loading = false, error = null) }
            }
            throw error
        }
        if (generation.get() != requestGeneration) return
        when (result) {
            is AppResult.Failure -> updateActive {
                withChapterStatus(loading = false, error = result.error)
            }

            is AppResult.Success -> replaceChapterContent(result.value)
        }
    }

    suspend fun loadMoreImages(session: Session) {
        val current = mutableState.value as? ReaderUiState.Image ?: return
        if (current.isLoadingMore || current.imagesExhausted) return
        val requestGeneration = generation.get()
        mutableState.value = current.copy(isLoadingMore = true, pageError = null)
        val result = try {
            contentLoader.loadImagePage(session, current.content)
        } catch (error: CancellationException) {
            if (generation.get() == requestGeneration) {
                updateImage { copy(isLoadingMore = false) }
            }
            throw error
        }
        if (generation.get() != requestGeneration) return
        when (result) {
            is AppResult.Failure -> updateImage {
                copy(isLoadingMore = false, pageError = result.error)
            }

            is AppResult.Success -> updateImage {
                val images = (content.images + result.value.images).distinct()
                copy(
                    content = content.copy(
                        images = images,
                        imageCount = result.value.imageCount,
                    ),
                    isLoadingMore = false,
                    pageError = null,
                    imagesExhausted = result.value.exhausted ||
                        images.size >= result.value.imageCount,
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
        updateActive { withChapterStatus(loading = false, error = null) }
    }

    fun dismissPageError() {
        updateImage { copy(pageError = null) }
    }

    fun close(requestId: String) {
        generation.incrementAndGet()
        requestStore.remove(requestId)
        mutableState.value = ReaderUiState.Idle
    }

    fun clearServer(serverId: String) {
        generation.incrementAndGet()
        requestStore.clearServer(serverId)
        val current = mutableState.value as? ReaderUiState.Active
        if (current?.serverId == serverId) {
            mutableState.value = ReaderUiState.Idle
        }
    }

    private fun ReaderRequest.toUiState(): ReaderUiState.Active =
        when (this) {
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

    private fun replaceChapterContent(content: ReaderContent) {
        val current = mutableState.value as? ReaderUiState.Active ?: return
        mutableState.value = when {
            current is ReaderUiState.Image && content is ReaderImageContent -> current.copy(
                content = content,
                contentRevision = current.contentRevision + 1,
                isChapterLoading = false,
                chapterError = null,
                isLoadingMore = false,
                pageError = null,
                imagesExhausted = content.images.size >= content.imageCount,
            )

            current is ReaderUiState.Text && content is ReaderTextContent -> current.copy(
                content = content,
                contentRevision = current.contentRevision + 1,
                isChapterLoading = false,
                chapterError = null,
            )

            else -> current.withChapterStatus(
                loading = false,
                error = AppError.InvalidData("reader_content_type"),
            )
        }
    }

    private fun ReaderUiState.Active.readerContent(): ReaderContent =
        when (this) {
            is ReaderUiState.Image -> content
            is ReaderUiState.Text -> content
        }

    private fun ReaderUiState.Active.withChapterStatus(
        loading: Boolean,
        error: AppError?,
    ): ReaderUiState.Active =
        when (this) {
            is ReaderUiState.Image -> copy(
                isChapterLoading = loading,
                chapterError = error,
            )

            is ReaderUiState.Text -> copy(
                isChapterLoading = loading,
                chapterError = error,
            )
        }

    private fun ReaderUiState.Active.startChapterLoading(): ReaderUiState.Active =
        when (this) {
            is ReaderUiState.Image -> copy(
                isChapterLoading = true,
                chapterError = null,
                isLoadingMore = false,
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
