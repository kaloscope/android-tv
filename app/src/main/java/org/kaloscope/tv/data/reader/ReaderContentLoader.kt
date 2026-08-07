package org.kaloscope.tv.data.reader

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.Session

interface ReaderContentLoader {
    suspend fun resolveChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent>

    suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
    ): AppResult<ReaderImagePage>
}
