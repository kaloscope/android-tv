package org.kaloscope.tv.data.media

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.dataOrThrow
import org.kaloscope.tv.core.network.networkCall

@Singleton
class DefaultMediaRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val json: Json,
) : MediaRepository {
    override suspend fun getLibraries(
        session: Session,
    ): AppResult<List<MediaLibrary>> =
        networkCall(json) {
            apiClientFactory.create(session.server.origin)
                .getMediaLibraries(session.authorization())
                .dataOrThrow()
                .filter { it.id > 0 && it.name.isNotBlank() }
                .map { it.toModel() }
        }

    override suspend fun getMediaPage(
        session: Session,
        libraryId: Long,
        pageNumber: Int,
        pageSize: Int,
        keyword: String?,
    ): AppResult<MediaPage> =
        networkCall(json) {
            apiClientFactory.create(session.server.origin)
                .getMediaPage(
                    authorization = session.authorization(),
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    libraryId = libraryId,
                    keyword = keyword?.trim()?.takeIf(String::isNotEmpty),
                )
                .dataOrThrow()
                .toModel(pageNumber, pageSize)
        }

    override suspend fun getMediaDetail(
        session: Session,
        mediaId: Long,
    ): AppResult<MediaDetail> =
        networkCall(json) {
            apiClientFactory.create(session.server.origin)
                .getMediaDetail(session.authorization(), mediaId)
                .dataOrThrow()
                .toDetail()
                ?: throw SerializationException("Invalid media detail")
        }

    private fun Session.authorization(): String = "Token $token"
}
