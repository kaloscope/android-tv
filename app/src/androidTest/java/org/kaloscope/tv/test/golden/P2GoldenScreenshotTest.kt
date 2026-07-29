package org.kaloscope.tv.test.golden

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.app.ServerSetupScreen
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.ServerImagePlaceholder
import org.kaloscope.tv.core.designsystem.ServerImageVisualState
import org.kaloscope.tv.core.designsystem.TvSearchField
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
import org.kaloscope.tv.feature.server.ServerSetupState

class P2GoldenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun libraryGridMatchesCurrentResolution() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KaloscopeTheme {
                KaloscopeBackground {
                    LibraryScreen(
                        session = session(),
                        state = libraryState(),
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
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        val width = Resources.getSystem().displayMetrics.widthPixels
        assertGolden("library-$width", composeRule.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun searchCursorMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.mainClock.autoAdvance = false
        var query by mutableStateOf("Kaloscope")
        composeRule.setContent {
            KaloscopeTheme {
                TvSearchField(
                    value = query,
                    hint = "搜索",
                    onValueChange = { query = it },
                    onSearch = {},
                    modifier = Modifier.testTag("golden-search"),
                )
            }
        }
        composeRule.onNodeWithTag("golden-search")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.mainClock.advanceTimeBy(100)
        assertGolden(
            "search-cursor-1920",
            composeRule.onNodeWithTag("golden-search").captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun searchSelectionMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        var query by mutableStateOf("Kaloscope")
        composeRule.setContent {
            KaloscopeTheme {
                TvSearchField(
                    value = query,
                    hint = "搜索",
                    onValueChange = { query = it },
                    onSearch = {},
                    modifier = Modifier.testTag("golden-search-selection"),
                )
            }
        }
        composeRule.onNodeWithTag("golden-search-selection")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
            .performSemanticsAction(SemanticsActions.SetSelection) {
                it(1, 5, false)
            }
        assertGolden(
            "search-selection-1920",
            composeRule.onNodeWithTag("golden-search-selection")
                .captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun imageStatesMatch1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                Column(Modifier.fillMaxSize()) {
                    ServerImagePlaceholder(ServerImageVisualState.Loading, "L")
                    ServerImagePlaceholder(ServerImageVisualState.Missing, "无")
                    ServerImagePlaceholder(ServerImageVisualState.Failed, "F")
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(600)
        assertGolden("image-states-1920", composeRule.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun controlStatesMatch1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.mainClock.autoAdvance = false
        var spec by mutableStateOf(controlStateGoldenSpecs().first())
        composeRule.setContent {
            KaloscopeTheme {
                ControlStateGoldenCell(
                    spec = spec,
                    modifier = Modifier.testTag("control-state-cell"),
                )
            }
        }

        val cells = controlStateGoldenSpecs().map { next ->
            composeRule.runOnIdle { spec = next }
            composeRule.mainClock.advanceTimeBy(220)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("control-state-cell")
                .captureToImage()
                .asAndroidBitmap()
        }
        assertGolden("control-states-1920", stitchControlStateCells(cells))
    }

    @Test
    fun serverDeleteFocusMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                GoldenServerSetup()
            }
        }

        composeRule.onNodeWithTag("saved-server-golden-server")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("delete-server-golden-server").assertIsFocused()

        assertGolden(
            "server-delete-focus-1920",
            composeRule.onRoot().captureToImage().asAndroidBitmap(),
        )
    }

    @Test
    fun serverDeletionDialogMatches1080p() {
        if (Resources.getSystem().displayMetrics.widthPixels != 1920) return
        composeRule.setContent {
            KaloscopeTheme {
                GoldenServerSetup()
            }
        }

        composeRule.onNodeWithTag("saved-server-golden-server")
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag("delete-server-golden-server")
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("confirm-dialog-cancel").assertIsFocused()

        assertGolden(
            "server-deletion-dialog-1920",
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(),
        )
    }
}

@Composable
private fun GoldenServerSetup() {
    ServerSetupScreen(
        savedServers = listOf(
            SavedServer(
                id = "golden-server",
                name = "家庭服务器",
                origin = "https://home.example",
            ),
        ),
        state = ServerSetupState(),
        onNameChange = {},
        onUrlChange = {},
        onTest = {},
        onSave = {},
        onSelectServer = {},
    )
}

private fun libraryState(): LibraryUiState.Content {
    val items = (1..30).map { id ->
        MediaSummary(
            id = id.toLong(),
            title = "固定媒体标题 $id",
            path = "/media/$id",
            posterPath = null,
            backdropPath = "/unused/$id",
            year = 2026,
            rating = if (id % 3 == 0) 8.6 else null,
            season = null,
            episode = null,
        )
    }
    return LibraryUiState.Content(
        libraries = listOf(MediaLibrary(1, "固定媒体库", MediaLibraryType.Movie)),
        selectedLibraryId = 1,
        items = LibraryItemsState.Content(
            items = items,
            total = items.size,
            pageNumber = 1,
            hasNext = false,
        ),
        focusedMediaId = 1,
        gridViewport = GridViewportSnapshot.Top,
    )
}

private fun session() = Session(
    server = SavedServer("golden", "Golden", "http://127.0.0.1:8000"),
    token = "fixture",
    user = SessionUser(1, "golden", "user"),
)
