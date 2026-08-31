package org.kaloscope.tv.feature.player

import org.kaloscope.tv.core.player.PlaybackRequest

internal data class PlayerEpisodeEntry(
    val stableId: String,
    val sourceIndex: Int,
    val title: String,
    val posterPath: String?,
    val showPoster: Boolean,
    val selected: Boolean,
)

internal object PlayerEpisodePresentation {
    fun entries(request: PlaybackRequest): List<PlayerEpisodeEntry> =
        when (request) {
            is PlaybackRequest.LocalMedia -> request.siblings.mapIndexed { index, episode ->
                PlayerEpisodeEntry(
                    stableId = "local:${episode.mediaId}",
                    sourceIndex = index,
                    title = episode.title,
                    posterPath = episode.posterPath,
                    showPoster = true,
                    selected = episode.mediaId == request.mediaId,
                )
            }

            is PlaybackRequest.NetworkVideo ->
                request.source.chapters.mapIndexed { index, chapter ->
                    PlayerEpisodeEntry(
                        stableId = "network:${chapter.id.orEmpty()}:$index",
                        sourceIndex = index,
                        title = chapter.title,
                        posterPath = null,
                        showPoster = false,
                        selected = index == request.source.selectedChapterIndex,
                    )
                }
        }
}
