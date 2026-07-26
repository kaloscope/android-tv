package org.kaloscope.tv.core.player

import androidx.media3.common.C
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleTrack

object SubtitleSelectionPolicy {
    fun preferredTrackId(
        tracks: List<SubtitleTrack>,
        settings: SubtitleSettings,
    ): String? {
        if (!settings.enabled || tracks.isEmpty()) {
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

    fun selectionFlags(
        trackId: String,
        selectedTrackId: String?,
    ): Int =
        if (trackId == selectedTrackId) {
            C.SELECTION_FLAG_DEFAULT
        } else {
            0
        }
}
