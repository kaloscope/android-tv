package org.kaloscope.tv.test

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.data.reader.ReaderContentLoader

open class StubReaderContentLoader : ReaderContentLoader {
    override suspend fun openLocal(session: Session, request: ReaderRequest.Local): AppResult<ReaderContent> =
        error("Unexpected local reading request")

    override suspend fun refreshLocal(session: Session, content: ReaderContent): AppResult<ReaderContent> =
        error("Unexpected local content refresh")

    override suspend fun recordProgress(session: Session, content: ReaderContent, position: ReaderPosition): AppResult<Unit> =
        error("Unexpected reading progress")

    override suspend fun resolveChapter(session: Session, content: ReaderContent, chapterIndex: Int): AppResult<ReaderContent> =
        error("Unexpected chapter request")

    override suspend fun loadImagePage(session: Session, content: ReaderImageContent, previous: Boolean): AppResult<ReaderImagePage> =
        error("Unexpected image page request")
}
