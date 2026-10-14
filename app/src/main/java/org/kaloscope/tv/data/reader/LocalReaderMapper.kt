package org.kaloscope.tv.data.reader

import kotlinx.serialization.SerializationException
import org.kaloscope.tv.core.model.ReaderChapter
import org.kaloscope.tv.core.model.ReaderContent
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderPosition
import org.kaloscope.tv.core.model.ReaderSource
import org.kaloscope.tv.core.model.ReaderTextBlock
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.ReaderTextRun
import org.kaloscope.tv.data.reader.remote.LocalContentData
import org.kaloscope.tv.data.reader.remote.LocalRunData
import org.kaloscope.tv.data.reader.remote.ReadingLocatorData

internal fun LocalContentData.toContent(
    mediaId: Long,
    workTitle: String,
    locator: ReadingLocatorData? = null,
): ReaderContent {
    if (
        itemId != mediaId || sourceItemId <= 0 || !VERSION.matches(version) ||
        chapters.isEmpty() || chapters.map { it.id }.distinct().size != chapters.size ||
        chapters.none { it.id == chapterId }
    ) invalidContent()
    val source = ReaderSource.Local(itemId, sourceItemId, chapterId, version, format)
    val directory = chapters.map {
        if (it.id.isBlank() || it.part < 1) invalidContent()
        ReaderChapter(it.id, it.title.orEmpty(), it.volume, it.part)
    }
    val selected = chapters.indexOfFirst { it.id == chapterId }
    val saved = locator?.takeIf { it.version == version }
    return when {
        mediaType == "image" && contentType == "images" && format in IMAGE_FORMATS -> {
            if (
                chapterId != "item:$sourceItemId" || imageCount < 0 || offset < 0 ||
                offset + images.size > imageCount || images.distinct().size != images.size ||
                (nextOffset != null && (nextOffset <= offset || nextOffset != offset + images.size || nextOffset >= imageCount)) ||
                (nextOffset == null && offset + images.size < imageCount)
            ) invalidContent()
            images.forEach { validateAsset(it, sourceItemId, version) }
            ReaderImageContent(
                source = source,
                title = workTitle,
                images = images,
                imageCount = imageCount,
                chapters = directory,
                selectedChapterIndex = selected,
                imageOffset = offset,
                nextOffset = nextOffset,
                initialPosition = ReaderPosition(
                    offset = saved?.takeIf { it.pageId == images.firstOrNull()?.assetId() }
                        ?.offset?.coerceIn(0.0, 1.0) ?: 0.0,
                ),
            )
        }
        mediaType == "text" && contentType in setOf("text", "blocks") -> {
            if (
                !ID.matches(chapterId) ||
                chapters.any { !ID.matches(it.id) } ||
                (contentType == "text" && format != "txt") ||
                (contentType == "blocks" && format != "epub")
            ) invalidContent()
            val mappedBlocks = blocks.map { block ->
                if (!ID.matches(block.id)) invalidContent()
                when (block.type) {
                    "paragraph", "heading", "quote" -> ReaderTextBlock.Text(
                        id = block.id,
                        runs = block.runs.map(LocalRunData::toRun),
                        headingLevel = block.level?.coerceIn(1, 6)
                            ?.takeIf { block.type == "heading" },
                        quote = block.type == "quote",
                    )
                    "list" -> ReaderTextBlock.ListBlock(
                        id = block.id,
                        items = block.items.map { it.map(LocalRunData::toRun) },
                        ordered = block.ordered,
                        start = block.start ?: 1,
                    )
                    "image" -> ReaderTextBlock.Image(
                        id = block.id,
                        url = block.url?.also { validateAsset(it, sourceItemId, version) },
                        alt = block.alt,
                    )
                    else -> invalidContent()
                }
            }
            if (mappedBlocks.map { it.id }.distinct().size != mappedBlocks.size) invalidContent()
            val savedChapter = saved?.takeIf { it.chapterId == chapterId }
            val savedIndex = if (format == "epub") {
                mappedBlocks.indexOfFirst { it.id == savedChapter?.blockId }
            } else {
                savedChapter?.paragraph ?: -1
            }
            val count = if (format == "epub") mappedBlocks.size else text.size
            ReaderTextContent(
                source = source,
                title = workTitle,
                text = text.joinToString("\n\n"),
                chapters = directory,
                selectedChapterIndex = selected,
                localParagraphs = text,
                blocks = mappedBlocks,
                initialPosition = if (savedIndex in 0 until count) {
                    ReaderPosition(savedIndex, savedChapter?.offset?.coerceIn(0.0, 1.0) ?: 0.0)
                } else ReaderPosition(),
            )
        }
        else -> invalidContent()
    }
}

internal fun String.assetId(): String = substringBefore('?').substringAfterLast('/')

private fun validateAsset(url: String, sourceId: Long, version: String) {
    // Local reading assets are application URLs, never filesystem paths or remote images.
    val id = url.assetId()
    if (!ID.matches(id) || url != "/_api/media/$sourceId/assets/$id?version=$version") invalidContent()
}

private fun LocalRunData.toRun() = ReaderTextRun(text, "strong" in marks, "em" in marks)
private fun invalidContent(): Nothing = throw SerializationException("Invalid local reading content")
private val ID = Regex("[0-9a-f]{32}")
private val VERSION = Regex("[0-9a-f]{64}")
private val IMAGE_FORMATS = setOf("dir", "cbz", "zip")
