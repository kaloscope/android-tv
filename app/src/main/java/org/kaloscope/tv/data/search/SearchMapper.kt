package org.kaloscope.tv.data.search

import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.kaloscope.tv.core.model.DanmakuComment
import org.kaloscope.tv.core.model.NetworkIndexer
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkSearchPage
import org.kaloscope.tv.core.model.NetworkSearchResult
import org.kaloscope.tv.core.model.NetworkVideoType
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
): NetworkPlaybackSource? {
    val resolvedId = id.clean() ?: return null
    val sourceUrl = url.clean() ?: return null
    if (!mediaType.isVideo()) {
        return null
    }
    return NetworkPlaybackSource(
        indexerId = indexerId,
        resourceId = resolvedId,
        title = title.clean() ?: fallbackTitle.trim(),
        url = sourceUrl,
        videoType = when (videoType?.lowercase()) {
            "hls", "m3u8" -> NetworkVideoType.Hls
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
    )
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
