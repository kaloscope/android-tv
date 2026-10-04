package org.kaloscope.tv.core.player

import androidx.media3.common.C
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleTrack

object SubtitleSelectionPolicy {
    fun preferredTrackId(
        tracks: List<SubtitleTrack>,
        settings: SubtitleSettings,
    ): String? =
        if (settings.enabled) preferredAvailableTrackId(tracks, settings) else null

    fun preferredAvailableTrackId(
        tracks: List<SubtitleTrack>,
        settings: SubtitleSettings,
    ): String? {
        if (tracks.isEmpty()) {
            return null
        }
        val expression = settings.languagePreference.trim()
        if (expression.isBlank()) {
            return tracks.first().id
        }
        val regex = runCatching { Regex(expression, RegexOption.IGNORE_CASE) }
            .getOrNull()
            ?: return tracks.first().id
        return tracks.firstOrNull { track ->
            regex.containsMatchIn(track.language.orEmpty()) ||
            regex.containsMatchIn(track.label)
        }?.id ?: tracks.first().id
    }

    fun restoredTrackId(
        tracks: List<SubtitleTrack>,
        rememberedTrackId: String?,
        settings: SubtitleSettings,
    ): String? =
        rememberedTrackId
            ?.takeIf { candidate -> tracks.any { it.id == candidate } }
            ?: preferredAvailableTrackId(tracks, settings)

    fun selectionFlags(
        trackId: String,
        selectedTrackId: String?,
    ): Int =
        if (trackId == selectedTrackId) {
            C.SELECTION_FLAG_DEFAULT
        } else {
            0
        }

    fun matchesMedia3TrackId(formatId: String?, trackId: String, subtitleIndex: Int): Boolean =
        // DefaultMediaSourceFactory merges external subtitles after source 0 (the main media).
        // Match the full prefixed ID so an ID containing ':' cannot select another subtitle.
        subtitleIndex >= 0 && formatId == "${subtitleIndex + 1}:$trackId"
}
