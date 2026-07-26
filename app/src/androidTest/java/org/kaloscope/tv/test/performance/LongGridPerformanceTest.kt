package org.kaloscope.tv.test.performance

import androidx.activity.ComponentActivity
import androidx.core.app.FrameMetricsAggregator
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
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
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun traverseFocusedPair() {
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            composeRule.waitForIdle()
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT)
            composeRule.waitForIdle()
        }

        fun measureTraversal(startTag: String): FrameDurationSummary {
            composeRule.onNodeWithTag(startTag)
                .performSemanticsAction(SemanticsActions.RequestFocus)
                .assertIsDisplayed()
                .assertIsFocused()
            repeat(WARM_UP_TRAVERSALS) {
                traverseFocusedPair()
            }

            val aggregator = FrameMetricsAggregator(FrameMetricsAggregator.TOTAL_DURATION)
            aggregator.add(composeRule.activity)
            repeat(MEASURED_TRAVERSALS) {
                traverseFocusedPair()
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(startTag).assertIsFocused()
            val histogram = aggregator.remove(composeRule.activity)
                ?.get(FrameMetricsAggregator.TOTAL_INDEX)
                ?: error("No total-duration frame metrics")
            return summarizeFrames(histogram)
        }

        val baseline = measureTraversal("media-card-1")
        composeRule.onNodeWithTag("library-results-grid").performScrollToIndex(180)
        val deepGrid = measureTraversal("media-card-181")
        Log.i(
            "KaloscopePerformance",
            "P2 long grid baseline p50=${baseline.p50Millis}ms " +
                "p95=${baseline.p95Millis}ms over100=${baseline.over100MillisRatio * 100}% " +
                "deep p50=${deepGrid.p50Millis}ms p95=${deepGrid.p95Millis}ms " +
                "over100=${deepGrid.over100MillisRatio * 100}% frames=${deepGrid.totalFrames}",
        )
        InstrumentationRegistry.getInstrumentation().sendStatus(
            2,
            android.os.Bundle().apply {
                putString(
                    "stream",
                    "P2 long grid: baseline p95=${baseline.p95Millis}ms " +
                        "deep p95=${deepGrid.p95Millis}ms " +
                        "deep over100=${deepGrid.over100MillisRatio * 100}% " +
                        "frames=${deepGrid.totalFrames}\n",
                )
            },
        )
        assertTrue(
            "Only ${baseline.totalFrames} baseline frames were measured",
            baseline.totalFrames >= MIN_MEASURED_FRAMES,
        )
        assertTrue(
            "Only ${deepGrid.totalFrames} deep-grid frames were measured",
            deepGrid.totalFrames >= MIN_MEASURED_FRAMES,
        )
        val allowedP95 = maxOf(
            RESPONSIVE_P95_LIMIT_MILLIS,
            baseline.p95Millis + MAX_DEEP_P95_OVERHEAD_MILLIS,
        )
        assertTrue(
            "Deep-grid P95 was ${deepGrid.p95Millis} ms; baseline was " +
                "${baseline.p95Millis} ms",
            deepGrid.p95Millis <= allowedP95,
        )
        if (baseline.p95Millis <= RESPONSIVE_P95_LIMIT_MILLIS) {
            val allowedSlowFrameRatio = maxOf(
                RESPONSIVE_SLOW_FRAME_RATIO,
                baseline.over100MillisRatio + MAX_DEEP_SLOW_FRAME_RATIO_OVERHEAD,
            )
            assertTrue(
                "Deep-grid slow-frame ratio was ${deepGrid.over100MillisRatio}; " +
                    "baseline was ${baseline.over100MillisRatio}",
                deepGrid.over100MillisRatio <= allowedSlowFrameRatio,
            )
        }
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
        focusedMediaId = null,
        gridViewport = GridViewportSnapshot.Top,
    )
}

private const val WARM_UP_TRAVERSALS = 3
private const val MEASURED_TRAVERSALS = 12
private const val MIN_MEASURED_FRAMES = 40
private const val RESPONSIVE_P95_LIMIT_MILLIS = 100
private const val MAX_DEEP_P95_OVERHEAD_MILLIS = 32
private const val RESPONSIVE_SLOW_FRAME_RATIO = 0.20
private const val MAX_DEEP_SLOW_FRAME_RATIO_OVERHEAD = 0.10

private fun performanceSession() = Session(
    server = SavedServer("performance", "Performance", "http://127.0.0.1:8000"),
    token = "fixture",
    user = SessionUser(1, "performance", "user"),
)
