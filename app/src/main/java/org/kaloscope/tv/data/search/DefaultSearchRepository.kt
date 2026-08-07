package org.kaloscope.tv.data.search

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
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
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall
import org.kaloscope.tv.core.player.TranscodeResolution

@Singleton
class DefaultSearchRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val json: Json,
    private val networkResourceRepository: NetworkResourceRepository,
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
                mediaTypeHint = config.details?.specific?.mediaType.toNetworkMediaType(),
                videoTypeHint = config.details?.specific?.videoType.toNetworkVideoType(),
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
            ).dataOrThrow().toModel(
                pageNumber = pageNumber,
                pageSize = profile.pageSize,
                mediaTypeHint = profile.mediaTypeHint,
                videoTypeHint = profile.videoTypeHint,
            )
        }

    override suspend fun resolvePlayback(
        session: Session,
        indexerId: Long,
        result: NetworkSearchResult,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource> =
        when (
            val resolved = networkResourceRepository.resolveResource(
                session = session,
                indexerId = indexerId,
                result = result,
                preferredDefinition = preferredDefinition,
            )
        ) {
            is AppResult.Failure -> resolved
            is AppResult.Success -> {
                val video = resolved.value as? org.kaloscope.tv.core.model.ResolvedNetworkResource.Video
                if (video == null) {
                    AppResult.Failure(AppError.InvalidData("network_playback"))
                } else {
                    AppResult.Success(video.source)
                }
            }
        }

    override suspend fun resolveChapter(
        session: Session,
        source: NetworkPlaybackSource,
        chapterIndex: Int,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource> =
        networkResourceRepository.resolveVideoChapter(
            session = session,
            source = source,
            chapterIndex = chapterIndex,
            preferredDefinition = preferredDefinition,
        )

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
