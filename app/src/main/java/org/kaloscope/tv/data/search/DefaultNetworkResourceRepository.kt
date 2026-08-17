package org.kaloscope.tv.data.search

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.common.trimmedOrNull
import org.kaloscope.tv.core.model.NetworkMediaType
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.ReaderSource
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.ResolvedNetworkResource
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.authorizationHeader
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall
import org.kaloscope.tv.core.player.NetworkVideoCodecSupport
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.data.search.remote.IndexerDetailsRequestData
import org.kaloscope.tv.data.search.remote.IndexerResourceData

@Singleton
class DefaultNetworkResourceRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val json: Json,
    private val videoCodecSupport: NetworkVideoCodecSupport =
        NetworkVideoCodecSupport.KeepServerOrder,
) : NetworkResourceRepository {
    override suspend fun resolveResource(
        session: Session,
        indexerId: Long,
        result: NetworkSearchResult,
        preferredDefinition: TranscodeResolution,
    ): AppResult<ResolvedNetworkResource> =
        networkCall(json) {
            val resource = details(
                session = session,
                indexerId = indexerId,
                resourceId = result.id,
            )
            val mediaType = resource.resolveMediaType(result.mediaType)
            when (mediaType) {
                NetworkMediaType.Audio ->
                    throw SerializationException("Unsupported network media type")

                NetworkMediaType.Video -> ResolvedNetworkResource.Video(
                    resolveInitialVideo(
                        session = session,
                        indexerId = indexerId,
                        resourceId = result.id,
                        fallbackTitle = result.title,
                        resource = resource,
                        preferredDefinition = preferredDefinition,
                        fallbackVideoType = result.videoTypeHint,
                    ),
                )

                NetworkMediaType.Image -> ResolvedNetworkResource.Image(
                    resolveInitialImage(
                        session = session,
                        indexerId = indexerId,
                        resourceId = result.id,
                        readerTitle = result.title,
                        resource = resource,
                    ),
                )

                NetworkMediaType.Text -> ResolvedNetworkResource.Text(
                    resolveInitialText(
                        session = session,
                        indexerId = indexerId,
                        resourceId = result.id,
                        readerTitle = result.title,
                        resource = resource,
                    ),
                )
            }
        }

    override suspend fun resolveVideoChapter(
        session: Session,
        source: NetworkPlaybackSource,
        chapterIndex: Int,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource> =
        networkCall(json) {
            val chapter = source.chapters.getOrNull(chapterIndex)
                ?: throw SerializationException("Missing network chapter")
            chapter.url?.let { directUrl ->
                return@networkCall source.copy(
                    title = chapter.title,
                    url = directUrl,
                    danmakus = emptyList(),
                    definitions = emptyList(),
                    selectedDefinitionIndex = null,
                    selectedChapterIndex = chapterIndex,
                )
            }
            val chapterId = chapter.id
                ?: throw SerializationException("Missing network chapter source")
            val resolved = details(
                session = session,
                indexerId = source.indexerId,
                resourceId = source.resourceId,
                chapterId = chapterId,
            ).toPlaybackSource(
                indexerId = source.indexerId,
                fallbackTitle = chapter.title,
                preferredDefinition = preferredDefinition,
                preferHevcForDash = videoCodecSupport.shouldPreferHevcForDash(),
                fallbackVideoType = source.videoType,
            ) ?: throw SerializationException("Missing playable network chapter")
            resolved.copy(
                chapters = source.chapters,
                selectedChapterIndex = chapterIndex,
            )
        }

    override suspend fun resolveReaderChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent> =
        networkCall(json) {
            val source = content.source as? ReaderSource.Network
                ?: throw SerializationException("Unsupported reader source")
            val chapter = content.chapters.getOrNull(chapterIndex)
                ?: throw SerializationException("Missing reader chapter")
            val resource = details(
                session = session,
                indexerId = source.indexerId,
                resourceId = source.resourceId,
                chapterId = chapter.id,
            )
            when (content) {
                is ReaderImageContent -> resource.toImageContent(
                    source = source.copy(chapterId = chapter.id),
                    readerTitle = content.title,
                    chapters = content.chapters,
                    selectedChapterIndex = chapterIndex,
                )

                is ReaderTextContent -> resource.toTextContent(
                    source = source.copy(chapterId = chapter.id),
                    readerTitle = content.title,
                    chapters = content.chapters,
                    selectedChapterIndex = chapterIndex,
                )
            }
        }

    override suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
    ): AppResult<ReaderImagePage> =
        networkCall(json) {
            val source = content.source as? ReaderSource.Network
                ?: throw SerializationException("Unsupported reader source")
            val resource = details(
                session = session,
                indexerId = source.indexerId,
                resourceId = source.resourceId,
                chapterId = source.chapterId,
                page = content.images.size + 1,
            )
            val knownImages = content.images.toHashSet()
            val appended = resource.images.orEmpty()
                .mapNotNull { it.trimmedOrNull() }
                .distinct()
                .filterNot(knownImages::contains)
            val imageCount = resource.imageCount
                ?.takeIf { it > 0 }
                ?: content.imageCount
            ReaderImagePage(
                images = appended,
                imageCount = imageCount,
                exhausted = appended.isEmpty() ||
                    content.images.size + appended.size >= imageCount,
            )
        }

    private suspend fun resolveInitialVideo(
        session: Session,
        indexerId: Long,
        resourceId: String,
        fallbackTitle: String,
        resource: IndexerResourceData,
        preferredDefinition: TranscodeResolution,
        fallbackVideoType: NetworkVideoType,
    ): NetworkPlaybackSource =
        resource.toPlaybackSource(
            indexerId = indexerId,
            fallbackTitle = fallbackTitle,
            preferredDefinition = preferredDefinition,
            preferHevcForDash = videoCodecSupport.shouldPreferHevcForDash(),
            fallbackVideoType = fallbackVideoType,
        ) ?: run {
            val chapters = resource.toChapters()
            val chapter = chapters.firstOrNull()
                ?: throw SerializationException("Missing playable network source")
            val chapterId = chapter.id
                ?: throw SerializationException("Missing playable network source")
            val resolved = details(
                session = session,
                indexerId = indexerId,
                resourceId = resourceId,
                chapterId = chapterId,
            ).toPlaybackSource(
                indexerId = indexerId,
                fallbackTitle = chapter.title,
                preferredDefinition = preferredDefinition,
                preferHevcForDash = videoCodecSupport.shouldPreferHevcForDash(),
                fallbackVideoType = fallbackVideoType,
            ) ?: throw SerializationException("Missing playable network chapter")
            resolved.copy(
                chapters = chapters,
                selectedChapterIndex = chapters.indices.firstOrNull(),
            )
        }

    private suspend fun resolveInitialImage(
        session: Session,
        indexerId: Long,
        resourceId: String,
        readerTitle: String,
        resource: IndexerResourceData,
    ): ReaderImageContent {
        val chapters = resource.toReaderChapters()
        val selectedChapterIndex = chapters.indices.firstOrNull()
        val activeChapter = selectedChapterIndex?.let(chapters::get)
        val source = ReaderSource.Network(indexerId, resourceId, activeChapter?.id)
        if (resource.images != null) {
            return resource.toImageContent(
                source = source,
                readerTitle = readerTitle,
                chapters = chapters,
                selectedChapterIndex = selectedChapterIndex,
            )
        }
        val chapter = activeChapter
            ?: throw SerializationException("Missing image reader content")
        return details(
            session = session,
            indexerId = indexerId,
            resourceId = resourceId,
            chapterId = chapter.id,
        ).toImageContent(
            source = source,
            readerTitle = readerTitle,
            chapters = chapters,
            selectedChapterIndex = selectedChapterIndex,
        )
    }

    private suspend fun resolveInitialText(
        session: Session,
        indexerId: Long,
        resourceId: String,
        readerTitle: String,
        resource: IndexerResourceData,
    ): ReaderTextContent {
        val chapters = resource.toReaderChapters()
        val selectedChapterIndex = chapters.indices.firstOrNull()
        val activeChapter = selectedChapterIndex?.let(chapters::get)
        val source = ReaderSource.Network(indexerId, resourceId, activeChapter?.id)
        if (resource.text != null) {
            return resource.toTextContent(
                source = source,
                readerTitle = readerTitle,
                chapters = chapters,
                selectedChapterIndex = selectedChapterIndex,
            )
        }
        val chapter = activeChapter
            ?: throw SerializationException("Missing text reader content")
        return details(
            session = session,
            indexerId = indexerId,
            resourceId = resourceId,
            chapterId = chapter.id,
        ).toTextContent(
            source = source,
            readerTitle = readerTitle,
            chapters = chapters,
            selectedChapterIndex = selectedChapterIndex,
        )
    }

    private suspend fun details(
        session: Session,
        indexerId: Long,
        resourceId: String,
        chapterId: String? = null,
        page: Int? = null,
    ): IndexerResourceData =
        apiClientFactory.create(session.server.origin).executeIndexerDetails(
            authorization = session.authorizationHeader(),
            indexerId = indexerId,
            body = IndexerDetailsRequestData(
                resourceId = resourceId,
                chapterId = chapterId?.let(::JsonPrimitive) ?: JsonNull,
                page = page,
            ),
        ).dataOrThrow() ?: throw SerializationException("Missing network details")

    private fun IndexerResourceData.resolveMediaType(
        fallback: NetworkMediaType,
    ): NetworkMediaType {
        if (mediaType.trimmedOrNull() == null) return fallback
        return mediaType.toNetworkMediaType()
            ?: throw SerializationException("Unsupported network media type")
    }

    private fun IndexerResourceData.toReaderChapters(): List<ReaderChapter> =
        chapters.orEmpty().mapNotNull { chapter ->
            val chapterId = chapter.id.trimmedOrNull()
            val chapterTitle = chapter.title.trimmedOrNull()
            if (chapterId == null || chapterTitle == null) {
                null
            } else {
                ReaderChapter(
                    id = chapterId,
                    title = chapterTitle,
                    volume = chapter.volume.trimmedOrNull(),
                )
            }
        }.distinctBy(ReaderChapter::id)

    private fun IndexerResourceData.toImageContent(
        source: ReaderSource.Network,
        readerTitle: String,
        chapters: List<ReaderChapter>,
        selectedChapterIndex: Int?,
    ): ReaderImageContent {
        val mappedImages = images
            ?.mapNotNull { it.trimmedOrNull() }
            ?.distinct()
            ?: throw SerializationException("Missing image reader content")
        return ReaderImageContent(
            source = source,
            title = readerTitle,
            images = mappedImages,
            imageCount = imageCount?.takeIf { it > 0 } ?: mappedImages.size,
            chapters = chapters,
            selectedChapterIndex = selectedChapterIndex,
        )
    }

    private fun IndexerResourceData.toTextContent(
        source: ReaderSource.Network,
        readerTitle: String,
        chapters: List<ReaderChapter>,
        selectedChapterIndex: Int?,
    ): ReaderTextContent = ReaderTextContent(
        source = source,
        title = readerTitle,
        text = toTextBody()
            ?: throw SerializationException("Missing text reader content"),
        chapters = chapters,
        selectedChapterIndex = selectedChapterIndex,
    )
}
