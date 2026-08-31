package org.kaloscope.tv.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.NetworkChapter
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.player.LocalEpisodeRef
import org.kaloscope.tv.core.player.PlaybackOrigin
import org.kaloscope.tv.core.player.PlaybackRequest

class PlayerEpisodePresentationTest {
    @Test
    fun `local siblings use poster rows and mark the current media id`() {
        val entries = PlayerEpisodePresentation.entries(
            PlaybackRequest.LocalMedia(
                requestId = "request-local",
                serverId = "server-1",
                mediaId = 302,
                path = "/episode-2.mkv",
                title = "Episode 2",
                resumePositionSeconds = 0,
                origin = PlaybackOrigin.MediaDetail,
                siblings = listOf(
                    LocalEpisodeRef(
                        mediaId = 301,
                        path = "/episode-1.mkv",
                        title = "S03E04 水王级魔术师",
                        seasonNumber = 3,
                        episodeNumber = 4,
                        posterPath = "/posters/episode-1.webp",
                        aired = "2026-07-20",
                    ),
                    LocalEpisodeRef(
                        mediaId = 302,
                        path = "/episode-2.mkv",
                        title = "E5 - 庆祝",
                        seasonNumber = 3,
                        episodeNumber = 5,
                        posterPath = null,
                        aired = "2026-07-27",
                    ),
                ),
            ),
        )

        assertEquals(listOf(0, 1), entries.map { it.sourceIndex })
        assertEquals(listOf("local:301", "local:302"), entries.map { it.stableId })
        assertEquals(
            listOf("S3E4 - 水王级魔术师", "S3E5 - 庆祝"),
            entries.map { it.title },
        )
        assertEquals(
            listOf("/posters/episode-1.webp", null),
            entries.map { it.posterPath },
        )
        assertEquals(
            listOf("2026-07-20", "2026-07-27"),
            entries.map { it.supportingText },
        )
        assertEquals(listOf(false, true), entries.map { it.selected })
        assertTrue(entries.all { it.showPoster })
    }

    @Test
    fun `network chapters never reserve poster space and mark the selected index`() {
        val entries = PlayerEpisodePresentation.entries(
            PlaybackRequest.NetworkVideo(
                requestId = "request-network",
                serverId = "server-1",
                title = "Episode 1",
                source = NetworkPlaybackSource(
                    indexerId = 7,
                    resourceId = "series-1",
                    title = "Episode 1",
                    url = "https://cdn.example.test/episode-1.m3u8",
                    videoType = NetworkVideoType.Hls,
                    danmakus = emptyList(),
                    chapters = listOf(
                        NetworkChapter("episode-1", null, "Episode 1", null),
                        NetworkChapter("episode-2", null, "Episode 2", null),
                    ),
                    selectedChapterIndex = 0,
                ),
            ),
        )

        assertEquals(listOf(0, 1), entries.map { it.sourceIndex })
        assertEquals(
            listOf("network:episode-1:0", "network:episode-2:1"),
            entries.map { it.stableId },
        )
        assertEquals(listOf(true, false), entries.map { it.selected })
        assertTrue(entries.all { it.posterPath == null })
        assertFalse(entries.any { it.showPoster })
    }
}
