package org.kaloscope.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.NetworkChapter
import org.kaloscope.tv.core.model.NetworkDefinition
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkVideoType

class PlaybackRequestNavigatorTest {
    @Test
    fun `local request exposes adjacent sibling and starts it from beginning`() {
        val request = localRequest()

        assertFalse(PlaybackRequestNavigator.hasPrevious(request))
        assertTrue(PlaybackRequestNavigator.hasNext(request))
        val next = PlaybackRequestNavigator.selectLocalAdjacent(request, 1)

        checkNotNull(next)
        assertEquals(302, next.mediaId)
        assertEquals("/episode-2.mkv", next.path)
        assertEquals(0L, next.resumePositionSeconds)
    }

    @Test
    fun `local request outside sibling list cannot select an adjacent episode`() {
        val request = localRequest().copy(mediaId = 999)

        assertEquals(null, PlaybackRequestNavigator.selectLocalAdjacent(request, 1))
    }

    @Test
    fun `network adjacent chapter reports target index`() {
        val request = networkRequest()

        assertFalse(PlaybackRequestNavigator.hasPrevious(request))
        assertTrue(PlaybackRequestNavigator.hasNext(request))
        assertEquals(1, PlaybackRequestNavigator.adjacentNetworkChapter(request, 1))
    }

    @Test
    fun `network definition selection keeps current playback position`() {
        val selected = PlaybackRequestNavigator.selectDefinition(
            request = networkRequest(),
            definitionIndex = 1,
            positionMillis = 48_500,
        )

        checkNotNull(selected)
        assertEquals("https://cdn.example/720.m3u8", selected.source.url)
        assertEquals(1, selected.source.selectedDefinitionIndex)
        assertEquals(48_500, selected.resumePositionMillis)
    }
}

private fun localRequest() = PlaybackRequest.LocalMedia(
    requestId = "request-1",
    serverId = "server-1",
    mediaId = 301,
    path = "/episode-1.mkv",
    title = "Episode 1",
    resumePositionSeconds = 20,
    origin = PlaybackOrigin.MediaDetail,
    siblings = listOf(
        LocalEpisodeRef(301, "/episode-1.mkv", "Episode 1"),
        LocalEpisodeRef(302, "/episode-2.mkv", "Episode 2"),
    ),
)

private fun networkRequest() = PlaybackRequest.NetworkVideo(
    requestId = "request-2",
    serverId = "server-1",
    title = "Episode 1",
    source = NetworkPlaybackSource(
        indexerId = 11,
        resourceId = "series-1",
        title = "Episode 1",
        url = "https://cdn.example/1080.m3u8",
        videoType = NetworkVideoType.Hls,
        danmakus = emptyList(),
        definitions = listOf(
            NetworkDefinition("1080P", "https://cdn.example/1080.m3u8"),
            NetworkDefinition("720P", "https://cdn.example/720.m3u8"),
        ),
        chapters = listOf(
            NetworkChapter("ep-1", null, "Episode 1", null),
            NetworkChapter("ep-2", null, "Episode 2", null),
        ),
        selectedDefinitionIndex = 0,
        selectedChapterIndex = 0,
    ),
)
