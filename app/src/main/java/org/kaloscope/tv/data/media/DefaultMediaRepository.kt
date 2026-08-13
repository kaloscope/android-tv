package org.kaloscope.tv.data.media

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.common.trimmedOrNull
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.MediaProbe
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.core.network.ApiClientFactory
import org.kaloscope.tv.core.network.MediaResourceData
import org.kaloscope.tv.core.network.authorizationHeader
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
            api(session)
                .getMediaLibraries(session.authorizationHeader())
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
            api(session)
                .getMediaPage(
                    authorization = session.authorizationHeader(),
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    libraryId = libraryId,
                    keyword = keyword.trimmedOrNull(),
                )
                .dataOrThrow()
                .toModel(pageNumber, pageSize)
        }

    override suspend fun getMediaDetail(
        session: Session,
        mediaId: Long,
    ): AppResult<MediaDetail> =
        networkCall(json) {
            api(session)
                .getMediaDetail(session.authorizationHeader(), mediaId)
                .dataOrThrow()
                .toDetail()
                ?: throw SerializationException("Invalid media detail")
        }

    override suspend fun getMediaProbe(
        session: Session,
        path: String,
    ): AppResult<MediaProbe> =
        networkCall(json) {
            api(session)
                .getMediaProbe(
                    authorization = session.authorizationHeader(),
                    path = path,
                )
                .dataOrThrow()
                .toModel()
        }

    override suspend fun getSubtitleTracks(
        session: Session,
        path: String,
    ): AppResult<List<SubtitleTrack>> =
        networkCall(json) {
            api(session)
                .getSubtitleTracks(
                    authorization = session.authorizationHeader(),
                    body = MediaResourceData(path),
                )
                .dataOrThrow()
                .mapNotNull { track ->
                    val url = track.url?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                    val label = track.label.trim().ifBlank { track.id.trim() }
                    if (track.id.isBlank() || label.isBlank()) {
                        null
                    } else {
                        SubtitleTrack(
                            id = track.id,
                            label = label,
                            url = url,
                            language = track.language?.takeIf(String::isNotBlank),
                        )
                    }
                }
        }

    override suspend fun getDanmakus(
        session: Session,
        path: String,
    ): AppResult<List<DanmakuComment>> =
        networkCall(json) {
            api(session)
                .getDanmakus(
                    authorization = session.authorizationHeader(),
                    body = MediaResourceData(path),
                )
                .dataOrThrow()
                .comments
                .mapNotNull { comment ->
                    val text = comment.text.trim()
                    val start = comment.start
                    if (text.isBlank() || start == null || start < 0) {
                        null
                    } else {
                        DanmakuComment(
                            id = comment.id,
                            text = text,
                            mode = comment.mode ?: "scroll",
                            color = comment.color,
                            startMillis = start,
                        )
                    }
                }
        }

    private fun api(session: Session) = apiClientFactory.create(session.server.origin)
}
