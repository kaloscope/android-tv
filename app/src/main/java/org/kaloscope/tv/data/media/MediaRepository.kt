package org.kaloscope.tv.data.media

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.Session

interface MediaRepository {
    suspend fun getLibraries(session: Session): AppResult<List<MediaLibrary>>

    suspend fun getMediaPage(
        session: Session,
        libraryId: Long,
        pageNumber: Int = 1,
        pageSize: Int = 20,
        keyword: String? = null,
    ): AppResult<MediaPage>

    suspend fun getMediaDetail(
        session: Session,
        mediaId: Long,
    ): AppResult<MediaDetail>
}
