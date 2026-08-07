package org.kaloscope.tv.feature.detail

import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary

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

internal fun mediaChildDisplayTitle(child: MediaSummary): String {
    val title = child.title.trim()
    val season = child.season ?: return title
    val episode = child.episode ?: return title
    val prefix = "S${season}E${episode}"
    val deduplicatedTitle = title.removeMatchingEpisodePrefixes(season, episode)
    return if (deduplicatedTitle.isBlank()) {
        prefix
    } else {
        "$prefix - $deduplicatedTitle"
    }
}

private fun String.removeMatchingEpisodePrefixes(
    season: Int,
    episode: Int,
): String {
    val matchingPrefix = Regex(
        pattern = """^(?:(?:S\s*0*$season\s*)?E(?:P(?:ISODE)?)?\s*0*$episode""" +
            """(?=$|[\s\-–—:：·])|第\s*0*$episode\s*[集话話回])""" +
            """\s*(?:[\-–—:：·]\s*)?""",
        option = RegexOption.IGNORE_CASE,
    )
    var remaining = trim()
    while (true) {
        val match = matchingPrefix.find(remaining) ?: return remaining
        val next = remaining.removeRange(match.range).trimStart()
        if (next == remaining) return remaining
        remaining = next
    }
}
