package org.kaloscope.tv.feature.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.GridViewportSnapshot
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
    fun initialLoadingUsesCenteredIndicator() {
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Loading,
                    resumePositionsByMediaId = emptyMap(),
                    onBack = {},
                    onRetry = {},
                    onChildFocused = {},
                    onChildViewportChanged = {},
                    onPlayParent = { _, _ -> },
                    onPlayChild = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("detail-loading-indicator").assertExists()
        composeRule.onNodeWithTag("detail-loading-skeleton").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("返回").assertExists()
    }

    @Test
    fun episodeCardShowsPosterMetadataAndKeepsInitialFocus() {
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(series()),
                    resumePositionsByMediaId = emptyMap(),
                    onBack = {},
                    onRetry = {},
                    onChildFocused = {},
                    onChildViewportChanged = {},
                    onPlayParent = { _, _ -> },
                    onPlayChild = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()
        composeRule.onNodeWithText("第 1 集").assertExists()
        composeRule.onNodeWithText("2026-01-02").assertExists()
    }

    @Test
    fun childrenFromDifferentSeasonsShareOneFlatRibbon() {
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(multiSeasonSeries()),
                    resumePositionsByMediaId = emptyMap(),
                    onBack = {},
                    onRetry = {},
                    onChildFocused = {},
                    onChildViewportChanged = {},
                    onPlayParent = { _, _ -> },
                    onPlayChild = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("media-child-card-300").assertExists()
        composeRule.onNodeWithTag("media-child-card-301").assertExists()
        composeRule.onNodeWithText("特别篇").assertDoesNotExist()
        composeRule.onNodeWithText("第 1 季").assertDoesNotExist()
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
                    resumePositionsByMediaId = emptyMap(),
                    onBack = {},
                    onRetry = {},
                    onChildFocused = {},
                    onChildViewportChanged = {},
                    onPlayParent = { _, _ -> },
                    onPlayChild = { _, _ -> },
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
                    resumePositionsByMediaId = emptyMap(),
                    onBack = {},
                    onRetry = {},
                    onChildFocused = {},
                    onChildViewportChanged = {},
                    onPlayParent = { detail, _ -> playedId = detail.id },
                    onPlayChild = { _, _ -> },
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
    fun backButtonRightAndDownReachPrimaryPlaybackAction() {
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(movie()),
                    resumePositionsByMediaId = emptyMap(),
                    onBack = {},
                    onRetry = {},
                    onChildFocused = {},
                    onChildViewportChanged = {},
                    onPlayParent = { _, _ -> },
                    onPlayChild = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("detail-back")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithText("播放").assertIsFocused()

        composeRule.onNodeWithTag("detail-back")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("播放").assertIsFocused()
    }

    @Test
    fun focusingBackAfterLowerContentScrollReturnsDetailToTop() {
        val state = MediaDetailUiState.Content(
            parent = movie(
                actors = listOf(MediaActor("沈川", "队长", null)),
            ).copy(
                directors = listOf("林舟"),
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
                    MediaDetailScreen(
                        session = session(),
                        state = state,
                        resumePositionsByMediaId = emptyMap(),
                        onBack = {},
                        onRetry = {},
                        onChildFocused = {},
                        onChildViewportChanged = {},
                        onPlayParent = { _, _ -> },
                        onPlayChild = { _, _ -> },
                    )
                }
            }
        }
        val poster = composeRule.onNodeWithTag("detail-parent-poster-501")
        val detailScroll = composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        )

        composeRule.onNodeWithText("播放").assertIsFocused()
        composeRule.onNodeWithTag("detail-credits-anchor")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        val scrolledOffset = detailScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()
        assertTrue(
            "Expected lower-content focus to scroll the detail content",
            scrolledOffset > 0f,
        )

        val back = composeRule.onNodeWithTag("detail-back")
        back.performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()

        composeRule.waitForIdle()
        val restoredOffset = detailScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()
        assertEquals(
            "Expected back focus to reset detail scroll from $scrolledOffset",
            0f,
            restoredOffset,
            0f,
        )
        val restoredPosterTop = poster.getUnclippedBoundsInRoot().top
        val backBottom = back.getUnclippedBoundsInRoot().bottom
        assertTrue(
            "Expected the restored poster to remain below the back button",
            backBottom <= restoredPosterTop,
        )
    }

    @Test
    fun remoteBackInvokesOnBack() {
        var backs = 0
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(movie()),
                    resumePositionsByMediaId = emptyMap(),
                    onBack = { backs += 1 },
                    onRetry = {},
                    onChildFocused = {},
                    onChildViewportChanged = {},
                    onPlayParent = { _, _ -> },
                    onPlayChild = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("播放")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Back) }

        composeRule.runOnIdle {
            assertEquals(1, backs)
        }
    }

    @Test
    fun errorRetryHandlesRemoteClick() {
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Error(AppError.Offline),
                    resumePositionsByMediaId = emptyMap(),
                    onBack = {},
                    onRetry = { retries += 1 },
                    onChildFocused = {},
                    onChildViewportChanged = {},
                    onPlayParent = { _, _ -> },
                    onPlayChild = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("返回").assertExists()
        composeRule.onNodeWithText("重试")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }

    @Test
    fun rightThenCenterPlaysNewChildWithItsResumePosition() {
        var playedId: Long? = null
        var resume: Long? = null
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = twoEpisodeSeries()),
            resumePositions = mapOf(301L to 42L, 302L to 84L),
            onPlayChild = { child, position ->
                playedId = child.id
                resume = position
            },
        )

        composeRule.onNodeWithTag("media-child-card-301")
            .assertIsFocused()
            .performKeyInput {
                keyDown(Key.DirectionRight)
                keyUp(Key.DirectionRight)
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }

        composeRule.runOnIdle {
            assertEquals(302L, playedId)
            assertEquals(84L, resume)
        }
    }

    @Test
    fun rightThenUpThenCenterPlaysTheNewChildFromHero() {
        var playedId: Long? = null
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = twoEpisodeSeries()),
            resumePositions = mapOf(301L to 42L, 302L to 84L),
            onPlayChild = { child, _ ->
                playedId = child.id
            },
        )

        composeRule.onNodeWithTag("media-child-card-301")
            .assertIsFocused()
            .performKeyInput {
                keyDown(Key.DirectionRight)
                keyUp(Key.DirectionRight)
                keyDown(Key.DirectionUp)
                keyUp(Key.DirectionUp)
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }

        composeRule.runOnIdle {
            assertEquals(302L, playedId)
        }
    }

    @Test
    fun focusedEpisodeOffersExplicitStartOver() {
        var resume: Long? = 99L
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = series(),
                focusedChildId = 301,
            ),
            resumePositions = mapOf(301L to 42L),
            onPlayChild = { _, position -> resume = position },
        )

        composeRule.onNodeWithText("从头播放")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(null, resume)
        }
    }

    @Test
    fun rightUpdatesChildPreviewWithoutReplacingParentPoster() {
        var plays = 0
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = twoEpisodeSeries()),
            onPlayChild = { _, _ -> plays += 1 },
        )

        composeRule.onNodeWithTag("media-child-card-301")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.runOnIdle {
            assertEquals(0, plays)
        }
        composeRule.onNodeWithText("S1E2 · 返程").assertExists()
        composeRule.onNodeWithText("S1E1 · 启程").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-parent-poster-201").assertExists()
    }

    @Test
    fun rememberedOffscreenChildIsComposedAndFocused() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = longSeries(),
                focusedChildId = 312,
                childViewport = GridViewportSnapshot(2, 18),
            ),
        )

        composeRule.onNodeWithTag("media-child-card-312")
            .assertExists()
            .assertIsFocused()
    }

    @Test
    fun invalidRememberedChildFallsBackToFirstChild() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = longSeries(),
                focusedChildId = 999,
                childViewport = GridViewportSnapshot(7, 0),
            ),
        )

        composeRule.onNodeWithTag("media-child-card-301")
            .assertExists()
            .assertIsFocused()
    }

    @Test
    fun downReachesCreditsAnchorAndUpReturnsToCurrentChild() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = twoEpisodeSeries().copy(
                    directors = listOf("林舟"),
                    actors = listOf(MediaActor("沈川", "队长", null)),
                ),
                focusedChildId = 302,
            ),
        )

        composeRule.onNodeWithTag("media-child-card-302")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithTag("detail-credits-anchor").assertIsFocused()
        composeRule.onNodeWithContentDescription("演职人员").assertIsFocused()

        composeRule.onNodeWithTag("detail-credits-anchor")
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.onNodeWithTag("media-child-card-302").assertIsFocused()
    }

    @Test
    fun tvShowUsesEpisodeSemanticsAndMovieCollectionUsesParts() {
        var state by mutableStateOf(MediaDetailUiState.Content(parent = series()))
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = state,
                    resumePositionsByMediaId = emptyMap(),
                    onBack = {},
                    onRetry = {},
                    onChildFocused = {},
                    onChildViewportChanged = {},
                    onPlayParent = { _, _ -> },
                    onPlayChild = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("分集").assertExists()
        composeRule.onNodeWithText("第 1 集").assertExists()

        composeRule.runOnIdle {
            state = MediaDetailUiState.Content(parent = movieCollection())
        }
        composeRule.onNodeWithText("分段").assertExists()
        composeRule.onNodeWithText("第 1 集").assertDoesNotExist()
    }

    private fun setStatefulDetailContent(
        initialState: MediaDetailUiState.Content,
        resumePositions: Map<Long, Long> = emptyMap(),
        onBack: () -> Unit = {},
        onPlayParent: (MediaDetail, Long?) -> Unit = { _, _ -> },
        onPlayChild: (MediaSummary, Long?) -> Unit = { _, _ -> },
    ) {
        var state by mutableStateOf(initialState)
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = state,
                    resumePositionsByMediaId = resumePositions,
                    onBack = onBack,
                    onRetry = {},
                    onChildFocused = { childId ->
                        state = state.copy(focusedChildId = childId)
                    },
                    onChildViewportChanged = { viewport ->
                        state = state.copy(childViewport = viewport)
                    },
                    onPlayParent = onPlayParent,
                    onPlayChild = onPlayChild,
                )
            }
        }
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

private fun movieCollection() = detail(
    id = 601,
    title = "星海三部曲",
    path = "/media/collection",
    children = listOf(
        MediaSummary(
            id = 602,
            title = "第一部",
            path = "/media/part-1.mkv",
            posterPath = null,
            backdropPath = null,
            year = 2026,
            rating = 8.2,
            season = null,
            episode = 1,
            aired = "2026-02-01",
        ),
    ),
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
    libraryType = MediaLibraryType.TvShow,
)

private fun multiSeasonSeries() = series().copy(
    children = listOf(
        series().children.first().copy(
            id = 300,
            season = 0,
            episode = 1,
        ),
        series().children.first(),
    ),
)

private fun twoEpisodeSeries() = series().copy(
    children = series().children + MediaSummary(
        id = 302,
        title = "返程",
        path = "/media/episode-2.mkv",
        posterPath = "/art/episode-2.jpg",
        backdropPath = "/art/episode-2-backdrop.jpg",
        year = 2026,
        rating = 8.7,
        season = 1,
        episode = 2,
        aired = "2026-01-09",
    ),
)

private fun longSeries() = series().copy(
    children = (1..12).map { episode ->
        MediaSummary(
            id = 300L + episode,
            title = "第 $episode 集内容",
            path = "/media/episode-$episode.mkv",
            posterPath = null,
            backdropPath = null,
            year = 2026,
            rating = 8.0,
            season = 1,
            episode = episode,
            aired = "2026-01-${episode.toString().padStart(2, '0')}",
        )
    },
)

private fun detail(
    id: Long,
    title: String,
    path: String,
    children: List<MediaSummary>,
    actors: List<MediaActor> = emptyList(),
    libraryType: MediaLibraryType = MediaLibraryType.Movie,
) = MediaDetail(
    id = id,
    library = MediaLibrary(21, "Library", libraryType),
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
