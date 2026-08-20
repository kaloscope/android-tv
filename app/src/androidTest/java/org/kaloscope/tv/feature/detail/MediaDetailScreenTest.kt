package org.kaloscope.tv.feature.detail

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaActor
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
    }

    @Test
    fun detailDoesNotExposeOnScreenBackControl() {
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

        composeRule.onNodeWithTag("detail-back").assertDoesNotExist()
    }

    @Test
    fun detailUsesStarRatingAndStructuredMetadata() {
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(
                        movie().copy(genres = listOf("科幻", "冒险")),
                    ),
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

        composeRule.onNodeWithTag("detail-rating-badge").assertExists()
        composeRule.onNodeWithText("★ 8.5", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("评分 8.5").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-metadata-year").assertTextContains("2026")
        composeRule.onNodeWithTag("detail-genre-0").assertTextContains("科幻")
        composeRule.onNodeWithTag("detail-genre-1").assertTextContains("冒险")
    }

    @Test
    fun writersAndStudiosRenderWithoutDirectorOrCast() {
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(
                        movie().copy(
                            writers = listOf("顾远"),
                            studios = listOf("星河影业"),
                        ),
                    ),
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

        composeRule.onNodeWithText("编剧").assertExists()
        composeRule.onNodeWithText("顾远").assertExists()
        composeRule.onNodeWithText("制片公司").assertExists()
        composeRule.onNodeWithText("星河影业").assertExists()
    }

    @Test
    fun resumeActionsExposePrimaryAndSecondaryHierarchy() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = series()),
            resumePositions = mapOf(301L to 42L),
        )

        composeRule.onNodeWithTag("detail-primary-action").assertIsSelected()
        composeRule.onNodeWithTag("detail-start-over-action").assertExists()
        composeRule.onNodeWithTag(
            "detail-start-over-icon",
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun heroUsesTvScaleAfterRemovingBackButton() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
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
        }

        val poster = composeRule.onNodeWithTag("detail-parent-poster-501")
            .fetchSemanticsNode().boundsInRoot
        val maximumTop = with(composeRule.density) { 72.dp.toPx() }
        val minimumPosterWidth = with(composeRule.density) { 136.dp.toPx() }

        assertTrue("Hero should reclaim the removed back button space", poster.top <= maximumTop)
        assertTrue("Poster should remain readable from TV distance", poster.width >= minimumPosterWidth)
    }

    @Test
    fun movieTitleRemainsFullyVisibleIn720pViewport() {
        val parent = movie().copy(
            plot = "一支深空探索队穿越寂静星云，寻找失联已久的先遣舰队。",
            genres = listOf("科幻", "冒险"),
            directors = listOf("林舟"),
            writers = listOf("顾远"),
            studios = listOf("Kaloscope Pictures"),
        )
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(640.dp)
                        .height(360.dp),
                ) {
                    MediaDetailScreen(
                        session = session(),
                        state = MediaDetailUiState.Content(parent),
                        resumePositionsByMediaId = mapOf(parent.id to 42L),
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

        val viewport = composeRule.onNodeWithTag("detail-cinematic-surface")
            .fetchSemanticsNode().boundsInRoot
        val titleNode = composeRule.onNodeWithText(parent.title).fetchSemanticsNode()
        val title = titleNode.boundsInRoot
        val tolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue("Movie title should start inside the 720p viewport: $title", title.top >= viewport.top)
        assertTrue(
            "Movie title should be fully visible in 640x360dp: $title",
            title.height + tolerance >= titleNode.size.height,
        )
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
        composeRule.onNodeWithText("S1E1 - 启程").assertExists()
        composeRule.onNodeWithText("第 1 集").assertDoesNotExist()
        composeRule.onNodeWithText("2026-01-02").assertExists()
    }

    @Test
    fun focusedEpisodeTitleUsesAccentColor() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = series()),
        )

        composeRule.mainClock.advanceTimeBy(500)
        val title = composeRule.onNodeWithTag(
            "media-child-title-301",
            useUnmergedTree = true,
        )
            .captureToImage()
            .asAndroidBitmap()

        assertTrue(
            "Focused episode title should use the active accent color",
            title.countPixelsNear(AndroidColor.rgb(0x7F, 0x96, 0xFF), tolerance = 8) >= 12,
        )
    }

    @Test
    fun episodeCardsKeepEqualHeightWhenMetadataIsMissing() {
        val first = twoEpisodeSeries().children.first()
        val second = twoEpisodeSeries().children.last().copy(
            year = null,
            aired = null,
        )
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = twoEpisodeSeries().copy(children = listOf(first, second)),
            ),
        )
        composeRule.onNodeWithTag("detail-primary-action")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()

        val firstHeight = composeRule.onNodeWithTag("media-child-card-301")
            .fetchSemanticsNode().boundsInRoot.height
        val secondHeight = composeRule.onNodeWithTag("media-child-card-302")
            .fetchSemanticsNode().boundsInRoot.height
        val tolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue(
            "Episode cards should not jump in height when metadata is absent: " +
                "first=$firstHeight, second=$secondHeight",
            kotlin.math.abs(firstHeight - secondHeight) <= tolerance,
        )
    }

    @Test
    fun episodeCarouselAlignsWithSectionHeading() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = series()),
        )
        composeRule.onNodeWithTag("detail-primary-action")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()

        val headingLeft = composeRule.onNodeWithText("分集")
            .fetchSemanticsNode().boundsInRoot.left
        val firstCardLeft = composeRule.onNodeWithTag("media-child-card-301")
            .fetchSemanticsNode().boundsInRoot.left
        val tolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue(
            "Episode cards should share the section heading's leading edge: " +
                "heading=$headingLeft, card=$firstCardLeft",
            kotlin.math.abs(headingLeft - firstCardLeft) <= tolerance,
        )
    }

    @Test
    fun episodeTitleKeepsEnoughHeightForTvFontMetrics() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = series()),
        )
        val card = composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()
        composeRule.waitForIdle()
        card.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        val title = composeRule.onNodeWithTag(
            "media-child-title-301",
            useUnmergedTree = true,
        )
        composeRule.waitUntil {
            title.fetchSemanticsNode().boundsInRoot.height > 0f
        }
        val titleBounds = title.fetchSemanticsNode().boundsInRoot
        val minimumUnclippedHeight = with(composeRule.density) { 22.dp.toPx() }

        assertTrue(
            "Episode title must leave enough vertical room for TV font metrics: $titleBounds",
            titleBounds.height >= minimumUnclippedHeight,
        )
    }

    @Test
    fun episodeTitleLeavesRoomForSupportingMetadata() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = mixedTitleSeries(),
                focusedChildId = 301,
            ),
        )
        val card = composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()
        composeRule.waitForIdle()
        card.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        val cardBounds = card.fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithTag(
            "media-child-title-301",
            useUnmergedTree = true,
        )
        composeRule.waitUntil {
            title.fetchSemanticsNode().boundsInRoot.height > 0f
        }
        val titleBounds = title.fetchSemanticsNode().boundsInRoot
        val minimumMetadataSpace = with(composeRule.density) { 26.dp.toPx() }
        val renderingTolerance = with(composeRule.density) { 0.5.dp.toPx() }

        assertTrue(
            "Episode title should leave room for metadata and the bottom inset: " +
                "card=$cardBounds, title=$titleBounds",
            cardBounds.bottom - titleBounds.bottom + renderingTolerance >=
                minimumMetadataSpace,
        )
    }

    @Test
    fun episodeTitlesWrapWithoutMovingMetadataRows() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = mixedTitleSeries(),
                focusedChildId = 303,
            ),
        )
        val focusedCard = composeRule.onNodeWithTag("media-child-card-303")
            .assertIsFocused()
        composeRule.waitForIdle()
        focusedCard.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        val wrappedTitle = composeRule.onNodeWithTag(
            "media-child-title-301",
            useUnmergedTree = true,
        )
        composeRule.waitUntil {
            wrappedTitle.fetchSemanticsNode().boundsInRoot.height > 0f
        }
        val wrappedTitleBounds = wrappedTitle.fetchSemanticsNode().boundsInRoot
        val wrappedDateTop = composeRule.onNodeWithText(
            "2026-07-31",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.top
        val shortDateTop = composeRule.onNodeWithText(
            "2026-08-07",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot.top
        val minimumWrappedHeight = with(composeRule.density) { 38.dp.toPx() }
        val alignmentTolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue(
            "A typical long episode title should use a second line: $wrappedTitleBounds",
            wrappedTitleBounds.height >= minimumWrappedHeight,
        )
        assertTrue(
            "Episode metadata rows should stay aligned when title lengths differ: " +
                "wrapped=$wrappedDateTop, short=$shortDateTop",
            kotlin.math.abs(wrappedDateTop - shortDateTop) <= alignmentTolerance,
        )
    }

    @Test
    fun episodeTitlePrefixesKeepSameRenderedTopAcrossFontFallbacks() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = mixedTitleSeries(),
                focusedChildId = 303,
            ),
        )
        val focusedCard = composeRule.onNodeWithTag("media-child-card-303")
            .assertIsFocused()
        composeRule.waitForIdle()
        focusedCard.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        val prefixWidth = with(composeRule.density) { 40.dp.roundToPx() }
        val mixedTitle = composeRule.onNodeWithTag(
            "media-child-title-301",
            useUnmergedTree = true,
        )
        val latinTitle = composeRule.onNodeWithTag(
            "media-child-title-302",
            useUnmergedTree = true,
        )
        val mixedLocalTop = mixedTitle.captureToImage()
            .asAndroidBitmap()
            .firstBrightPixelRow(prefixWidth)
        val latinLocalTop = latinTitle.captureToImage()
            .asAndroidBitmap()
            .firstBrightPixelRow(prefixWidth)

        assertTrue(
            "Episode title prefixes should render in captured title nodes: " +
                "mixed=$mixedLocalTop, latin=$latinLocalTop",
            mixedLocalTop >= 0 && latinLocalTop >= 0,
        )
        val mixedTop = mixedTitle.fetchSemanticsNode().boundsInRoot.top + mixedLocalTop
        val latinTop = latinTitle.fetchSemanticsNode().boundsInRoot.top + latinLocalTop
        assertTrue(
            "Mixed-script and Latin-only episode title ink should share the same top row: " +
                "mixed=$mixedTop, latin=$latinTop",
            kotlin.math.abs(mixedTop - latinTop) <= 1f,
        )
    }

    @Test
    fun episodeCarouselShowsDirectionalEdgeFades() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = longSeries()),
        )

        composeRule.onNodeWithTag("detail-child-carousel-end-fade").assertExists()
        composeRule.onNodeWithTag("detail-child-carousel-start-fade").assertDoesNotExist()

        composeRule.onNodeWithTag("media-child-card-301")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-child-carousel-start-fade").assertExists()
        composeRule.onNodeWithTag("detail-child-carousel-end-fade").assertExists()
    }

    @Test
    fun movingRightBringsFocusedEpisodeTowardLeadingEdge() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = longSeries()),
        )
        val carouselLeft = composeRule.onNodeWithTag("detail-child-carousel")
            .fetchSemanticsNode().boundsInRoot.left
        val secondBefore = composeRule.onNodeWithTag("media-child-card-302")
            .fetchSemanticsNode().boundsInRoot.left

        composeRule.onNodeWithTag("media-child-card-301")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()

        val focused = composeRule.onNodeWithTag("media-child-card-302")
            .assertIsFocused()
            .fetchSemanticsNode().boundsInRoot
        val maximumLeadingInset = with(composeRule.density) { 140.dp.toPx() }
        val minimumMovement = with(composeRule.density) { 48.dp.toPx() }

        assertTrue(
            "Focused episode should settle near the carousel leading edge: $focused",
            focused.left - carouselLeft <= maximumLeadingInset,
        )
        assertTrue(
            "Episode ribbon should visibly move instead of only changing focus",
            secondBefore - focused.left >= minimumMovement,
        )
    }

    @Test
    fun movingBetweenEpisodesKeepsSeriesAtTopThroughoutAnimation() {
        var state by mutableStateOf(
            MediaDetailUiState.Content(
                parent = longSeries().copy(
                    plot = "父级简介第一行\n父级简介第二行\n父级简介第三行",
                    genres = listOf("剧情", "科幻", "冒险", "悬疑", "太空"),
                    studios = listOf("Kaloscope Animation Studio"),
                    actors = listOf(MediaActor("沈川", "队长", null)),
                ),
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(960.dp)
                        .height(540.dp),
                ) {
                    MediaDetailScreen(
                        session = session(),
                        state = state,
                        resumePositionsByMediaId = emptyMap(),
                        onBack = {},
                        onRetry = {},
                        onChildFocused = { childId ->
                            state = state.copy(focusedChildId = childId)
                        },
                        onChildViewportChanged = { viewport ->
                            state = state.copy(childViewport = viewport)
                        },
                        onPlayParent = { _, _ -> },
                        onPlayChild = { _, _ -> },
                    )
                }
            }
        }
        val detailScroll = composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        )
        composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()
        assertEquals(
            0f,
            detailScroll.fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value(),
            0f,
        )

        fun verticalOffsetsWhileMoving(fromChildId: Long, direction: Key): List<Float> {
            composeRule.mainClock.autoAdvance = false
            composeRule.onNodeWithTag("media-child-card-$fromChildId")
                .performKeyInput { pressKey(direction) }
            val offsets = buildList {
                repeat(30) {
                    composeRule.mainClock.advanceTimeByFrame()
                    add(
                        detailScroll.fetchSemanticsNode()
                            .config[SemanticsProperties.VerticalScrollAxisRange]
                            .value(),
                    )
                }
            }
            composeRule.mainClock.autoAdvance = true
            composeRule.waitForIdle()
            return offsets
        }

        val rightOffsets = verticalOffsetsWhileMoving(301, Key.DirectionRight)
        composeRule.onNodeWithTag("media-child-card-302").assertIsFocused()
        val leftOffsets = verticalOffsetsWhileMoving(302, Key.DirectionLeft)
        composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()

        assertTrue(
            "Moving right moved the detail page: $rightOffsets",
            rightOffsets.all { it == 0f },
        )
        assertTrue(
            "Moving left moved the detail page: $leftOffsets",
            leftOffsets.all { it == 0f },
        )
    }

    @Test
    fun parentPosterClipsItsCorners() {
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

        val poster = composeRule.onNodeWithTag("detail-parent-poster-201")
            .captureToImage()
            .asAndroidBitmap()
        val corner = poster.getPixel(0, 0)
        val interior = poster.getPixel(poster.width / 2, 8.coerceAtMost(poster.height - 1))

        assertNotEquals("Expected the poster corner to be clipped", interior, corner)
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
    fun castStripExposesEveryActorThroughRemotePaging() {
        val actors = (1..10).map { index ->
            MediaActor("演员$index", "角色$index", null)
        }
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
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
        }

        val play = composeRule.onNodeWithText("播放").assertIsFocused()
        play.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()
        play.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        val castCarousel = composeRule.onNodeWithTag("cast-carousel").assertIsFocused()
        repeat(9) {
            castCarousel.performKeyInput { pressKey(Key.DirectionRight) }
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag("cast-item-9")
            .assertExists()
            .assertIsSelected()
        composeRule.onNodeWithText("演员10").assertIsDisplayed()

        castCarousel.performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cast-item-8").assertIsSelected()

        castCarousel.performKeyInput { pressKey(Key.DirectionDown) }
        castCarousel.assertIsFocused()

        castCarousel.performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.waitForIdle()
        castCarousel.assertIsFocused()

        castCarousel.performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.waitForIdle()
        play.assertIsFocused()
    }

    @Test
    fun castCardsAlignWithSectionHeading() {
        val actors = listOf(
            MediaActor("演员甲", "领航员", null),
            MediaActor("演员乙", null, null),
        )
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = movie(actors)),
        )
        focusCastCarousel()

        val headingLeft = composeRule.onNodeWithText("演职人员")
            .fetchSemanticsNode().boundsInRoot.left
        val firstCardLeft = composeRule.onNodeWithTag("cast-item-0")
            .fetchSemanticsNode().boundsInRoot.left
        val tolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue(
            "Cast cards should share the section heading's leading edge: " +
                "heading=$headingLeft, card=$firstCardLeft",
            kotlin.math.abs(headingLeft - firstCardLeft) <= tolerance,
        )
    }

    @Test
    fun castCardsKeepEqualHeightWhenRoleIsMissing() {
        val actors = listOf(
            MediaActor("演员甲", "领航员", null),
            MediaActor("演员乙", null, null),
        )
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(parent = movie(actors)),
        )
        focusCastCarousel()

        val withRoleHeight = composeRule.onNodeWithTag("cast-item-0")
            .fetchSemanticsNode().boundsInRoot.height
        val withoutRoleHeight = composeRule.onNodeWithTag("cast-item-1")
            .fetchSemanticsNode().boundsInRoot.height
        val tolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue(
            "Cast cards should keep equal height when a role is absent: " +
                "withRole=$withRoleHeight, withoutRole=$withoutRoleHeight",
            kotlin.math.abs(withRoleHeight - withoutRoleHeight) <= tolerance,
        )
    }

    @Test
    fun moreInfoShowsFullPlotAndGenresAndRestoresFocus() {
        val fullPlot = (1..18).joinToString("\n") { paragraph ->
            "第${paragraph}段：探索队在漫长航行中发现未知信号，并沿着信号追踪到群星之外。"
        }
        var backs = 0
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(
                        movie().copy(
                            plot = fullPlot,
                            genres = listOf("剧情", "科幻", "冒险", "悬疑", "太空", "未来"),
                        ),
                    ),
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

        composeRule.onNodeWithTag("detail-more-info-action")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithTag("detail-more-info-panel").assertExists()
        composeRule.onNodeWithTag("detail-more-info-plot").assertTextContains(fullPlot)
        val close = composeRule.onNodeWithTag("detail-more-info-close").assertIsFocused()
        val infoScroll = composeRule.onNodeWithTag("detail-more-info-content")
        val initialRange = infoScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(initialRange.maxValue() > 0f)

        repeat(8) {
            close.performKeyInput { pressKey(Key.DirectionDown) }
            composeRule.waitForIdle()
        }
        close.assertIsFocused()
        val scrolledRange = infoScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(scrolledRange.value() > 0f)
        composeRule.onNodeWithTag("detail-more-info-genre-5").assertTextContains("未来")

        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-more-info-panel").assertDoesNotExist()
        composeRule.onNodeWithTag("detail-more-info-action").assertIsFocused()
        composeRule.runOnIdle { assertEquals(0, backs) }
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
    fun moviePlayButtonShowsReadablePlayIcon() {
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

        assertReadablePrimaryPlayIcon("播放")
    }

    @Test
    fun resumeButtonShowsReadablePlayIcon() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = series(),
                focusedChildId = 301,
            ),
            resumePositions = mapOf(301L to 42L),
        )

        assertReadablePrimaryPlayIcon("继续播放")
    }

    @Test
    fun resumeActionDownFocusesCurrentlySelectedEpisode() {
        setStatefulDetailContent(
            initialState = MediaDetailUiState.Content(
                parent = twoEpisodeSeries(),
                focusedChildId = 301,
            ),
            resumePositions = mapOf(301L to 42L),
        )
        composeRule.onNodeWithTag("detail-primary-action")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.onNodeWithText("继续播放")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()
    }

    @Test
    fun episodeUpFocusesResumeWithoutMovingSeriesViewport() {
        val state = MediaDetailUiState.Content(
            parent = twoEpisodeSeries(),
            focusedChildId = 302,
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
                        resumePositionsByMediaId = mapOf(302L to 42L),
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
        val child = composeRule.onNodeWithTag("media-child-card-302").assertIsFocused()
        val detailScroll = composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        )

        composeRule.mainClock.autoAdvance = false
        child.performKeyInput { pressKey(Key.DirectionUp) }
        val offsets = buildList {
            add(
                detailScroll.fetchSemanticsNode()
                    .config[SemanticsProperties.VerticalScrollAxisRange]
                    .value(),
            )
            repeat(30) {
                composeRule.mainClock.advanceTimeByFrame()
                add(
                    detailScroll.fetchSemanticsNode()
                        .config[SemanticsProperties.VerticalScrollAxisRange]
                        .value(),
                )
            }
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("继续播放").assertIsFocused()
        assertTrue(
            "Episode Up moved the detail viewport: $offsets",
            offsets.all { it == 0f },
        )
    }

    @Test
    fun movieVerticalBoundaryKeysScrollWithoutLeavingPlaybackAction() {
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
        val detailScroll = composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        )

        val play = composeRule.onNodeWithText("播放").assertIsFocused()
        play.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        play.assertIsFocused()
        val bottomRange = detailScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(
            "Expected detail content to extend below the first viewport",
            bottomRange.maxValue() > 0f,
        )
        assertEquals(bottomRange.maxValue(), bottomRange.value(), 0f)

        play.performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.waitForIdle()
        play.assertIsFocused()
        val topOffset = detailScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()
        assertEquals(0f, topOffset, 0f)
    }

    @Test
    fun seriesWithoutCreditsDownMovesToAbsoluteBottom() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
                    MediaDetailScreen(
                        session = session(),
                        state = MediaDetailUiState.Content(parent = series()),
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
        val child = composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()
        val detailScroll = composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        )

        child.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        child.assertIsFocused()
        val bottomRange = detailScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(
            "Expected a series without credits to remain vertically scrollable",
            bottomRange.maxValue() > 0f,
        )
        assertEquals(bottomRange.maxValue(), bottomRange.value(), 0f)
    }

    @Test
    fun initialEpisodeFocusKeepsSeriesAtTop() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
                    MediaDetailScreen(
                        session = session(),
                        state = MediaDetailUiState.Content(parent = series()),
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
        val detailScroll = composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        )

        composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()
        composeRule.onNodeWithText("群星档案").assertIsDisplayed()
        val topRange = detailScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(
            "Expected the detail content to extend below the first viewport",
            topRange.maxValue() > 0f,
        )
        assertEquals(0f, topRange.value(), 0f)
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
    fun focusedChildDetailReplacesParentPlot() {
        val parent = series().copy(plot = "整部剧的父级简介")
        val childDetail = parent.copy(
            id = 301,
            title = "启程",
            path = "/media/episode-1.mkv",
            plot = "第一集独有的分集简介",
            season = 1,
            episode = 1,
            children = emptyList(),
        )
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(
                        parent = parent,
                        focusedChildId = 301,
                        focusedChildDetail = childDetail,
                    ),
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

        composeRule.onNodeWithText("第一集独有的分集简介").assertExists()
        composeRule.onNodeWithText("整部剧的父级简介").assertDoesNotExist()
    }

    @Test
    fun childDetailArrivalKeepsEpisodeCarouselStationary() {
        val parentPlot = "父级简介第一行\n父级简介第二行\n父级简介第三行\n父级简介第四行"
        val parent = series().copy(
            plot = parentPlot,
            genres = listOf("剧情", "科幻", "冒险", "悬疑", "太空"),
        )
        val childDetail = parent.copy(
            id = 301,
            title = "启程",
            path = "/media/episode-1.mkv",
            plot = "第一集简介",
            season = 1,
            episode = 1,
            children = emptyList(),
        )
        var state by mutableStateOf(
            MediaDetailUiState.Content(
                parent = parent,
                focusedChildId = 301,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(960.dp)
                        .height(540.dp),
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
        composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()
        val posterTop = composeRule.onNodeWithTag("detail-parent-poster-201")
            .fetchSemanticsNode().boundsInRoot.top
        val initialCarouselOffset = composeRule.onNodeWithTag("detail-child-carousel")
            .fetchSemanticsNode().boundsInRoot.top - posterTop

        composeRule.runOnIdle {
            state = state.copy(focusedChildDetail = childDetail)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("第一集简介").assertExists()
        val updatedPosterTop = composeRule.onNodeWithTag("detail-parent-poster-201")
            .fetchSemanticsNode().boundsInRoot.top
        val updatedCarouselOffset = composeRule.onNodeWithTag("detail-child-carousel")
            .fetchSemanticsNode().boundsInRoot.top - updatedPosterTop
        val tolerance = with(composeRule.density) { 1.dp.toPx() }
        assertTrue(
            "Child plot replacement moved the episode carousel: " +
                "before=$initialCarouselOffset, after=$updatedCarouselOffset",
            kotlin.math.abs(initialCarouselOffset - updatedCarouselOffset) <= tolerance,
        )
    }

    @Test
    fun synopsisAndEpisodeSelectionFitInside1080pViewport() {
        val plot = "简介第一行\n简介第二行\n简介第三行\n简介第四行"
        val parent = series().copy(
            title = "一段足够长以便在电视详情页面换成两行显示的媒体标题",
            plot = plot,
            genres = listOf("剧情", "科幻", "冒险", "悬疑", "太空"),
        )
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(960.dp)
                        .height(540.dp),
                ) {
                    MediaDetailScreen(
                        session = session(),
                        state = MediaDetailUiState.Content(parent = parent),
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
        composeRule.onNodeWithTag("media-child-card-301").assertIsFocused()

        val viewport = composeRule.onNodeWithTag("detail-cinematic-surface")
            .fetchSemanticsNode().boundsInRoot
        val synopsisNode = composeRule.onNodeWithText(plot).fetchSemanticsNode()
        val synopsis = synopsisNode.boundsInRoot
        val episodeNode = composeRule.onNodeWithTag("media-child-card-301")
            .fetchSemanticsNode()
        val episode = episodeNode.boundsInRoot
        val tolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue("Synopsis should remain inside the first viewport", synopsis.top >= viewport.top)
        assertTrue(
            "Synopsis should be fully visible in 960x540dp: $synopsis",
            synopsis.height + tolerance >= synopsisNode.size.height,
        )
        assertTrue(
            "Episode selection should be fully visible in 960x540dp: $episode",
            episode.bottom <= viewport.bottom + tolerance &&
                episode.height + tolerance >= episodeNode.size.height,
        )
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
    fun visibleRememberedChildKeepsExactCarouselViewport() {
        val expectedViewport = GridViewportSnapshot(2, 18)
        var latestViewport: GridViewportSnapshot? = null
        composeRule.setContent {
            KaloscopeTheme {
                MediaDetailScreen(
                    session = session(),
                    state = MediaDetailUiState.Content(
                        parent = longSeries(),
                        focusedChildId = 303,
                        childViewport = expectedViewport,
                    ),
                    resumePositionsByMediaId = emptyMap(),
                    onBack = {},
                    onRetry = {},
                    onChildFocused = {},
                    onChildViewportChanged = { latestViewport = it },
                    onPlayParent = { _, _ -> },
                    onPlayChild = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("media-child-card-303").assertIsFocused()
        composeRule.runOnIdle {
            assertEquals(expectedViewport, latestViewport)
        }
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
    fun seriesVerticalBoundaryKeysScrollBeforeChangingFocus() {
        val state = MediaDetailUiState.Content(
            parent = twoEpisodeSeries().copy(
                directors = listOf("林舟"),
                actors = listOf(MediaActor("沈川", "队长", null)),
            ),
            focusedChildId = 302,
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
        val child = composeRule.onNodeWithTag("media-child-card-302").assertIsFocused()
        val detailScroll = composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        )

        child.performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()

        child.assertIsFocused()
        val bottomRange = detailScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(
            "Expected detail content to extend below the first viewport",
            bottomRange.maxValue() > 0f,
        )
        assertEquals(bottomRange.maxValue(), bottomRange.value(), 0f)

        child.performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.waitForIdle()

        child.assertIsFocused()
        val topOffset = detailScroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()
        assertEquals(0f, topOffset, 0f)

        child.performKeyInput { pressKey(Key.DirectionUp) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("播放").assertIsFocused()
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
        composeRule.onNodeWithText("S1E1 - 启程").assertExists()

        composeRule.runOnIdle {
            state = MediaDetailUiState.Content(parent = movieCollection())
        }
        composeRule.onNodeWithText("分段").assertExists()
        composeRule.onNodeWithTag("media-child-card-602").assertTextContains("第一部")
        composeRule.onNodeWithText("S1E1 - 启程").assertDoesNotExist()
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

    private fun focusCastCarousel() {
        val play = composeRule.onNodeWithText("播放").assertIsFocused()
        repeat(2) {
            play.performKeyInput { pressKey(Key.DirectionDown) }
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("cast-carousel").assertIsFocused()
    }

    private fun assertReadablePrimaryPlayIcon(actionLabel: String) {
        val iconBounds = composeRule.onNodeWithTag(
            "detail-primary-play-icon",
            useUnmergedTree = true,
        )
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        val actionBounds = composeRule.onNodeWithText(actionLabel)
            .fetchSemanticsNode()
            .boundsInRoot
        val minimumIconSize = with(composeRule.density) { 24.dp.toPx() }

        assertTrue(
            "Primary play icon width must be at least 24 dp: " +
                "icon=$iconBounds, action=$actionBounds",
            iconBounds.width >= minimumIconSize,
        )
        assertTrue(
            "Primary play icon height must be at least 24 dp: " +
                "icon=$iconBounds, action=$actionBounds",
            iconBounds.height >= minimumIconSize,
        )
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

private fun mixedTitleSeries(): MediaDetail {
    val episode = series().children.single()
    return series().copy(
        children = listOf(
            episode.copy(
                id = 301,
                title = "喵喵们要去海边玩耍",
                episode = 5,
                aired = "2026-07-31",
            ),
            episode.copy(
                id = 302,
                title = "",
                episode = 6,
                aired = "2026-08-07",
            ),
            episode.copy(
                id = 303,
                title = "本喵也是人类",
                episode = 7,
                aired = "2026-08-14",
            ),
        ),
    )
}

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

private fun Bitmap.countPixelsNear(
    expected: Int,
    tolerance: Int = 3,
): Int {
    var matches = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = getPixel(x, y)
            if (
                kotlin.math.abs(AndroidColor.red(expected) - AndroidColor.red(pixel)) <= tolerance &&
                kotlin.math.abs(AndroidColor.green(expected) - AndroidColor.green(pixel)) <= tolerance &&
                kotlin.math.abs(AndroidColor.blue(expected) - AndroidColor.blue(pixel)) <= tolerance
            ) {
                matches += 1
            }
        }
    }
    return matches
}

private fun Bitmap.firstBrightPixelRow(searchWidth: Int): Int {
    val widthLimit = minOf(width, searchWidth)
    for (y in 0 until height) {
        for (x in 0 until widthLimit) {
            val pixel = getPixel(x, y)
            if (
                AndroidColor.red(pixel) >= 128 &&
                AndroidColor.green(pixel) >= 128 &&
                AndroidColor.blue(pixel) >= 128
            ) {
                return y
            }
        }
    }
    return -1
}
