package org.kaloscope.tv.feature.detail

import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.formatEpisodeDisplayTitle

internal enum class MediaChildSectionKind {
    Episodes,
    Parts,
    Chapters,
}

internal fun resolveDetailBackdrop(
    parent: MediaDetail,
    focusedChild: MediaSummary?,
): String? = listOf(
    focusedChild?.backdropPath,
    parent.backdropPath,
    focusedChild?.posterPath,
    parent.posterPath,
).firstOrNull { !it.isNullOrBlank() }

internal fun resolveDetailPlot(
    parent: MediaDetail,
    focusedChildDetail: MediaDetail?,
): String? {
    val childPlot = focusedChildDetail?.plot?.takeIf(String::isNotBlank)
    return if (
        childSectionKind(parent) == MediaChildSectionKind.Episodes &&
        parent.children.isNotEmpty()
    ) {
        childPlot
    } else {
        childPlot ?: parent.plot?.takeIf(String::isNotBlank)
    }
}

internal fun childSectionKind(parent: MediaDetail): MediaChildSectionKind =
    when (parent.library?.type) {
        MediaLibraryType.TvShow -> MediaChildSectionKind.Episodes
        MediaLibraryType.Comic, MediaLibraryType.Novel -> MediaChildSectionKind.Chapters
        else -> MediaChildSectionKind.Parts
    }

internal fun mediaChildDisplayTitle(child: MediaSummary): String = formatEpisodeDisplayTitle(
    title = child.title,
    seasonNumber = child.season,
    episodeNumber = child.episode,
)
