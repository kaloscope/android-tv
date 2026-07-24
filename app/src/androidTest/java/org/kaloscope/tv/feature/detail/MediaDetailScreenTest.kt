package org.kaloscope.tv.feature.detail

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser

class MediaDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun moviePlayButtonStartsTheDisplayedMedia() {
        var playedId: Long? = null
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(movie()),
                    resumePositionSeconds = null,
                    onBack = {},
                    onRetry = {},
                    onSelectChild = {},
                    onPlay = { detail, _ -> playedId = detail.id },
                )
            }
        }

        composeRule.onNodeWithText("播放")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(501L, playedId)
        }
    }

    @Test
    fun seriesRequiresAnEpisodeBeforeShowingPlaybackActions() {
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(series()),
                    resumePositionSeconds = null,
                    onBack = {},
                    onRetry = {},
                    onSelectChild = {},
                    onPlay = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("播放").assertDoesNotExist()
        composeRule.onNodeWithText("启程").assertIsFocused()
    }
}

private fun session() = Session(
    server = SavedServer("server-id", "Home", "http://127.0.0.1:8000"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)

private fun movie() = detail(
    id = 501,
    title = "航行日志",
    path = "/media/movie.mkv",
    children = emptyList(),
)

private fun series() = detail(
    id = 201,
    title = "群星档案",
    path = "/media/series",
    children = listOf(
        MediaSummary(
            id = 301,
            title = "启程",
            path = "/media/episode-1.mkv",
            posterPath = null,
            backdropPath = null,
            year = 2026,
            rating = 8.5,
            season = 1,
            episode = 1,
        ),
    ),
)

private fun detail(
    id: Long,
    title: String,
    path: String,
    children: List<MediaSummary>,
) = MediaDetail(
    id = id,
    library = MediaLibrary(21, "Library", MediaLibraryType.Movie),
    title = title,
    path = path,
    posterPath = null,
    backdropPath = null,
    year = 2026,
    rating = 8.5,
    season = null,
    episode = null,
    aired = null,
    plot = null,
    genres = emptyList(),
    directors = emptyList(),
    writers = emptyList(),
    studios = emptyList(),
    actors = emptyList(),
    children = children,
)
