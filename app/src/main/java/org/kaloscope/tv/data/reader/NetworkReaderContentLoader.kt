package org.kaloscope.tv.data.reader

import javax.inject.Inject
import javax.inject.Singleton
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.data.search.NetworkResourceRepository

@Singleton
class NetworkReaderContentLoader @Inject constructor(
    private val repository: NetworkResourceRepository,
) : ReaderContentLoader {
    override suspend fun resolveChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent> =
        repository.resolveReaderChapter(session, content, chapterIndex)

    override suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
    ): AppResult<ReaderImagePage> = repository.loadImagePage(session, content)
}
