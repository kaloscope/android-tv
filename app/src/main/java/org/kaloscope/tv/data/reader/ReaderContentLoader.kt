package org.kaloscope.tv.data.reader

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.reader.ReaderRequest

interface ReaderContentLoader {
    suspend fun openLocal(session: Session, request: ReaderRequest.Local): AppResult<ReaderContent>

    suspend fun refreshLocal(session: Session, content: ReaderContent): AppResult<ReaderContent>

    suspend fun recordProgress(
        session: Session,
        content: ReaderContent,
        position: ReaderPosition,
    ): AppResult<Unit>

    suspend fun resolveChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent>

    suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
        previous: Boolean = false,
    ): AppResult<ReaderImagePage>
}
