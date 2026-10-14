package org.kaloscope.tv.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.NetworkChapter
import org.kaloscope.tv.core.model.NetworkPlaybackSource
import org.kaloscope.tv.core.model.NetworkVideoType
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.ReaderSource
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryItemsState
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.player.PlayerUiState
import org.kaloscope.tv.feature.reader.ReaderUiState
import org.kaloscope.tv.feature.search.SearchResultsState
import org.kaloscope.tv.feature.search.SearchUiState

class RootStateInspectionTest {
    @Test
    fun `reader progress authorization failures reach root session handling`() {
        val source = ReaderSource.Local(1, 1, "chapter", "version", "txt")
        val text = ReaderUiState.Text(
            "request", "server", ReaderTextContent(source, "Book", "Text"),
            TextReaderSettings(), ReaderChapterOrder.Ascending,
        )
        val image = ReaderUiState.Image(
            "request", "server", ReaderImageContent(source, "Comic", emptyList(), 0),
            ImageReaderSettings(), ReaderChapterOrder.Ascending,
        )
        assertFalse(ReaderUiState.Loading("request", "server").hasUnauthorized())
        for (error in listOf(AppError.Unauthorized, AppError.Forbidden, AppError.Offline)) {
            assertEquals(error == AppError.Unauthorized, text.copy(progressError = error).hasUnauthorized())
            assertEquals(error == AppError.Unauthorized, image.copy(progressError = error).hasUnauthorized())
        }
    }

    @Test
    fun `library invalidates the session only for catalog or page authorization failures`() {
        val content = LibraryUiState.Content(libraries = emptyList(), selectedLibraryId = 21)
        val page = LibraryItemsState.Content(
            items = emptyList(),
            total = 0,
            pageNumber = 1,
            hasNext = false,
        )

        assertFalse(LibraryUiState.Loading.hasUnauthorized())
        assertFalse(LibraryUiState.EmptyLibraries.hasUnauthorized())
        assertFalse(content.hasUnauthorized())
        assertFalse(content.copy(items = LibraryItemsState.Empty).hasUnauthorized())

        for ((error, invalidatesSession) in listOf(
            null to false,
            AppError.Unauthorized to true,
            AppError.Forbidden to false,
            AppError.Offline to false,
            AppError.Timeout to false,
        )) {
            assertEquals(
                invalidatesSession,
                content.copy(items = page.copy(loadMoreError = error)).hasUnauthorized(),
            )
            if (error != null) {
                assertEquals(invalidatesSession, LibraryUiState.Error(error).hasUnauthorized())
                assertEquals(
                    invalidatesSession,
                    content.copy(items = LibraryItemsState.Error(error)).hasUnauthorized(),
                )
            }
        }
    }

    @Test
    fun `search inspects catalog page and resolution errors independently`() {
        val content = SearchUiState.Content(profiles = emptyList(), selectedIndexerId = 11)
        val page = SearchResultsState.Content(
            items = emptyList(),
            total = null,
            pageNumber = 1,
            hasNext = false,
        )

        assertFalse(SearchUiState.Loading.hasUnauthorized())
        assertFalse(SearchUiState.EmptyIndexers.hasUnauthorized())
        for (results in listOf(
            SearchResultsState.AwaitingQuery,
            SearchResultsState.Loading,
            SearchResultsState.Empty,
        )) {
            assertFalse(content.copy(results = results).hasUnauthorized())
            assertTrue(
                content.copy(results = results, resolutionError = AppError.Unauthorized)
                    .hasUnauthorized(),
            )
        }

        for ((error, invalidatesSession) in listOf(
            null to false,
            AppError.Unauthorized to true,
            AppError.Forbidden to false,
            AppError.Offline to false,
            AppError.Timeout to false,
        )) {
            val pageError = content.copy(results = page.copy(loadMoreError = error))
            assertEquals(invalidatesSession, pageError.hasUnauthorized())
            assertTrue(pageError.copy(resolutionError = AppError.Unauthorized).hasUnauthorized())
            assertEquals(
                invalidatesSession,
                content.copy(resolutionError = error).hasUnauthorized(),
            )
            assertTrue(
                content.copy(
                    results = SearchResultsState.Error(AppError.Unauthorized),
                    resolutionError = error,
                ).hasUnauthorized(),
            )
            if (error != null) {
                assertEquals(invalidatesSession, SearchUiState.Error(error).hasUnauthorized())
                assertEquals(
                    invalidatesSession,
                    content.copy(results = SearchResultsState.Error(error)).hasUnauthorized(),
                )
            }
        }
    }

    @Test
    fun `child detail authorization failure invalidates the ready session`() {
        val content = MediaDetailUiState.Content(
            parent = detail(),
            childDetailError = AppError.Unauthorized,
        )

        assertTrue(content.hasUnauthorized())
        assertFalse(content.copy(childDetailError = AppError.Offline).hasUnauthorized())
    }

    @Test
    fun `home refresh authorization failure invalidates the ready session`() {
        val content = HomeUiState.Content(
            items = emptyList(),
            refreshError = AppError.Unauthorized,
        )

        assertTrue(content.hasUnauthorized())
        assertFalse(content.copy(refreshError = AppError.Offline).hasUnauthorized())
    }

    @Test
    fun `progress authorization failure invalidates the session while player prepares`() {
        val loading = PlayerUiState.Loading(progressError = AppError.Unauthorized)

        assertTrue(loading.hasUnauthorized())
        for (error in listOf(null, AppError.Forbidden, AppError.Offline, AppError.Timeout)) {
            assertFalse(loading.copy(progressError = error).hasUnauthorized())
        }
    }

    @Test
    fun `network episode authorization failure invalidates the ready session`() {
        val content = networkPlayerContent().copy(switchError = AppError.Unauthorized)

        assertTrue(content.hasUnauthorized())
    }

    @Test
    fun `network episode failures unrelated to authentication preserve the ready session`() {
        val content = networkPlayerContent()
        assertFalse(content.hasUnauthorized())

        for (error in listOf(AppError.Forbidden, AppError.Offline, AppError.Timeout)) {
            assertFalse(
                "Episode error $error must not invalidate the session",
                content.copy(switchError = error).hasUnauthorized(),
            )
        }
    }
}

private fun networkPlayerContent() = PlayerUiState.Content(
    request = PlaybackRequest.NetworkVideo(
        requestId = "request-id",
        serverId = "server-id",
        title = "Episode 1",
        source = NetworkPlaybackSource(
            indexerId = 11,
            resourceId = "series-id",
            title = "Episode 1",
            url = "https://media.example/episode-1.m3u8",
            videoType = NetworkVideoType.Hls,
            danmakus = emptyList(),
            chapters = listOf(
                NetworkChapter("ep-1", null, "Episode 1", null),
                NetworkChapter("ep-2", null, "Episode 2", null),
            ),
            selectedChapterIndex = 0,
        ),
    ),
    subtitles = emptyList(),
    danmakus = emptyList(),
    extraFailures = emptyMap(),
)

private fun detail() = MediaDetail(
    id = 201,
    library = null,
    title = "群星档案",
    path = "/media/201",
    posterPath = null,
    backdropPath = null,
    year = 2026,
    rating = null,
    season = null,
    episode = null,
    aired = null,
    plot = "简介",
    genres = emptyList(),
    directors = emptyList(),
    writers = emptyList(),
    studios = emptyList(),
    actors = emptyList(),
    children = emptyList(),
)
