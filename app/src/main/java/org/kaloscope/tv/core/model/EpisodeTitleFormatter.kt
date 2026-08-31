package org.kaloscope.tv.core.model

internal fun formatEpisodeDisplayTitle(
    title: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
): String {
    val trimmedTitle = title.trim()
    val season = seasonNumber ?: return trimmedTitle
    val episode = episodeNumber ?: return trimmedTitle
    val prefix = "S${season}E${episode}"
    val deduplicatedTitle = trimmedTitle.removeMatchingEpisodePrefixes(season, episode)
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
