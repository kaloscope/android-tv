package org.kaloscope.tv.data.search

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DEFAULT_COVER_ASPECT_RATIO
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall
import org.kaloscope.tv.data.search.remote.IndexerDetailsRequestData

@Singleton
class DefaultSearchRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val json: Json,
) : SearchRepository {
    override suspend fun getAvailableProfiles(
        session: Session,
    ): AppResult<List<IndexerSourceProfile>> {
        val indexers = when (val result = getIndexers(session)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value
        }
        if (indexers.isEmpty()) {
            return AppResult.Success(emptyList())
        }
        val semaphore = Semaphore(permits = PROFILE_LOAD_CONCURRENCY)
        val loads = supervisorScope {
            indexers.map { indexer ->
                async {
                    semaphore.withPermit {
                        loadCatalogProfile(session, indexer)
                    }
                }
            }.awaitAll()
        }
        val profiles = loads.mapNotNull { load ->
            (load as? ProfileLoad.Available)?.profile
        }
        if (profiles.isNotEmpty()) {
            return AppResult.Success(profiles)
        }
        val failure = loads.filterIsInstance<ProfileLoad.Failed>().firstOrNull()
        return failure?.let { AppResult.Failure(it.error) }
            ?: AppResult.Success(emptyList())
    }

    private suspend fun getIndexers(
        session: Session,
    ): AppResult<List<NetworkIndexer>> =
        networkCall(json) {
            api(session).getIndexers(session.authorization())
                .dataOrThrow()
                .toModels()
        }

    private suspend fun getProfile(
        session: Session,
        indexer: NetworkIndexer,
    ): AppResult<IndexerSourceProfile?> =
        networkCall(json) {
            val client = api(session)
            val config = client.getIndexerConfig(session.authorization(), indexer.id)
                .dataOrThrow()
            val loginRequired = config.auth?.login?.required == true
            if (
                loginRequired &&
                client.getIndexerAuth(session.authorization(), indexer.id).dataOrThrow() == null
            ) {
                return@networkCall null
            }
            IndexerSourceProfile(
                indexer = indexer,
                pageSize = config.search?.display?.pageSize
                    ?.takeIf { it in 1..100 }
                    ?: DEFAULT_PAGE_SIZE,
                keywordRequired = config.search?.keyword?.required ?: true,
                coverRatio = config.search?.display?.coverRatio.toCoverAspectRatio(),
                filters = config.search?.toFilterDefinitions().orEmpty(),
            )
        }

    override suspend fun search(
        session: Session,
        profile: IndexerSourceProfile,
        keyword: String,
        filters: Map<String, SearchFilterValue>,
        pageNumber: Int,
    ): AppResult<NetworkSearchPage> =
        networkCall(json) {
            api(session).executeIndexerSearch(
                authorization = session.authorization(),
                indexerId = profile.indexer.id,
                body = buildIndexerSearchRequest(
                    profile = profile,
                    keyword = keyword,
                    filters = filters,
                    pageNumber = pageNumber,
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

    private suspend fun loadCatalogProfile(
        session: Session,
        indexer: NetworkIndexer,
    ): ProfileLoad =
        when (val result = getProfile(session, indexer)) {
            is AppResult.Failure -> ProfileLoad.Failed(result.error)
            is AppResult.Success -> if (result.value == null) {
                ProfileLoad.Hidden
            } else {
                ProfileLoad.Available(result.value)
            }
        }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val PROFILE_LOAD_CONCURRENCY = 4
    }
}

private sealed interface ProfileLoad {
    data class Available(val profile: IndexerSourceProfile) : ProfileLoad

    data object Hidden : ProfileLoad

    data class Failed(val error: AppError) : ProfileLoad
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
