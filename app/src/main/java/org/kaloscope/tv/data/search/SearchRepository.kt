package org.kaloscope.tv.data.search

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.Session

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
}
