package org.kaloscope.tv.data.reader

import javax.inject.Inject
import javax.inject.Singleton
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.model.ReaderSource
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.reader.ReaderRequest
import org.kaloscope.tv.data.search.NetworkResourceRepository

@Singleton
class DefaultReaderContentLoader @Inject constructor(
    private val network: NetworkResourceRepository,
    private val local: LocalReaderRepository,
) : ReaderContentLoader {
    override suspend fun openLocal(session: Session, request: ReaderRequest.Local) = local.open(session, request)

    override suspend fun refreshLocal(session: Session, content: ReaderContent) = local.refresh(session, content)

    override suspend fun recordProgress(session: Session, content: ReaderContent, position: ReaderPosition) =
        local.recordProgress(session, content, position)

    override suspend fun resolveChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent> = when (content.source) {
        is ReaderSource.Local -> local.resolveChapter(session, content, chapterIndex)
        is ReaderSource.Network -> network.resolveReaderChapter(session, content, chapterIndex)
    }

    override suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
        previous: Boolean,
    ): AppResult<ReaderImagePage> = when (content.source) {
        is ReaderSource.Local -> local.imagePage(session, content, previous)
        is ReaderSource.Network -> network.loadImagePage(session, content)
    }
}
