package org.kaloscope.tv.data.search

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DEFAULT_COVER_ASPECT_RATIO
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall
import org.kaloscope.tv.data.search.remote.IndexerDetailsRequestData
import org.kaloscope.tv.data.search.remote.IndexerSearchRequestData

@Singleton
class DefaultSearchRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val json: Json,
) : SearchRepository {
    override suspend fun getIndexers(
        session: Session,
    ): AppResult<List<NetworkIndexer>> =
        networkCall(json) {
            api(session).getIndexers(session.authorization())
                .dataOrThrow()
                .toModels()
        }

    override suspend fun getProfile(
        session: Session,
        indexer: NetworkIndexer,
    ): AppResult<IndexerSourceProfile> =
        networkCall(json) {
            val client = api(session)
            val config = client.getIndexerConfig(session.authorization(), indexer.id)
                .dataOrThrow()
            val loginRequired = config.auth?.login?.required == true
            val webAuthRequired = loginRequired &&
                client.getIndexerAuth(session.authorization(), indexer.id).dataOrThrow() == null
            IndexerSourceProfile(
                indexer = indexer,
                pageSize = config.search?.display?.pageSize
                    ?.takeIf { it in 1..100 }
                    ?: DEFAULT_PAGE_SIZE,
                keywordRequired = config.search?.keyword?.required ?: true,
                webAuthRequired = webAuthRequired,
                coverRatio = config.search?.display?.coverRatio.toCoverAspectRatio(),
            )
        }

    override suspend fun search(
        session: Session,
        profile: IndexerSourceProfile,
        keyword: String,
        pageNumber: Int,
    ): AppResult<NetworkSearchPage> =
        networkCall(json) {
            api(session).executeIndexerSearch(
                authorization = session.authorization(),
                indexerId = profile.indexer.id,
                body = IndexerSearchRequestData(
                    pageNumber = pageNumber,
                    pageSize = profile.pageSize,
                    keyword = keyword.trim(),
                ),
            ).dataOrThrow().toModel(pageNumber, profile.pageSize)
        }

    override suspend fun resolvePlayback(
        session: Session,
        indexerId: Long,
        result: NetworkSearchResult,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource> =
        networkCall(json) {
            val resource = api(session).executeIndexerDetails(
                authorization = session.authorization(),
                indexerId = indexerId,
                body = IndexerDetailsRequestData(resourceId = result.id),
            ).dataOrThrow() ?: throw SerializationException("Missing network details")
            resource.toPlaybackSource(indexerId, result.title, preferredDefinition)
                ?: resolveInitialChapter(
                    session = session,
                    indexerId = indexerId,
                    fallbackTitle = result.title,
                    resource = resource,
                    preferredDefinition = preferredDefinition,
                )
        }

    override suspend fun resolveChapter(
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
            val resolved = api(session).executeIndexerDetails(
                authorization = session.authorization(),
                indexerId = source.indexerId,
                body = IndexerDetailsRequestData(
                    resourceId = source.resourceId,
                    chapterId = JsonPrimitive(chapterId),
                ),
            ).dataOrThrow()
                ?.toPlaybackSource(source.indexerId, chapter.title, preferredDefinition)
                ?: throw SerializationException("Missing playable network chapter")
            resolved.copy(
                chapters = source.chapters,
                selectedChapterIndex = chapterIndex,
            )
        }

    private suspend fun resolveInitialChapter(
        session: Session,
        indexerId: Long,
        fallbackTitle: String,
        resource: org.kaloscope.tv.data.search.remote.IndexerResourceData,
        preferredDefinition: TranscodeResolution,
    ): NetworkPlaybackSource {
        val firstChapter = resource.chapters.orEmpty().firstOrNull()
            ?: throw SerializationException("Missing playable network source")
        // Some indexers expose only chapter IDs until details runs for one chapter.
        val chapterId = firstChapter.id?.trim()?.takeIf(String::isNotEmpty)
            ?: throw SerializationException("Missing playable network source")
        val resolved = api(session).executeIndexerDetails(
            authorization = session.authorization(),
            indexerId = indexerId,
            body = IndexerDetailsRequestData(
                resourceId = resource.id ?: throw SerializationException("Missing resource id"),
                chapterId = JsonPrimitive(chapterId),
            ),
        ).dataOrThrow()
            ?.toPlaybackSource(indexerId, firstChapter.title ?: fallbackTitle, preferredDefinition)
            ?: throw SerializationException("Missing playable network chapter")
        val chapters = resource.toChapters()
        return resolved.copy(
            chapters = chapters,
            selectedChapterIndex = chapters.indices.firstOrNull(),
        )
    }

    private fun api(session: Session) = apiClientFactory.create(session.server.origin)

    private fun Session.authorization(): String = "Token $token"

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}

internal fun String?.toCoverAspectRatio(): Float {
    val raw = this?.trim()?.lowercase()
    // Auto cannot provide stable TV grid geometry before the image is loaded.
    if (raw.isNullOrEmpty() || raw == "auto") {
        return DEFAULT_COVER_ASPECT_RATIO
    }
    val parts = raw.split('/', limit = 2)
    val width = parts.firstOrNull()?.toFloatOrNull()
    val height = parts.getOrNull(1)?.toFloatOrNull() ?: 1f
    val ratio = if (width != null && height != 0f) width / height else Float.NaN
    return ratio.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_COVER_ASPECT_RATIO
}
