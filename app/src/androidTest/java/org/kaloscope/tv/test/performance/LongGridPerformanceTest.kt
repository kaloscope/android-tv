package org.kaloscope.tv.test.performance

import androidx.activity.ComponentActivity
import androidx.core.app.FrameMetricsAggregator
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import android.util.Log
import android.view.KeyEvent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.feature.library.LibraryItemsState
import org.kaloscope.tv.feature.library.LibraryScreen
import org.kaloscope.tv.feature.library.LibraryUiState

class LongGridPerformanceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun twoHundredItemGridKeepsFocusResponsive() {
        composeRule.setContent {
            KaloscopeTheme {
                LibraryScreen(
                    session = performanceSession(),
                    state = performanceState(),
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
        composeRule.onNodeWithTag("library-results-grid").performScrollToIndex(180)
        composeRule.onNodeWithTag("media-card-181")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsDisplayed()

        val aggregator = FrameMetricsAggregator(FrameMetricsAggregator.TOTAL_DURATION)
        aggregator.add(composeRule.activity)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        repeat(60) {
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT)
        }
        composeRule.waitForIdle()
        val histogram = aggregator.remove(composeRule.activity)
            ?.get(FrameMetricsAggregator.TOTAL_INDEX)
            ?: error("No total-duration frame metrics")
        val summary = summarizeFrames(histogram)
        Log.i(
            "KaloscopePerformance",
            "P2 long grid p50=${summary.p50Millis}ms p95=${summary.p95Millis}ms " +
                "over100=${summary.over100MillisRatio * 100}% frames=${summary.totalFrames}",
        )
        InstrumentationRegistry.getInstrumentation().sendStatus(
            2,
            android.os.Bundle().apply {
                putString(
                    "stream",
                    "P2 long grid: p50=${summary.p50Millis}ms " +
                        "p95=${summary.p95Millis}ms " +
                        "over100=${summary.over100MillisRatio * 100}% " +
                        "frames=${summary.totalFrames}\n",
                )
            },
        )
        assertTrue("P95 was ${summary.p95Millis} ms", summary.p95Millis <= 100)
        assertTrue(
            "Slow-frame ratio was ${summary.over100MillisRatio}",
            summary.over100MillisRatio <= 0.20,
        )
    }
}

private fun performanceState(): LibraryUiState.Content {
    val items = (1..200).map { id ->
        MediaSummary(
            id = id.toLong(),
            title = "媒体 $id",
            path = "/media/$id",
            posterPath = null,
            backdropPath = null,
            year = 2026,
            rating = 8.0,
            season = null,
            episode = null,
        )
    }
    return LibraryUiState.Content(
        libraries = listOf(MediaLibrary(1, "性能媒体库", MediaLibraryType.Movie)),
        selectedLibraryId = 1,
        items = LibraryItemsState.Content(
            items = items,
            total = items.size,
            pageNumber = 1,
            hasNext = false,
        ),
        focusedMediaId = 181,
        gridViewport = GridViewportSnapshot(180, 0),
    )
}

private fun performanceSession() = Session(
    server = SavedServer("performance", "Performance", "http://127.0.0.1:8000"),
    token = "fixture",
    user = SessionUser(1, "performance", "user"),
)
