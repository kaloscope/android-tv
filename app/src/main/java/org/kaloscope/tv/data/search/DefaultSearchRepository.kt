package org.kaloscope.tv.data.search

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.Session
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
    ): AppResult<NetworkPlaybackSource> =
        networkCall(json) {
            api(session).executeIndexerDetails(
                authorization = session.authorization(),
                indexerId = indexerId,
                body = IndexerDetailsRequestData(resourceId = result.id),
            ).dataOrThrow()
                ?.toPlaybackSource(indexerId, result.title)
                ?: throw SerializationException("Missing playable network source")
        }

    private fun api(session: Session) = apiClientFactory.create(session.server.origin)

    private fun Session.authorization(): String = "Token $token"

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
