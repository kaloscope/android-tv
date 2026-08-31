package org.kaloscope.tv.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.test.assertFocusedContentCardBottomInsideViewport
import org.kaloscope.tv.test.assertFocusedContentCardScale
import org.kaloscope.tv.test.assertFocusedContentCardSurface
import org.kaloscope.tv.test.assertFocusedContentCardTopClearance
import org.kaloscope.tv.test.assertSidebarNavigationSurfaces
import org.kaloscope.tv.test.captureToImage

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun librarySidebarUsesApprovedRestingSurfaces() {
        val libraries = listOf(
            MediaLibrary(21, "剧集库", MediaLibraryType.TvShow),
            MediaLibrary(22, "电影库", MediaLibraryType.Movie),
        )
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background),
                ) {
                    LibraryScreen(
                        session = session(),
                        state = state().copy(
                            libraries = libraries,
                            selectedLibraryId = 21,
                        ),
                        restoreMediaId = null,
                        requestInitialFocus = false,
                        onSelectLibrary = {},
                        onQueryChange = {},
                        onSearch = {},
                        onRetry = {},
                        onLoadMore = {},
                        onMediaFocused = {},
                        onOpenMedia = {},
                    )
                }
            }
        }

        val selected = composeRule.onNodeWithTag("library-sidebar-item-21")
            .captureToImage()
            .asAndroidBitmap()
        val unselected = composeRule.onNodeWithTag("library-sidebar-item-22")
            .captureToImage()
            .asAndroidBitmap()
        val sampleInset = with(composeRule.density) { 16.dp.roundToPx() }

        assertSidebarNavigationSurfaces(
            label = "Library sidebar",
            selected = selected,
            unselected = unselected,
            sampleInset = sampleInset,
        )
    }

    @Test
    fun longLibraryNameUsesSingleLineEllipsis() {
        val longName = "适合全家观看的超长名称电视剧媒体资料库"
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state().copy(
                        libraries = listOf(
                            MediaLibrary(21, longName, MediaLibraryType.TvShow),
                        ),
                    ),
                    restoreMediaId = null,
                    requestInitialFocus = false,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        val layoutResults = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(longName, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(layoutResults)
            }

        val layout = layoutResults.single()
        assertEquals(1, layout.lineCount)
        assertTrue("Long library name should end with an ellipsis", layout.isLineEllipsized(0))
    }

    @Test
    fun restingMediaCardShowsScreenBackground() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background),
                ) {
                    LibraryScreen(
                        session = session(),
                        state = state(),
                        restoreMediaId = null,
                        requestInitialFocus = false,
                        onSelectLibrary = {},
                        onQueryChange = {},
                        onSearch = {},
                        onRetry = {},
                        onLoadMore = {},
                        onMediaFocused = {},
                        onOpenMedia = {},
                    )
                }
            }
        }

        val card = composeRule.onNodeWithTag("media-card-1")
            .captureToImage()
            .asAndroidBitmap()
        val sampleX = with(composeRule.density) { 20.dp.roundToPx() }
            .coerceIn(0, card.width - 1)
        val sampleY = with(composeRule.density) { card.height - 8.dp.roundToPx() }
            .coerceIn(0, card.height - 1)

        assertEquals(
            "Resting media card should reveal the screen background",
            Background.toArgb(),
            card.getPixel(sampleX, sampleY),
        )
    }

    @Test
    fun focusedMediaCardUsesLighterBlueSurface() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(),
                    restoreMediaId = null,
                    requestInitialFocus = false,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("media-card-1")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(1_000)
        val focused = composeRule.onNodeWithTag("media-card-1")
            .captureToImage()
            .asAndroidBitmap()
        val sampleInset = with(composeRule.density) { 12.dp.roundToPx() }

        assertFocusedContentCardSurface(
            label = "Library media card",
            bitmap = focused,
            sampleX = sampleInset,
            sampleY = focused.height - sampleInset,
        )
    }

    @Test
    fun focusedMediaCardUsesTwoPercentScale() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(),
                    restoreMediaId = null,
                    requestInitialFocus = false,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-search-action-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(1_000)
        val resting = composeRule.onNodeWithTag("media-card-1")
            .captureToImage()
            .asAndroidBitmap()
        composeRule.onNodeWithTag("media-card-1")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.mainClock.advanceTimeBy(1_000)
        val focused = composeRule.onNodeWithTag("media-card-1")
            .captureToImage()
            .asAndroidBitmap()

        assertFocusedContentCardScale(
            label = "Library media card",
            resting = resting,
            focused = focused,
            expectedScale = 1.02f,
        )
    }

    @Test
    fun initialLoadingUsesCenteredIndicator() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = LibraryUiState.Loading,
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-loading-indicator").assertExists()
        composeRule.onNodeWithTag("library-loading-skeleton").assertDoesNotExist()
    }

    @Test
    fun mediaLoadingKeepsKnownControlsAndCentersIndicatorInContentRegion() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state().copy(items = LibraryItemsState.Loading),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithText("剧集库").assertExists()
        composeRule.onNodeWithTag("library-search-input").assertExists()
        composeRule.onNodeWithTag("library-items-loading-indicator").assertExists()
        composeRule.onNodeWithTag("library-items-loading-skeleton").assertDoesNotExist()
    }

    @Test
    fun selectedLibraryRemainsSelectedWhileSearchActionOwnsFocus() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-search-action-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.onNodeWithText("剧集库").assertIsSelected()
    }

    @Test
    fun selectingLowerLibraryKeepsFocusOnSelectedItem() {
        var currentState by mutableStateOf(
            state().copy(
                libraries = listOf(
                    MediaLibrary(21, "剧集库", MediaLibraryType.TvShow),
                    MediaLibrary(22, "电影库", MediaLibraryType.Movie),
                ),
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = currentState,
                    restoreMediaId = null,
                    onSelectLibrary = { libraryId ->
                        currentState = currentState.copy(
                            selectedLibraryId = libraryId,
                            items = LibraryItemsState.Loading,
                            focusedMediaId = null,
                        )
                    },
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-sidebar-item-22")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("library-sidebar-item-22")
            .assertIsSelected()
            .assertIsFocused()
        composeRule.onNodeWithTag("library-sidebar-item-21").assertIsNotFocused()
    }

    @Test
    fun singleLibraryInitialFocusesSearchInput() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-search-input").assertIsFocused()
        composeRule.onNodeWithTag("library-sidebar-item-21")
            .assertIsNotFocused()
            .assertIsSelected()
    }

    @Test
    fun movingLeftFromLibrarySearchInputSkipsSingleLibrary() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(),
                    restoreMediaId = null,
                    requestInitialFocus = false,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-search-input")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }

        composeRule.onNodeWithTag("library-search-input").assertIsFocused()
        composeRule.onNodeWithTag("library-sidebar-item-21").assertIsNotFocused()
    }

    @Test
    fun multipleLibrariesInitialFocusesFirstLibrary() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state().copy(
                        libraries = listOf(
                            MediaLibrary(21, "剧集库", MediaLibraryType.TvShow),
                            MediaLibrary(22, "电影库", MediaLibraryType.Movie),
                        ),
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-sidebar-item-21").assertIsFocused()
        composeRule.onNodeWithTag("library-search-input").assertIsNotFocused()
    }

    @Test
    fun librarySearchActionUsesCompactSharedControlHeight() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val buttonBounds = composeRule.onNodeWithTag("library-search-action-button")
            .fetchSemanticsNode()
            .boundsInRoot
        val iconBounds = composeRule.onNodeWithTag(
            testTag = "library-search-action-icon",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        assertEquals(48f * density, buttonBounds.width, 1f)
        assertEquals(48f * density, buttonBounds.height, 1f)
        assertEquals(24f * density, iconBounds.width, 1f)
        assertEquals(24f * density, iconBounds.height, 1f)
        composeRule.onNodeWithText("搜索", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun resultsStartTwentyFourDpBelowSearchField() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(media = mediaItems(5)),
                    restoreMediaId = null,
                    requestInitialFocus = false,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val inputBounds = composeRule.onNodeWithTag("library-search-input")
            .fetchSemanticsNode()
            .boundsInRoot
        val firstResultBounds = composeRule.onNodeWithTag("media-card-1")
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(
            24f * density,
            firstResultBounds.top - inputBounds.bottom,
            1f,
        )
    }

    @Test
    fun portraitGridKeepsFourCardsWithCompactSpacingAt1080p() {
        val width = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.widthPixels
        if (width != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 36.dp),
                ) {
                    LibraryScreen(
                        session = session(),
                        state = state(media = mediaItems(5)),
                        restoreMediaId = null,
                        requestInitialFocus = false,
                        onSelectLibrary = {},
                        onQueryChange = {},
                        onSearch = {},
                        onRetry = {},
                        onLoadMore = {},
                        onMediaFocused = {},
                        onOpenMedia = {},
                    )
                }
            }
        }

        val cardBounds = (1..5).map { id ->
            composeRule.onNodeWithTag("media-card-$id")
                .fetchSemanticsNode()
                .boundsInRoot
        }
        val gridBounds = composeRule.onNodeWithTag("library-results-grid")
            .fetchSemanticsNode()
            .boundsInRoot
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        cardBounds.take(4).forEach { bounds ->
            assertEquals(cardBounds.first().top, bounds.top, 0.5f)
        }
        assertTrue(
            "The fifth portrait card should start the second row",
            cardBounds[4].top > cardBounds.first().top,
        )
        assertTrue(
            "Media cards should use the expanded content width",
            cardBounds.first().width >= 166f * density,
        )
        assertEquals(
            8f * density,
            cardBounds[1].left - cardBounds[0].right,
            1f,
        )
        assertEquals(
            12f * density,
            cardBounds[4].top - cardBounds[0].bottom,
            1f,
        )
        assertFocusedContentCardTopClearance(
            label = "First-row library card",
            cardBounds = cardBounds.first(),
            viewportBounds = gridBounds,
            density = density,
            focusScale = 1.02f,
        )
    }

    @Test
    fun focusedSecondRowCardKeepsScaledBottomInsideGridAt1080p() {
        val width = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.widthPixels
        if (width != 1920) return
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(
                            start = 44.dp,
                            top = 84.dp,
                            end = 44.dp,
                            bottom = 24.dp,
                        ),
                ) {
                    LibraryScreen(
                        session = session(),
                        state = state(media = mediaItems(9)),
                        restoreMediaId = null,
                        requestInitialFocus = false,
                        onSelectLibrary = {},
                        onQueryChange = {},
                        onSearch = {},
                        onRetry = {},
                        onLoadMore = {},
                        onMediaFocused = {},
                        onOpenMedia = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("media-card-5")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.mainClock.advanceTimeBy(1_000)
        val cardBounds = composeRule.onNodeWithTag("media-card-5")
            .fetchSemanticsNode()
            .boundsInRoot
        val gridBounds = composeRule.onNodeWithTag("library-results-grid")
            .fetchSemanticsNode()
            .boundsInRoot
        val screenshot = composeRule.onRoot()
            .captureToImage()
            .asAndroidBitmap()

        assertFocusedContentCardBottomInsideViewport(
            label = "Second-row library card",
            bitmap = screenshot,
            cardBounds = cardBounds,
            viewportBounds = gridBounds,
            density = composeRule.density.density,
        )
    }

    @Test
    fun librarySearchActionSubmitsSearch() {
        var searches = 0
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = { searches += 1 },
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-search-action-button")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, searches)
        }
    }

    @Test
    fun rightFromLibrarySearchFieldMovesToSearchAction() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-search-input")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("library-search-action-button")
            .assertIsFocused()
    }

    @Test
    fun librarySidebarUsesTypeIconsInsteadOfNameInitials() {
        val libraries = listOf(
            MediaLibrary(1, "电影库", MediaLibraryType.Movie),
            MediaLibrary(2, "剧集库", MediaLibraryType.TvShow),
            MediaLibrary(3, "其他库", MediaLibraryType.Unknown),
        )
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state().copy(
                        libraries = libraries,
                        selectedLibraryId = 1,
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        listOf(
            "library-type-icon-movie",
            "library-type-icon-tv-show",
            "library-type-icon-unknown",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        }
        listOf("电", "剧", "其").forEach { initial ->
            composeRule.onNodeWithText(initial, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun mediaCardShowsValidRatingAndDoesNotUseBackdropAsPoster() {
        val media = mediaItems(1).single().copy(
            rating = 8.14,
            backdropPath = "/backdrop.jpg",
        )
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(media = listOf(media)),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("media-rating-1", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("★ 8.1", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("server-image-missing", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun mediaCardHidesInvalidRating() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(media = listOf(mediaItems(1).single().copy(rating = 10.1))),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("media-rating-1", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun firstUsableMediaPublishesInitialBackdrop() {
        var selectedBackdrop: LibraryBackdropPresentation? = null
        val media = listOf(
            mediaItems(1).single(),
            mediaItems(2).last().copy(
                title = "背景来源",
                backdropPath = "/second-backdrop.jpg",
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(media = media),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                    onBackdropChanged = { selectedBackdrop = it },
                )
            }
        }

        composeRule.runOnIdle {
            assertEquals(
                LibraryBackdropPresentation(
                    path = "/second-backdrop.jpg",
                    title = "背景来源",
                ),
                selectedBackdrop,
            )
        }
    }

    @Test
    fun focusedMediaPublishesItsBackdrop() {
        var currentState by mutableStateOf(
            state(
                media = mediaItems(2).map { media ->
                    media.copy(backdropPath = "/backdrop-${media.id}.jpg")
                },
            ),
        )
        var selectedBackdrop: LibraryBackdropPresentation? = null
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = currentState,
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = { focusedId ->
                        currentState = currentState.copy(focusedMediaId = focusedId)
                    },
                    onOpenMedia = {},
                    onBackdropChanged = { selectedBackdrop = it },
                )
            }
        }

        composeRule.onNodeWithTag("media-card-1")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithTag("media-card-2").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(
                LibraryBackdropPresentation(
                    path = "/backdrop-2.jpg",
                    title = "媒体2",
                ),
                selectedBackdrop,
            )
        }
    }

    @Test
    fun restoredMediaPublishesItsBackdrop() {
        var selectedBackdrop: LibraryBackdropPresentation? = null
        val media = mediaItems(3).map { item ->
            item.copy(backdropPath = "/backdrop-${item.id}.jpg")
        }
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(media = media),
                    restoreMediaId = 3,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                    onBackdropChanged = { selectedBackdrop = it },
                )
            }
        }

        composeRule.onNodeWithTag("media-card-3").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(
                LibraryBackdropPresentation(
                    path = "/backdrop-3.jpg",
                    title = "媒体3",
                ),
                selectedBackdrop,
            )
        }
    }

    @Test
    fun loadingAndImageLessContentDoNotPublishNull() {
        var currentState by mutableStateOf<LibraryUiState>(
            state(
                media = listOf(
                    mediaItems(1).single().copy(
                        backdropPath = "/retained-backdrop.jpg",
                    ),
                ),
            ),
        )
        val publishedBackdrops = mutableListOf<LibraryBackdropPresentation>()
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = currentState,
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                    onBackdropChanged = publishedBackdrops::add,
                )
            }
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    LibraryBackdropPresentation(
                        path = "/retained-backdrop.jpg",
                        title = "媒体1",
                    ),
                ),
                publishedBackdrops,
            )
            currentState = state().copy(items = LibraryItemsState.Loading)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            currentState = state(media = mediaItems(2))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    LibraryBackdropPresentation(
                        path = "/retained-backdrop.jpg",
                        title = "媒体1",
                    ),
                ),
                publishedBackdrops,
            )
        }
    }

    @Test
    fun mediaTitleAndYearUseCenteredFullWidthSlots() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = listOf(
                            mediaItems(1).single().copy(year = 2026),
                        ),
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        val cardBounds = composeRule.onNodeWithTag("media-card-1")
            .fetchSemanticsNode()
            .boundsInRoot
        val titleBounds = composeRule.onNodeWithText(
            text = "媒体1",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val yearBounds = composeRule.onNodeWithText(
            text = "2026",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        assertEquals(cardBounds.center.x, titleBounds.center.x, 1f)
        assertEquals(cardBounds.center.x, yearBounds.center.x, 1f)
    }

    @Test
    fun mediaCardUsesProminentPosterAndCompactMetadataSpacing() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = listOf(
                            mediaItems(1).single().copy(year = 2026),
                        ),
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val cardBounds = composeRule.onNodeWithTag("media-card-1")
            .fetchSemanticsNode()
            .boundsInRoot
        val posterBounds = composeRule.onNodeWithTag(
            testTag = "server-image-missing",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val titleBounds = composeRule.onNodeWithTag(
            testTag = "media-title-1",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val yearBounds = composeRule.onNodeWithTag(
            testTag = "media-year-1",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val posterWidthRatio = posterBounds.width / cardBounds.width
        val posterTitleGap = titleBounds.top - posterBounds.bottom
        val bottomPadding = cardBounds.bottom - yearBounds.bottom

        assertTrue(
            "Poster should occupy at least 94% of the card width but was " +
                "${posterWidthRatio * 100f}%",
            posterWidthRatio >= 0.94f,
        )
        assertEquals(4f * density, posterBounds.left - cardBounds.left, 1f)
        assertEquals(4f * density, cardBounds.right - posterBounds.right, 1f)
        assertEquals(6f * density, posterTitleGap, 1f)
        assertEquals(4f * density, bottomPadding, 1f)
    }

    @Test
    fun mediaCardReservesTwoTitleLines() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = listOf(
                            mediaItems(1).single().copy(
                                title = "足以占用两行的媒体标题",
                            ),
                        ),
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        val titleHeight = composeRule.onNodeWithTag(
            testTag = "media-title-1",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.height

        assertEquals(36f * density, titleHeight, 1f)
    }

    @Test
    fun missingYearKeepsCardsTheSameHeight() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = listOf(
                            mediaItems(1).single().copy(year = 2026),
                            mediaItems(2).last().copy(year = null),
                        ),
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onOpenMedia = {},
                )
            }
        }

        val datedCardHeight = composeRule.onNodeWithTag("media-card-1")
            .fetchSemanticsNode()
            .boundsInRoot
            .height
        val undatedCardHeight = composeRule.onNodeWithTag("media-card-2")
            .fetchSemanticsNode()
            .boundsInRoot
            .height

        assertEquals(datedCardHeight, undatedCardHeight, 1f)
        val emptyYearHeight = composeRule.onNodeWithTag(
            testTag = "media-year-2",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.height
        assertTrue(emptyYearHeight > 0f)
    }

    @Test
    fun deepViewportRestoresFocusedMediaFromSessionState() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = mediaItems(30),
                        focusedMediaId = 25,
                        gridViewport = GridViewportSnapshot(24, 0),
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("media-card-25").assertIsFocused()
    }

    @Test
    fun rememberedFocusDoesNotOverrideDpadMovement() {
        var currentState by mutableStateOf(
            state(
                media = mediaItems(4),
                focusedMediaId = 1,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = currentState,
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = { focusedId ->
                        currentState = currentState.copy(focusedMediaId = focusedId)
                    },
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("media-card-1")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("media-card-2").assertIsFocused()
    }

    @Test
    fun missingRestoreTargetFallsBackNearSavedViewport() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = mediaItems(20),
                        focusedMediaId = 999,
                        gridViewport = GridViewportSnapshot(18, 0),
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("media-card-19").assertIsFocused()
    }

    @Test
    fun prefetchZoneRequestsOneNextPage() {
        var loads = 0
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(media = mediaItems(20), hasNext = true),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = { loads += 1 },
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-results-grid").performScrollToIndex(19)
        composeRule.onNodeWithTag("media-card-20")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.runOnIdle {
            assertEquals(1, loads)
        }
    }

    @Test
    fun loadingNextPageShowsInlineIndicatorAndMessage() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = mediaItems(20),
                        hasNext = true,
                        isLoadingMore = true,
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = {},
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-results-grid").performScrollToIndex(20)
        composeRule.onNodeWithTag("library-load-more-loading-indicator").assertExists()
        composeRule.onNodeWithText("正在加载…").assertExists()
    }

    @Test
    fun finalPageDoesNotPrefetchOrRenderPagingFooter() {
        var loads = 0
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(media = mediaItems(20), hasNext = false),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = { loads += 1 },
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-results-grid").performScrollToIndex(19)
        composeRule.onNodeWithTag("media-card-20")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.onNodeWithTag("library-load-more-loading").assertDoesNotExist()
        composeRule.onNodeWithTag("library-load-more-retry").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, loads)
        }
    }

    @Test
    fun loadMoreFailureKeepsMediaAndOffersFocusableRetry() {
        var loads = 0
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = session(),
                    state = state(
                        media = mediaItems(20),
                        hasNext = true,
                        loadMoreError = AppError.Offline,
                    ),
                    restoreMediaId = null,
                    onSelectLibrary = {},
                    onQueryChange = {},
                    onSearch = {},
                    onRetry = {},
                    onLoadMore = { loads += 1 },
                    onMediaFocused = {},
                    onGridViewportChanged = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithTag("library-results-grid").performScrollToIndex(20)
        composeRule.onNodeWithTag("media-card-20").assertExists()
        composeRule.onNodeWithTag("library-load-more-retry")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, loads)
        }
    }
}

private fun state(
    media: List<MediaSummary> = mediaItems(1),
    focusedMediaId: Long? = null,
    gridViewport: GridViewportSnapshot = GridViewportSnapshot.Top,
    hasNext: Boolean = false,
    isLoadingMore: Boolean = false,
    loadMoreError: AppError? = null,
) = LibraryUiState.Content(
    libraries = listOf(MediaLibrary(21, "剧集库", MediaLibraryType.TvShow)),
    selectedLibraryId = 21,
    items = LibraryItemsState.Content(
        items = media,
        total = media.size,
        pageNumber = 1,
        hasNext = hasNext,
        isLoadingMore = isLoadingMore,
        loadMoreError = loadMoreError,
    ),
    focusedMediaId = focusedMediaId,
    gridViewport = gridViewport,
)

private fun mediaItems(count: Int) = (1..count).map { id ->
    MediaSummary(
        id = id.toLong(),
        title = "媒体$id",
        path = "/media/$id",
        posterPath = null,
        backdropPath = null,
        year = null,
        rating = null,
        season = null,
        episode = null,
    )
}

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
