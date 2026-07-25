package org.kaloscope.tv.core.model

enum class SearchFilterType {
    Text,
    Radio,
    Checkbox,
    Select,
    DateTime,
}

data class SearchFilterOption(
    val value: String,
    val label: String,
)

data class SearchFilterDefinition(
    val key: String,
    val label: String,
    val type: SearchFilterType,
    val options: List<SearchFilterOption> = emptyList(),
)

sealed interface SearchFilterValue {
    data class Scalar(val value: String) : SearchFilterValue

    data class Multiple(val values: List<String>) : SearchFilterValue
}
