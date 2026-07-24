package org.kaloscope.tv.data.search

import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.NetworkChapter
import org.kaloscope.tv.core.model.NetworkDefinition
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.player.TranscodeResolution
import org.kaloscope.tv.data.search.remote.IndexerPageData
import org.kaloscope.tv.data.search.remote.IndexerResourceData
import org.kaloscope.tv.data.search.remote.IndexerResourcePageData

internal fun IndexerPageData.toModels(): List<NetworkIndexer> =
    items.mapNotNull { indexer ->
        val name = indexer.name.clean()
        if (indexer.id <= 0 || name == null || "search_start" !in indexer.nodeTypes) {
            null
        } else {
            NetworkIndexer(
                id = indexer.id,
                name = name,
                iconPath = indexer.icon.clean(),
            )
        }
    }

internal fun IndexerResourcePageData.toModel(
    pageNumber: Int,
    pageSize: Int,
): NetworkSearchPage {
    val visibleItems = items.mapNotNull(IndexerResourceData::toSearchResult)
    val hasNext = when {
        total != null -> pageNumber * pageSize < total
        totalPages != null -> pageNumber < totalPages
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
): NetworkPlaybackSource? {
    val resolvedId = id.clean() ?: return null
    if (!mediaType.isVideo()) {
        return null
    }
    val mappedDefinitions = definitions.orEmpty().mapNotNull { definition ->
        val definitionUrl = definition.url.clean()
        val label = definition.definition
            ?.jsonPrimitive
            ?.contentOrNull
            .clean()
        if (definitionUrl == null || label == null) {
            null
        } else {
            NetworkDefinition(label = label, url = definitionUrl)
        }
    }
    val selectedDefinitionIndex = mappedDefinitions
        .indexOfFirst { it.label.matches(preferredDefinition) }
        .takeIf { it >= 0 }
        ?: mappedDefinitions.indices.firstOrNull()
    val mappedChapters = toChapters()
    // Definitions override the generic URL because they carry the preferred quality.
    val sourceUrl = selectedDefinitionIndex
        ?.let(mappedDefinitions::get)
        ?.url
        ?: url.clean()
        ?: mappedChapters.firstOrNull()?.url
        ?: return null
    return NetworkPlaybackSource(
        indexerId = indexerId,
        resourceId = resolvedId,
        title = title.clean() ?: fallbackTitle.trim(),
        url = sourceUrl,
        videoType = when (videoType?.lowercase()) {
            "hls", "m3u8" -> NetworkVideoType.Hls
            "dash", "mpd" -> NetworkVideoType.Dash
            "mp4" -> NetworkVideoType.Mp4
            else -> NetworkVideoType.Unknown
        },
        danmakus = danmakus.orEmpty().mapNotNull { comment ->
            val text = comment.text.clean()
            val start = comment.start
            if (text == null || start == null || start < 0) {
                null
            } else {
                DanmakuComment(
                    id = comment.id,
                    text = text,
                    mode = comment.mode ?: "scroll",
                    color = comment.color,
                    startMillis = start,
                )
            }
        },
        definitions = mappedDefinitions,
        chapters = mappedChapters,
        selectedDefinitionIndex = selectedDefinitionIndex,
        selectedChapterIndex = mappedChapters.indices.firstOrNull(),
    )
}

internal fun IndexerResourceData.toChapters(): List<NetworkChapter> =
    chapters.orEmpty().mapNotNull { chapter ->
        val chapterId = chapter.id.clean()
        val chapterUrl = chapter.url.clean()
        val chapterTitle = chapter.title.clean() ?: chapter.volume.clean()
        if ((chapterId == null && chapterUrl == null) || chapterTitle == null) {
            null
        } else {
            NetworkChapter(
                id = chapterId,
                url = chapterUrl,
                title = chapterTitle,
                volume = chapter.volume.clean(),
            )
        }
    }

private fun IndexerResourceData.toSearchResult(): NetworkSearchResult? {
    val resolvedId = id.clean() ?: return null
    val resolvedTitle = title.clean() ?: return null
    if (!mediaType.isVideo()) {
        return null
    }
    return NetworkSearchResult(
        id = resolvedId,
        title = resolvedTitle,
        coverPath = cover.clean(),
        rating = rating?.jsonPrimitive?.doubleOrNull,
        category = category.clean(),
        uploader = uploader.clean(),
        uploadedAt = uploadedAt.clean(),
    )
}

private fun String?.isVideo(): Boolean =
    this == null || equals("video", ignoreCase = true)

private fun String?.clean(): String? =
    this?.trim()?.takeIf(String::isNotEmpty)

private fun String.matches(resolution: TranscodeResolution): Boolean {
    val normalized = lowercase().filter(Char::isLetterOrDigit)
    return when (resolution) {
        TranscodeResolution.Original ->
            normalized in setOf("original", "source", "originalquality") || contains("原画")

        else -> resolution.queryValue.filter(Char::isDigit) in normalized
    }
}
