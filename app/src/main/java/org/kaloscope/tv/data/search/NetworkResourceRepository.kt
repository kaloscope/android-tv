package org.kaloscope.tv.data.search

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderImagePage
import org.kaloscope.tv.core.model.ResolvedNetworkResource
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.player.TranscodeResolution

interface NetworkResourceRepository {
    suspend fun resolveResource(
        session: Session,
        indexerId: Long,
        result: NetworkSearchResult,
        preferredDefinition: TranscodeResolution,
    ): AppResult<ResolvedNetworkResource>

    suspend fun resolveVideoChapter(
        session: Session,
        source: NetworkPlaybackSource,
        chapterIndex: Int,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource>

    suspend fun resolveReaderChapter(
        session: Session,
        content: ReaderContent,
        chapterIndex: Int,
    ): AppResult<ReaderContent>

    suspend fun loadImagePage(
        session: Session,
        content: ReaderImageContent,
    ): AppResult<ReaderImagePage>
}
