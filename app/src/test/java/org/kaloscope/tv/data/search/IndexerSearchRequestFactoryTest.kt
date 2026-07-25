package org.kaloscope.tv.data.search

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.SearchFilterDefinition
import org.kaloscope.tv.core.model.SearchFilterOption
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.SearchFilterValue

class IndexerSearchRequestFactoryTest {
    @Test
    fun `request flattens scalar and multiple values after base fields`() {
        val body = buildIndexerSearchRequest(
            profile = profile(
                filters = listOf(
                    definition("region", SearchFilterType.Select, "cn", "jp"),
                    definition("genre", SearchFilterType.Checkbox, "sci-fi", "drama"),
                ),
            ),
            keyword = " 星际 ",
            filters = mapOf(
                "region" to SearchFilterValue.Scalar("cn"),
                "genre" to SearchFilterValue.Multiple(listOf("drama", "sci-fi", "drama")),
            ),
            pageNumber = 2,
        )

        assertEquals("search_start", body["\$start"]?.jsonPrimitive?.content)
        assertEquals(2, body["page_num"]?.jsonPrimitive?.int)
        assertEquals(20, body["page_size"]?.jsonPrimitive?.int)
        assertEquals("星际", body["keyword"]?.jsonPrimitive?.content)
        assertEquals("cn", body["region"]?.jsonPrimitive?.content)
        assertEquals(
            listOf("sci-fi", "drama"),
            body["genre"]?.jsonArray?.map { it.jsonPrimitive.content },
        )
        assertNull(body["filters"])
    }

    @Test
    fun `request removes empty unknown and invalid filter values`() {
        val body = buildIndexerSearchRequest(
            profile = profile(
                filters = listOf(
                    definition("region", SearchFilterType.Select, "cn"),
                    definition("genre", SearchFilterType.Checkbox, "drama"),
                    definition("alias", SearchFilterType.Text),
                ),
            ),
            keyword = "",
            filters = mapOf(
                "region" to SearchFilterValue.Scalar("unknown"),
                "genre" to SearchFilterValue.Multiple(listOf("unknown")),
                "alias" to SearchFilterValue.Scalar("  "),
                "other" to SearchFilterValue.Scalar("value"),
                "page_num" to SearchFilterValue.Scalar("99"),
            ),
            pageNumber = 1,
        )

        assertEquals(1, body["page_num"]?.jsonPrimitive?.int)
        assertNull(body["region"])
        assertNull(body["genre"])
        assertNull(body["alias"])
        assertNull(body["other"])
    }
}

private fun definition(
    key: String,
    type: SearchFilterType,
    vararg values: String,
) = SearchFilterDefinition(
    key = key,
    label = key,
    type = type,
    options = values.map { SearchFilterOption(value = it, label = it) },
)

private fun profile(
    filters: List<SearchFilterDefinition>,
) = IndexerSourceProfile(
    indexer = NetworkIndexer(11, "Preview", null),
    pageSize = 20,
    keywordRequired = true,
    filters = filters,
)
