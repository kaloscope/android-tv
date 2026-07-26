package org.kaloscope.tv.feature.detail

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaActor
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
    fun initialLoadingUsesSkeleton() {
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Loading,
                    resumePositionSeconds = null,
                    onBack = {},
                    onRetry = {},
                    onSelectChild = {},
                    onPlay = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("detail-loading-skeleton").assertExists()
    }

    @Test
    fun episodeCardShowsPosterMetadataAndKeepsInitialFocus() {
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

        composeRule.onNodeWithTag("episode-card-301").assertIsFocused()
        composeRule.onNodeWithText("第 1 集").assertExists()
        composeRule.onNodeWithText("2026-01-02").assertExists()
    }

    @Test
    fun castStripShowsOnlyEightNonInteractiveActors() {
        val actors = (1..10).map { index ->
            MediaActor("演员$index", "角色$index", null)
        }
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(movie(actors)),
                    resumePositionSeconds = null,
                    onBack = {},
                    onRetry = {},
                    onSelectChild = {},
                    onPlay = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("cast-strip").assertExists()
        composeRule.onAllNodesWithTag("cast-item-0").assertCountEquals(1)
        composeRule.onNodeWithText("演员8").assertExists()
        composeRule.onNodeWithText("演员9").assertDoesNotExist()
    }

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

private fun movie(actors: List<MediaActor> = emptyList()) = detail(
    id = 501,
    title = "航行日志",
    path = "/media/movie.mkv",
    children = emptyList(),
    actors = actors,
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
            aired = "2026-01-02",
        ),
    ),
)

private fun detail(
    id: Long,
    title: String,
    path: String,
    children: List<MediaSummary>,
    actors: List<MediaActor> = emptyList(),
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
    actors = actors,
    children = children,
)
