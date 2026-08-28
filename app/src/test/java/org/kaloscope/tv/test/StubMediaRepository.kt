package org.kaloscope.tv.test

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.MediaProbe
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.data.media.MediaRepository

/** Fail-fast base for focused repository fakes that override only expected calls. */
internal open class StubMediaRepository : MediaRepository {
    override suspend fun getLibraries(session: Session): AppResult<List<MediaLibrary>> =
        unexpectedCall("getLibraries")

    override suspend fun getMediaPage(
        session: Session,
        libraryId: Long,
        pageNumber: Int,
        pageSize: Int,
        keyword: String?,
    ): AppResult<MediaPage> = unexpectedCall("getMediaPage")

    override suspend fun getMediaDetail(
        session: Session,
        mediaId: Long,
    ): AppResult<MediaDetail> = unexpectedCall("getMediaDetail")

    override suspend fun getMediaProbe(
        session: Session,
        path: String,
    ): AppResult<MediaProbe> = unexpectedCall("getMediaProbe")

    override suspend fun getSubtitleTracks(
        session: Session,
        path: String,
    ): AppResult<List<SubtitleTrack>> = unexpectedCall("getSubtitleTracks")

    override suspend fun getDanmakus(
        session: Session,
        path: String,
    ): AppResult<List<DanmakuComment>> = unexpectedCall("getDanmakus")

    private fun unexpectedCall(method: String): Nothing =
        error("Unexpected MediaRepository.$method call")
}
