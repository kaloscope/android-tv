package org.kaloscope.tv.data.reader

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.model.ReaderSource
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.authorizationHeader
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.data.reader.remote.ReadingLocatorData
import org.kaloscope.tv.data.reader.remote.ReadingRecordData
import retrofit2.HttpException

@Singleton
class LocalReaderRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val json: Json,
) {
    private val historyMutex = Mutex()

    suspend fun open(session: Session, request: ReaderRequest.Local): AppResult<ReaderContent> {
        val relationType = when (request.libraryType) {
            MediaLibraryType.Comic -> "image"
            MediaLibraryType.Novel -> "text"
            else -> return AppResult.Failure(AppError.InvalidData("reading library"))
        }
        val history = historyMutex.withLock {
            networkCall(json) {
                api(session).getReadingHistory(session.authorizationHeader(), relationType, request.mediaId)
                    .dataOrThrow().items.firstOrNull {
                        it.relationId == request.mediaId && it.relationType == relationType
                    }
            }
        }
        if (history is AppResult.Failure) return history
        val locator = (history as AppResult.Success).value?.locator?.takeIf {
            val savedChapter = it.chapterId ?: "item:${it.chapterItemId ?: request.mediaId}"
            request.chapterId == null || request.chapterId == savedChapter
        }
        val selected = locator?.chapterId
            ?: locator?.pageId?.let { "item:${locator.chapterItemId ?: request.mediaId}" }
            ?: request.chapterId
        val result = content(session, request.mediaId, request.title, selected, locator?.version, locator)
        val recovered = if (result.isContentChanged()) {
            content(
                session, request.mediaId, request.title,
                selected.takeIf { relationType == "image" },
            )
        } else result
        return if (recovered is AppResult.Success &&
            (recovered.value is ReaderImageContent) != (relationType == "image")
        ) AppResult.Failure(AppError.InvalidData("reading media type")) else recovered
    }

    suspend fun resolveChapter(
        session: Session,
        current: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent> {
        val source = current.source as? ReaderSource.Local
            ?: return AppResult.Failure(AppError.InvalidData("local reader source"))
        val chapter = current.chapters.getOrNull(chapterIndex)
            ?: return AppResult.Failure(AppError.InvalidData("reader chapter"))
        // Comic chapters have independent versions. Novel sections share one published index.
        val version = source.version.takeIf { current is ReaderTextContent }
        val result = content(session, source.mediaId, current.title, chapter.id, version)
        return if (result.isContentChanged()) content(
            session, source.mediaId, current.title,
            chapter.id.takeIf { current is ReaderImageContent },
        ) else result
    }

    suspend fun refresh(session: Session, current: ReaderContent): AppResult<ReaderContent> {
        val source = current.source as? ReaderSource.Local
            ?: return AppResult.Failure(AppError.InvalidData("local reader source"))
        val result = content(session, source.mediaId, current.title, source.chapterId, source.version)
        return if (result.isContentChanged()) content(
            session, source.mediaId, current.title,
            source.chapterId.takeIf { current is ReaderImageContent },
        ) else result
    }

    suspend fun imagePage(
        session: Session,
        current: ReaderImageContent,
        previous: Boolean,
    ): AppResult<ReaderImagePage> = networkCall(json) {
        val source = current.source as? ReaderSource.Local
            ?: throw SerializationException("Invalid local reader source")
        val offset = if (previous) (current.imageOffset - PAGE_SIZE).coerceAtLeast(0)
            else current.nextOffset ?: current.imageCount
        val limit = if (previous) current.imageOffset - offset else PAGE_SIZE
        val data = api(session).getLocalContent(
            session.authorizationHeader(), source.mediaId, source.chapterId, source.version,
            offset = offset, limit = limit.coerceAtLeast(1),
        ).dataOrThrow()
        val page = data.toContent(source.mediaId, current.title) as? ReaderImageContent
            ?: throw SerializationException("Invalid image page")
        if (page.source != source || page.imageOffset != offset || page.imageCount != current.imageCount) {
            throw SerializationException("Mismatched local image page")
        }
        ReaderImagePage(page.images, page.imageCount, page.nextOffset == null, page.imageOffset, page.nextOffset)
    }

    suspend fun recordProgress(
        session: Session,
        content: ReaderContent,
        position: ReaderPosition,
    ): AppResult<Unit> = historyMutex.withLock {
        networkCall(json) {
            val record = content.readingRecord(position) ?: return@networkCall
            val response = api(session).recordReadingProgress(session.authorizationHeader(), record)
            if (!response.isSuccessful) throw HttpException(response)
        }
    }

    private suspend fun content(
        session: Session,
        mediaId: Long,
        title: String,
        chapterId: String? = null,
        version: String? = null,
        locator: ReadingLocatorData? = null,
    ): AppResult<ReaderContent> = networkCall(json) {
        api(session).getLocalContent(
            session.authorizationHeader(), mediaId, chapterId, version, pageId = locator?.pageId,
        ).dataOrThrow().toContent(mediaId, title, locator)
    }

    private fun api(session: Session) = apiClientFactory.create(session.server.origin)
}

internal fun AppResult<*>.isContentChanged(): Boolean =
    this is AppResult.Failure && (error as? AppError.Api)?.code == "content_changed"

internal fun ReaderContent.readingRecord(position: ReaderPosition): ReadingRecordData? {
    val local = source as? ReaderSource.Local ?: return null
    if (!position.offset.isFinite() || position.chapterProgress?.isFinite() == false) return null
    val offset = position.offset.coerceIn(0.0, 1.0)
    val chapterProgress = position.chapterProgress?.coerceIn(0.0, 1.0)
    val anchor: ReadingLocatorData
    val percentage: Double
    val relationType: String
    when (this) {
        is ReaderImageContent -> {
            val url = images.getOrNull(position.index) ?: return null
            anchor = ReadingLocatorData(
                version = local.version,
                chapterItemId = local.sourceItemId.takeIf { it != local.mediaId },
                pageId = url.assetId(),
                offset = offset,
            )
            percentage = (chapterProgress ?: ((imageOffset + position.index + offset) / imageCount.coerceAtLeast(1))) * 100.0
            relationType = "image"
        }
        is ReaderTextContent -> {
            val isEpub = local.format == "epub"
            val count = if (isEpub) blocks.size else paragraphs.size
            if (position.index !in 0 until count) return null
            anchor = ReadingLocatorData(
                version = local.version,
                chapterId = local.chapterId,
                paragraph = position.index.takeUnless { isEpub },
                blockId = blocks.getOrNull(position.index)?.id.takeIf { isEpub },
                offset = offset,
            )
            percentage = ((selectedChapterIndex ?: 0) + (chapterProgress ?: ((position.index + offset) / count))) *
                100.0 / chapters.size.coerceAtLeast(1)
            relationType = "text"
        }
    }
    return ReadingRecordData(relationType, local.mediaId, percentage.toInt().coerceIn(0, 100), anchor)
}

private const val PAGE_SIZE = 20
