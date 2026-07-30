package org.kaloscope.tv.data.search

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.kaloscope.tv.core.model.IndexerSourceProfile
import org.kaloscope.tv.core.model.SearchFilterDefinition
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.SearchFilterValue

internal fun buildIndexerSearchRequest(
    profile: IndexerSourceProfile,
    keyword: String,
    filters: Map<String, SearchFilterValue>,
    pageNumber: Int,
): JsonObject = buildJsonObject {
    put("\$start", "search_start")
    put("page_num", pageNumber)
    put("page_size", profile.pageSize)
    put("keyword", keyword.trim())
    put("mobile", false)
    for (definition in profile.filters) {
        if (definition.key in BASE_SEARCH_KEYS) {
            continue
        }
        when (val value = filters[definition.key]) {
            is SearchFilterValue.Scalar ->
                value.validScalar(definition)?.let { put(definition.key, it) }

            is SearchFilterValue.Multiple ->
                value.validValues(definition).takeIf { it.isNotEmpty() }?.let { values ->
                    put(definition.key, JsonArray(values.map(::JsonPrimitive)))
                }

            null -> Unit
        }
    }
}

private fun SearchFilterValue.Scalar.validScalar(
    definition: SearchFilterDefinition,
): String? {
    val cleaned = value.trim().takeIf(String::isNotEmpty) ?: return null
    return when (definition.type) {
        SearchFilterType.Text,
        SearchFilterType.DateTime,
        -> cleaned

        SearchFilterType.Radio,
        SearchFilterType.Select,
        -> cleaned.takeIf { selected ->
            definition.options.any { it.value == selected }
        }

        SearchFilterType.Checkbox -> null
    }
}

private fun SearchFilterValue.Multiple.validValues(
    definition: SearchFilterDefinition,
): List<String> {
    if (definition.type != SearchFilterType.Checkbox) {
        return emptyList()
    }
    val selected = values.map(String::trim).filter(String::isNotEmpty).toSet()
    return definition.options.map { it.value }.filter(selected::contains)
}

private val BASE_SEARCH_KEYS = setOf(
    "\$start",
    "page_num",
    "page_size",
    "keyword",
    "mobile",
)
