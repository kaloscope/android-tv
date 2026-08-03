package org.kaloscope.tv.feature.player

import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleTrack
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.SubtitleSelectionPolicy

internal data class PlayerSessionSettingsState(
    val subtitleSettings: SubtitleSettings,
    val selectedSubtitleTrackId: String?,
    val rememberedSubtitleTrackId: String?,
    val danmakuSettings: DanmakuSettings,
)

internal data class PlayerRequestSessionState(
    val requestId: String,
    val sessionSettings: PlayerSessionSettingsState,
    val playbackSpeed: Float = 1f,
) {
    fun updateRequest(
        request: PlaybackRequest,
        tracks: List<SubtitleTrack>,
    ): PlayerRequestSessionState =
        if (request.requestId == requestId) {
            copy(
                sessionSettings = PlayerSessionSettingsPolicy.refreshTracks(
                    state = sessionSettings,
                    tracks = tracks,
                ),
            )
        } else {
            initial(request, tracks)
        }

    companion object {
        fun initial(
            request: PlaybackRequest,
            tracks: List<SubtitleTrack>,
        ): PlayerRequestSessionState = PlayerRequestSessionState(
            requestId = request.requestId,
            sessionSettings = PlayerSessionSettingsPolicy.initial(
                tracks = tracks,
                subtitleSettings = request.subtitleSettings,
                danmakuSettings = request.danmakuSettings,
            ),
        )
    }
}

internal object PlayerSessionSettingsPolicy {
    fun initial(
        tracks: List<SubtitleTrack>,
        subtitleSettings: SubtitleSettings,
        danmakuSettings: DanmakuSettings,
    ): PlayerSessionSettingsState {
        val remembered = SubtitleSelectionPolicy.preferredAvailableTrackId(
            tracks = tracks,
            settings = subtitleSettings,
        )
        return PlayerSessionSettingsState(
            subtitleSettings = subtitleSettings,
            selectedSubtitleTrackId = SubtitleSelectionPolicy.preferredTrackId(
                tracks = tracks,
                settings = subtitleSettings,
            ),
            rememberedSubtitleTrackId = remembered,
            danmakuSettings = danmakuSettings,
        )
    }

    fun refreshTracks(
        state: PlayerSessionSettingsState,
        tracks: List<SubtitleTrack>,
    ): PlayerSessionSettingsState {
        if (tracks.isEmpty()) {
            return state.copy(selectedSubtitleTrackId = null)
        }
        val restored = SubtitleSelectionPolicy.restoredTrackId(
            tracks = tracks,
            rememberedTrackId = state.rememberedSubtitleTrackId,
            settings = state.subtitleSettings,
        )
        return state.copy(
            selectedSubtitleTrackId = restored.takeIf {
                state.subtitleSettings.enabled
            },
            rememberedSubtitleTrackId = restored,
        )
    }

    fun toggleSubtitles(
        state: PlayerSessionSettingsState,
        tracks: List<SubtitleTrack>,
    ): PlayerSessionSettingsState {
        val active = state.selectedSubtitleTrackId
        if (active != null) {
            return state.copy(
                subtitleSettings = state.subtitleSettings.copy(enabled = false),
                selectedSubtitleTrackId = null,
                rememberedSubtitleTrackId = active,
            )
        }
        val restored = SubtitleSelectionPolicy.restoredTrackId(
            tracks = tracks,
            rememberedTrackId = state.rememberedSubtitleTrackId,
            settings = state.subtitleSettings,
        )
        return state.copy(
            subtitleSettings = state.subtitleSettings.copy(enabled = restored != null),
            selectedSubtitleTrackId = restored,
            rememberedSubtitleTrackId = restored,
        )
    }

    fun selectSubtitleTrack(
        state: PlayerSessionSettingsState,
        tracks: List<SubtitleTrack>,
        trackId: String,
    ): PlayerSessionSettingsState {
        if (tracks.none { it.id == trackId }) return state
        return state.copy(
            subtitleSettings = state.subtitleSettings.copy(enabled = true),
            selectedSubtitleTrackId = trackId,
            rememberedSubtitleTrackId = trackId,
        )
    }

    fun toggleDanmakus(
        state: PlayerSessionSettingsState,
    ): PlayerSessionSettingsState = state.copy(
        danmakuSettings = state.danmakuSettings.copy(
            enabled = !state.danmakuSettings.enabled,
        ),
    )
}
