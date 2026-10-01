package org.kaloscope.tv.data.search

import kotlin.math.roundToInt
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import org.kaloscope.tv.core.common.trimmedOrNull
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.NetworkChapter
import org.kaloscope.tv.core.model.NetworkDefinition
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkMediaType
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.SearchFilterDefinition
import org.kaloscope.tv.core.model.SearchFilterOption
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.player.NetworkDefinitionSelectionPolicy
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.data.search.remote.IndexerPageData
import org.kaloscope.tv.data.search.remote.IndexerResourceData
import org.kaloscope.tv.data.search.remote.IndexerResourcePageData
import org.kaloscope.tv.data.search.remote.IndexerSearchConfigData

internal fun IndexerPageData.toModels(): List<NetworkIndexer> =
    items.mapNotNull { indexer ->
        val name = indexer.name.trimmedOrNull()
        if (
            indexer.id <= 0 ||
            name == null ||
            "search_start" !in indexer.nodeTypes ||
            !indexer.onlyPreview
        ) {
            return@mapNotNull null
        }
        NetworkIndexer(
            id = indexer.id,
            name = name,
            iconPath = indexer.icon.trimmedOrNull(),
        )
    }

internal fun IndexerSearchConfigData.toFilterDefinitions(): List<SearchFilterDefinition> =
    filters.mapNotNull filter@{ (rawKey, data) ->
        val key = rawKey.trimmedOrNull() ?: return@filter null
        if (key in RESERVED_INDEXER_SEARCH_KEYS) {
            return@filter null
        }
        val type = when (data.type.trimmedOrNull()?.lowercase()) {
            "text" -> SearchFilterType.Text
            "radio" -> SearchFilterType.Radio
            "checkbox" -> SearchFilterType.Checkbox
            "select" -> SearchFilterType.Select
            "datetime" -> SearchFilterType.DateTime
            else -> return@filter null
        }
        val options = data.options.orEmpty().mapNotNull option@{ (rawValue, rawLabel) ->
            val value = rawValue.trimmedOrNull() ?: return@option null
            SearchFilterOption(
                value = value,
                label = rawLabel.trimmedOrNull() ?: value,
            )
        }
        if (type.requiresOptions() && options.isEmpty()) {
            return@filter null
        }
        SearchFilterDefinition(
            key = key,
            label = data.label.trimmedOrNull() ?: key,
            type = type,
            options = options,
        )
    }

internal fun IndexerResourcePageData.toModel(
    pageNumber: Int,
    pageSize: Int,
    mediaTypeHint: NetworkMediaType? = null,
    videoTypeHint: NetworkVideoType = NetworkVideoType.Unknown,
): NetworkSearchPage {
    val visibleItems = items.mapNotNull {
        it.toSearchResult(mediaTypeHint, videoTypeHint)
    }
    val hasNext = when {
        total != null -> pageNumber.toLong() * pageSize < total
        totalPages != null && totalPages != 0 -> pageNumber < totalPages
        else -> items.size >= pageSize
    }
    return NetworkSearchPage(
        items = visibleItems,
        total = total,
        pageNumber = pageNumber,
        pageSize = pageSize,
        hasNext = hasNext,
    )
}

internal fun IndexerResourceData.toPlaybackSource(
    indexerId: Long,
    fallbackTitle: String,
    preferredDefinition: TranscodeResolution,
    preferHevcForDash: Boolean = false,
    fallbackVideoType: NetworkVideoType = NetworkVideoType.Unknown,
): NetworkPlaybackSource? {
    val resolvedId = id.trimmedOrNull() ?: return null
    if (mediaType.resolveMediaType() != NetworkMediaType.Video) {
        return null
    }
    val mappedDefinitions = definitions.orEmpty().mapNotNull { definition ->
        val definitionUrl = definition.url.trimmedOrNull()
        val label = when (val value = definition.definition) {
            null -> null
            is JsonPrimitive -> value.contentOrNull
            else -> throw SerializationException("Invalid network definition label")
        }.trimmedOrNull()
        if (definitionUrl == null || label == null) {
            return@mapNotNull null
        }
        NetworkDefinition(label = label, url = definitionUrl)
    }
    val videoType = videoType.resolveVideoType(fallbackVideoType)
    val preferredDefinitionIndex = mappedDefinitions
        .indexOfFirst { it.label.matches(preferredDefinition) }
        .takeIf { it >= 0 }
    val selectedDefinitionIndex = NetworkDefinitionSelectionPolicy.selectIndex(
        definitions = mappedDefinitions,
        serverSelectedIndex = preferredDefinitionIndex,
        preferHevc = videoType == NetworkVideoType.Dash && preferHevcForDash,
    )
    val mappedChapters = toChapters()
    // Definitions override the generic URL because they carry the preferred quality.
    val resourceUrl = selectedDefinitionIndex
        ?.let(mappedDefinitions::get)
        ?.url
        ?: url.trimmedOrNull()
    val sourceUrl = resourceUrl
        ?: mappedChapters.firstOrNull()?.url
        ?: return null
    // Resolved content can identify a later episode; directory fallback still starts at the first.
    val selectedChapterIndex = mappedChapters.indexOfFirst { it.id == resolvedId }
        .takeIf { resourceUrl != null && it >= 0 }
        ?: mappedChapters.indices.firstOrNull()
    return NetworkPlaybackSource(
        indexerId = indexerId,
        resourceId = resolvedId,
        title = title.trimmedOrNull() ?: fallbackTitle.trim(),
        url = sourceUrl,
        videoType = videoType,
        danmakus = danmakus.orEmpty().mapNotNull { comment ->
            val text = comment.text.trimmedOrNull()
            val start = comment.start
            if (text == null || start == null || start < 0) {
                return@mapNotNull null
            }
            DanmakuComment(
                id = comment.id,
                text = text,
                mode = comment.mode ?: "scroll",
                color = comment.color,
                startMillis = start,
            )
        },
        definitions = mappedDefinitions,
        chapters = mappedChapters,
        selectedDefinitionIndex = selectedDefinitionIndex,
        selectedChapterIndex = selectedChapterIndex,
    )
}

internal fun IndexerResourceData.toChapters(): List<NetworkChapter> =
    chapters.orEmpty().mapNotNull { chapter ->
        val chapterId = chapter.id.trimmedOrNull()
        val chapterUrl = chapter.url.trimmedOrNull()
        val chapterTitle = chapter.title.trimmedOrNull() ?: chapter.volume.trimmedOrNull()
        if ((chapterId == null && chapterUrl == null) || chapterTitle == null) {
            return@mapNotNull null
        }
        NetworkChapter(
            id = chapterId,
            url = chapterUrl,
            title = chapterTitle,
            volume = chapter.volume.trimmedOrNull(),
        )
    }

private fun IndexerResourceData.toSearchResult(
    mediaTypeHint: NetworkMediaType?,
    videoTypeHint: NetworkVideoType,
): NetworkSearchResult? {
    val resolvedId = id.trimmedOrNull() ?: return null
    val resolvedTitle = title.trimmedOrNull() ?: return null
    val resolvedMediaType = mediaType.resolveMediaType(mediaTypeHint) ?: return null
    if (resolvedMediaType == NetworkMediaType.Audio) return null
    return NetworkSearchResult(
        id = resolvedId,
        title = resolvedTitle,
        coverPath = cover.trimmedOrNull(),
        rating = rating.toNumberOrNull(),
        category = category.trimmedOrNull(),
        uploader = uploader.trimmedOrNull(),
        uploadedAt = uploadedAt.trimmedOrNull(),
        ranking = ranking.toRankingOrNull(),
        misc = misc.trimmedOrNull(),
        size = size.trimmedOrNull(),
        mediaType = resolvedMediaType,
        videoTypeHint = videoType.resolveVideoType(videoTypeHint),
    )
}

internal fun IndexerResourceData.toTextBody(): String? =
    when (val value = text) {
        is JsonPrimitive -> value.contentOrNull
        is JsonArray -> value.map { element ->
            (element as? JsonPrimitive)?.contentOrNull ?: return null
        }.joinToString("\n\n")
        else -> null
    }

private fun JsonElement?.toNumberOrNull(): Double? =
    when (this) {
        null -> null
        is JsonPrimitive -> doubleOrNull
        else -> throw SerializationException("Invalid numeric search metadata")
    }

private fun JsonElement?.toRankingOrNull(): Int? {
    val value = toNumberOrNull() ?: return null
    return value.takeIf { it.isFinite() && it in 1.0..100.0 }?.roundToInt()
}

internal fun String?.toNetworkVideoType(): NetworkVideoType =
    when (trimmedOrNull()?.lowercase()) {
        "hls", "m3u8" -> NetworkVideoType.Hls
        "dash", "mpd" -> NetworkVideoType.Dash
        "mp4" -> NetworkVideoType.Mp4
        else -> NetworkVideoType.Unknown
    }

internal fun String?.resolveVideoType(fallback: NetworkVideoType): NetworkVideoType {
    // Missing types inherit the fallback; explicit unknown values must not.
    return if (trimmedOrNull() == null) fallback else toNetworkVideoType()
}

internal fun String?.toNetworkMediaType(): NetworkMediaType? =
    when (trimmedOrNull()?.lowercase()) {
        "video" -> NetworkMediaType.Video
        "audio" -> NetworkMediaType.Audio
        "image" -> NetworkMediaType.Image
        "text" -> NetworkMediaType.Text
        else -> null
    }

private fun String?.resolveMediaType(
    hint: NetworkMediaType? = null,
): NetworkMediaType? =
    if (trimmedOrNull() == null) {
        hint ?: NetworkMediaType.Video
    } else {
        toNetworkMediaType()
    }

private fun String.matches(resolution: TranscodeResolution): Boolean {
    val normalized = lowercase().filter(Char::isLetterOrDigit)
    return when (resolution) {
        TranscodeResolution.Original ->
            normalized in setOf("original", "source", "originalquality") || contains("原画")

        else -> resolution.queryValue.filter(Char::isDigit) in normalized
    }
}

private fun SearchFilterType.requiresOptions(): Boolean =
    this == SearchFilterType.Radio ||
        this == SearchFilterType.Checkbox ||
        this == SearchFilterType.Select
