package org.kaloscope.tv.data.media

import kotlin.math.roundToLong
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.kaloscope.tv.core.common.trimmedOrNull
import org.kaloscope.tv.core.model.MediaActor
import org.kaloscope.tv.core.model.MediaChapter
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaPage
import org.kaloscope.tv.core.model.MediaProbe
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.data.media.remote.MediaItemData
import org.kaloscope.tv.data.media.remote.MediaLibraryData
import org.kaloscope.tv.data.media.remote.MediaPageData
import org.kaloscope.tv.data.media.remote.MediaProbeData

internal fun MediaLibraryData.toModel(): MediaLibrary =
    MediaLibrary(
        id = id,
        name = name.trim(),
        type = when (libraryType) {
            "movie" -> MediaLibraryType.Movie
            "tv_show" -> MediaLibraryType.TvShow
            else -> MediaLibraryType.Unknown
        },
    )

internal fun MediaPageData.toModel(
    pageNumber: Int,
    pageSize: Int,
): MediaPage {
    if (total < 0 || pageNumber < 1 || pageSize < 1) {
        throw SerializationException("Invalid media pagination")
    }
    return MediaPage(
        items = items.mapNotNull(MediaItemData::toSummary),
        total = total,
        pageNumber = pageNumber,
        pageSize = pageSize,
        hasNext = pageNumber * pageSize < total,
    )
}

internal fun MediaProbeData.toModel(): MediaProbe {
    val validChapters = chapters
        .filter { chapter ->
            chapter.start.isFinite() &&
                chapter.end.isFinite() &&
                chapter.start >= 0.0 &&
                chapter.end > chapter.start
        }
        .sortedBy { it.start }
        .mapIndexed { index, chapter ->
            MediaChapter(
                id = chapter.id.trim().ifBlank { (index + 1).toString() },
                title = chapter.title.trim().ifBlank { "章节 ${index + 1}" },
                startMillis = chapter.start.toMillis(),
                endMillis = chapter.end.toMillis(),
            )
        }
    return MediaProbe(
        durationMillis = duration
            .takeIf { it.isFinite() && it >= 0.0 }
            ?.toMillis()
            ?: 0L,
        chapters = validChapters,
    )
}

internal fun MediaItemData.toDetail(): MediaDetail? {
    val resolvedTitle = displayTitle()
    if (id <= 0 || path.isBlank() || resolvedTitle.isBlank()) {
        return null
    }
    val detailMetadata = metadata
    // Backend children may include hidden files retained for administrative workflows.
    val visibleChildren = children
        .mapNotNull(MediaItemData::toSummary)
        .sortedWith(Comparator(::compareMediaSummaries))
    return MediaDetail(
        id = id,
        library = lib
            ?.takeIf { it.id > 0 && it.name.isNotBlank() }
            ?.toModel(),
        title = resolvedTitle,
        path = path,
        posterPath = poster.trimmedOrNull() ?: detailMetadata?.poster.trimmedOrNull(),
        backdropPath = backdrop.trimmedOrNull() ?: detailMetadata?.backdrop.trimmedOrNull(),
        year = year,
        rating = rating.asRating(),
        season = season,
        episode = episode,
        aired = aired.trimmedOrNull(),
        plot = detailMetadata?.plot.trimmedOrNull(),
        genres = detailMetadata?.genres.cleanValues(),
        directors = detailMetadata?.directors.cleanValues(),
        writers = detailMetadata?.writers.cleanValues(),
        studios = detailMetadata?.studios.cleanValues(),
        actors = detailMetadata?.actors
            .orEmpty()
            .mapNotNull { actor ->
                val actorName = actor.name.trimmedOrNull() ?: return@mapNotNull null
                MediaActor(
                    name = actorName,
                    role = actor.role.trimmedOrNull(),
                    thumbPath = actor.thumb.trimmedOrNull(),
                )
            },
        children = visibleChildren,
    )
}

private fun MediaItemData.toSummary(): MediaSummary? {
    val resolvedTitle = displayTitle()
    if (!visible || id <= 0 || path.isBlank() || resolvedTitle.isBlank()) {
        return null
    }
    return MediaSummary(
        id = id,
        title = resolvedTitle,
        path = path,
        posterPath = poster.trimmedOrNull(),
        backdropPath = backdrop.trimmedOrNull(),
        year = year,
        rating = rating.asRating(),
        season = season,
        episode = episode,
        aired = aired.trimmedOrNull(),
    )
}

private fun compareMediaSummaries(
    left: MediaSummary,
    right: MediaSummary,
): Int {
    val seasonComparison = compareValues(left.season ?: 0, right.season ?: 0)
    if (seasonComparison != 0) return seasonComparison
    val episodeComparison = compareValues(left.episode ?: 0, right.episode ?: 0)
    if (episodeComparison != 0) return episodeComparison
    return compareNaturalTitles(left.title, right.title)
}

private fun compareNaturalTitles(left: String, right: String): Int {
    var leftIndex = 0
    var rightIndex = 0
    while (leftIndex < left.length && rightIndex < right.length) {
        val leftChar = left[leftIndex]
        val rightChar = right[rightIndex]
        if (leftChar.isDigit() && rightChar.isDigit()) {
            val leftStart = leftIndex
            val rightStart = rightIndex
            while (leftIndex < left.length && left[leftIndex].isDigit()) leftIndex += 1
            while (rightIndex < right.length && right[rightIndex].isDigit()) rightIndex += 1
            val leftRun = left.substring(leftStart, leftIndex)
            val rightRun = right.substring(rightStart, rightIndex)
            val leftValue = leftRun.trimStart('0').ifEmpty { "0" }
            val rightValue = rightRun.trimStart('0').ifEmpty { "0" }
            val lengthComparison = compareValues(leftValue.length, rightValue.length)
            if (lengthComparison != 0) return lengthComparison
            val valueComparison = leftValue.compareTo(rightValue)
            if (valueComparison != 0) return valueComparison
            val runComparison = compareValues(leftRun.length, rightRun.length)
            if (runComparison != 0) return runComparison
        } else {
            val charComparison = leftChar.lowercaseChar().compareTo(rightChar.lowercaseChar())
            if (charComparison != 0) return charComparison
            leftIndex += 1
            rightIndex += 1
        }
    }
    val lengthComparison = compareValues(left.length, right.length)
    return if (lengthComparison != 0) lengthComparison else left.compareTo(right)
}

private fun MediaItemData.displayTitle(): String =
    title.trimmedOrNull() ?: name.trim()

private fun kotlinx.serialization.json.JsonElement?.asRating(): Double? =
    this?.jsonPrimitive?.doubleOrNull

private fun List<String>?.cleanValues(): List<String> =
    orEmpty().mapNotNull(String?::trimmedOrNull)

private fun Double.toMillis(): Long =
    (coerceAtMost(Long.MAX_VALUE / 1_000.0) * 1_000.0).roundToLong()
