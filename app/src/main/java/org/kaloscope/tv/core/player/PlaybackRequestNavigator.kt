package org.kaloscope.tv.core.player

object PlaybackRequestNavigator {
    fun hasPrevious(request: PlaybackRequest): Boolean =
        currentIndex(request)?.let { it > 0 } == true

    fun hasNext(request: PlaybackRequest): Boolean {
        val index = currentIndex(request) ?: return false
        val lastIndex = when (request) {
            is PlaybackRequest.LocalMedia -> request.siblings.lastIndex
            is PlaybackRequest.NetworkVideo -> request.source.chapters.lastIndex
        }
        return index < lastIndex
    }

    fun selectLocalAdjacent(
        request: PlaybackRequest.LocalMedia,
        offset: Int,
    ): PlaybackRequest.LocalMedia? {
        val current = request.siblings.indexOfFirst { it.mediaId == request.mediaId }
        if (current < 0) {
            return null
        }
        val target = request.siblings.getOrNull(current + offset) ?: return null
        return request.copy(
            mediaId = target.mediaId,
            path = target.path,
            title = target.title,
            seasonNumber = target.seasonNumber,
            episodeNumber = target.episodeNumber,
            resumePositionSeconds = 0,
        )
    }

    fun adjacentNetworkChapter(
        request: PlaybackRequest.NetworkVideo,
        offset: Int,
    ): Int? {
        val current = request.source.selectedChapterIndex ?: return null
        return (current + offset).takeIf(request.source.chapters.indices::contains)
    }

    fun selectDefinition(
        request: PlaybackRequest.NetworkVideo,
        definitionIndex: Int,
        positionMillis: Long,
    ): PlaybackRequest.NetworkVideo? {
        val definition = request.source.definitions.getOrNull(definitionIndex) ?: return null
        return request.copy(
            source = request.source.copy(
                url = definition.url,
                selectedDefinitionIndex = definitionIndex,
            ),
            resumePositionMillis = positionMillis.coerceAtLeast(0),
        )
    }

    private fun currentIndex(request: PlaybackRequest): Int? =
        when (request) {
            is PlaybackRequest.LocalMedia ->
                request.siblings.indexOfFirst { it.mediaId == request.mediaId }
                    .takeIf { it >= 0 }

            is PlaybackRequest.NetworkVideo -> request.source.selectedChapterIndex
        }
}
