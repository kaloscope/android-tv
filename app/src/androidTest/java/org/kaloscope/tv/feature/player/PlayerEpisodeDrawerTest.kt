package org.kaloscope.tv.feature.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser

class PlayerEpisodeDrawerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedEpisodeScrollsIntoViewAndCenterReturnsItsAbsoluteIndex() {
        var selectedIndex = -1
        setDrawer(
            episodes = List(24) { index ->
                episode(
                    index = index,
                    selected = index == 19,
                    showPoster = false,
                )
            },
            onSelect = { selectedIndex = it },
        )

        composeRule.onNodeWithText("Episode 20")
            .assertIsSelected()
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        composeRule.runOnIdle { assertEquals(19, selectedIndex) }
    }

    @Test
    fun localEpisodesRenderMissingPostersWithTheSharedPlaceholder() {
        setDrawer(
            episodes = listOf(
                episode(index = 0, selected = true, showPoster = true),
                episode(index = 1, selected = false, showPoster = true),
            ),
        )

        composeRule.onAllNodesWithTag(
            testTag = "player-episode-poster",
            useUnmergedTree = true,
        ).assertCountEquals(2)
        composeRule.onAllNodesWithTag(
            testTag = "server-image-missing",
            useUnmergedTree = true,
        ).assertCountEquals(2)
    }

    @Test
    fun networkEpisodesDoNotReservePosterSpace() {
        setDrawer(
            episodes = listOf(
                episode(index = 0, selected = true, showPoster = false),
                episode(index = 1, selected = false, showPoster = false),
            ),
        )

        composeRule.onAllNodesWithTag("player-episode-poster")
            .assertCountEquals(0)
        composeRule.onNodeWithText("Episode 1").assertExists()
        composeRule.onNodeWithText("Episode 2").assertExists()
    }

    @Test
    fun episodeDrawerUsesTheLeftSidePanelPosition() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag("drawer-root"),
                ) {
                    PlayerEpisodeDrawer(
                        session = session(),
                        episodes = listOf(
                            episode(index = 0, selected = true, showPoster = false),
                            episode(index = 1, selected = false, showPoster = false),
                        ),
                        onSelect = {},
                        onDismiss = {},
                    )
                }
            }
        }

        val root = composeRule.onNodeWithTag("drawer-root")
            .fetchSemanticsNode()
            .boundsInRoot
        val drawer = composeRule.onNodeWithTag("player-episode-drawer")
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(root.left, drawer.left, 1f)
    }

    @Test
    fun backDismissesTheEpisodeDrawer() {
        var dismissCount = 0
        setDrawer(
            episodes = listOf(
                episode(index = 0, selected = true, showPoster = false),
                episode(index = 1, selected = false, showPoster = false),
            ),
            onDismiss = { dismissCount += 1 },
        )

        InstrumentationRegistry.getInstrumentation().apply {
            waitForIdleSync()
            sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(1, dismissCount) }
    }

    private fun setDrawer(
        episodes: List<PlayerEpisodeEntry>,
        onSelect: (Int) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            KaloscopeTheme {
                PlayerEpisodeDrawer(
                    session = session(),
                    episodes = episodes,
                    onSelect = onSelect,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    private fun episode(
        index: Int,
        selected: Boolean,
        showPoster: Boolean,
    ) = PlayerEpisodeEntry(
        stableId = "episode-$index",
        sourceIndex = index,
        title = "Episode ${index + 1}",
        posterPath = null,
        showPoster = showPoster,
        selected = selected,
    )

    private fun session() = Session(
        server = SavedServer("server-1", "Fixture", "https://media.example.test"),
        token = "fixture-token",
        user = SessionUser(1, "tv", "user"),
    )
}
