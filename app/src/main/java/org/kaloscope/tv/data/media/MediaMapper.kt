package org.kaloscope.tv.data.media

import kotlin.math.roundToLong
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
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
        .sortedWith(
            compareBy<MediaSummary>(
                { it.season ?: 0 },
                { it.episode ?: 0 },
                { it.title.lowercase() },
            ),
        )
    return MediaDetail(
        id = id,
        library = lib
            ?.takeIf { it.id > 0 && it.name.isNotBlank() }
            ?.toModel(),
        title = resolvedTitle,
        path = path,
        posterPath = poster.nonBlankOrNull() ?: detailMetadata?.poster.nonBlankOrNull(),
        backdropPath = backdrop.nonBlankOrNull() ?: detailMetadata?.backdrop.nonBlankOrNull(),
        year = year,
        rating = rating.asRating(),
        season = season,
        episode = episode,
        aired = aired.nonBlankOrNull(),
        plot = detailMetadata?.plot.nonBlankOrNull(),
        genres = detailMetadata?.genres.cleanValues(),
        directors = detailMetadata?.directors.cleanValues(),
        writers = detailMetadata?.writers.cleanValues(),
        studios = detailMetadata?.studios.cleanValues(),
        actors = detailMetadata?.actors
            .orEmpty()
            .mapNotNull { actor ->
                val actorName = actor.name.nonBlankOrNull() ?: return@mapNotNull null
                MediaActor(
                    name = actorName,
                    role = actor.role.nonBlankOrNull(),
                    thumbPath = actor.thumb.nonBlankOrNull(),
                )
            },
        children = visibleChildren,
    )
}

private fun MediaItemData.toSummary(): MediaSummary? {
    val resolvedTitle = displayTitle()
    if (!visible || id <= 0 || resolvedTitle.isBlank()) {
        return null
    }
    return MediaSummary(
        id = id,
        title = resolvedTitle,
        path = path,
        posterPath = poster.nonBlankOrNull(),
        backdropPath = backdrop.nonBlankOrNull(),
        year = year,
        rating = rating.asRating(),
        season = season,
        episode = episode,
        aired = aired.nonBlankOrNull(),
    )
}

private fun MediaItemData.displayTitle(): String =
    title.nonBlankOrNull() ?: name.trim()

private fun kotlinx.serialization.json.JsonElement?.asRating(): Double? =
    this?.jsonPrimitive?.doubleOrNull

private fun String?.nonBlankOrNull(): String? =
    this?.trim()?.takeIf(String::isNotEmpty)

private fun List<String>?.cleanValues(): List<String> =
    orEmpty().mapNotNull(String?::nonBlankOrNull)

private fun Double.toMillis(): Long =
    (coerceAtMost(Long.MAX_VALUE / 1_000.0) * 1_000.0).roundToLong()
