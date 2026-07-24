package org.kaloscope.tv.data.search

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.player.TranscodeResolution

interface SearchRepository {
    suspend fun getIndexers(session: Session): AppResult<List<NetworkIndexer>>

    suspend fun getProfile(
        session: Session,
        indexer: NetworkIndexer,
    ): AppResult<IndexerSourceProfile>

    suspend fun search(
        session: Session,
        profile: IndexerSourceProfile,
        keyword: String,
        pageNumber: Int,
    ): AppResult<NetworkSearchPage>

    suspend fun resolvePlayback(
        session: Session,
        indexerId: Long,
        result: NetworkSearchResult,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource>

    suspend fun resolveChapter(
        session: Session,
        source: NetworkPlaybackSource,
        chapterIndex: Int,
        preferredDefinition: TranscodeResolution,
    ): AppResult<NetworkPlaybackSource>
}
