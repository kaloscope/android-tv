package org.kaloscope.tv.feature.detail

import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.formatEpisodeDisplayTitle

internal enum class MediaChildSectionKind {
    Episodes,
    Parts,
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

internal fun childSectionKind(parent: MediaDetail): MediaChildSectionKind =
    if (parent.library?.type == MediaLibraryType.TvShow) {
        MediaChildSectionKind.Episodes
    } else {
        MediaChildSectionKind.Parts
    }

internal fun mediaChildDisplayTitle(child: MediaSummary): String = formatEpisodeDisplayTitle(
    title = child.title,
    seasonNumber = child.season,
    episodeNumber = child.episode,
)
