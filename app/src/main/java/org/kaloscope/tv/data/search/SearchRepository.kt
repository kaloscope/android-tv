package org.kaloscope.tv.data.search

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.player.TranscodeResolution

interface SearchRepository {
    suspend fun getAvailableProfiles(
        session: Session,
    ): AppResult<List<IndexerSourceProfile>>

    suspend fun search(
        session: Session,
        profile: IndexerSourceProfile,
        keyword: String,
        filters: Map<String, SearchFilterValue>,
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
