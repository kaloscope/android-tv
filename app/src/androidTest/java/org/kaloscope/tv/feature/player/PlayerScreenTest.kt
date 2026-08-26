package org.kaloscope.tv.feature.player

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.player.PlaybackControllerFactory
import org.kaloscope.tv.core.player.PlaybackPreparationStage

class PlayerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadingShowsResourceStageBelowIndicator() {
        composeRule.setContent {
            KaloscopeTheme {
                val context = LocalContext.current
                PlayerScreen(
                    session = session(),
                    state = PlayerUiState.Loading(PlaybackPreparationStage.Resource),
                    controllerFactory = remember(context) {
                        PlaybackControllerFactory(context)
                    },
                    onProgress = { _, _, _, _ -> },
                    onSelectDefinition = { _, _ -> },
                    onPrevious = {},
                    onNext = {},
                    onRetryExtra = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("player-loading-indicator").assertExists()
        composeRule.onNodeWithText("正在获取资源…").assertExists()
    }

    @Test
    fun danmakuLoadingUsesTheSameFullscreenLayoutWithItsStage() {
        composeRule.setContent {
            KaloscopeTheme {
                val context = LocalContext.current
                PlayerScreen(
                    session = session(),
                    state = PlayerUiState.Loading(PlaybackPreparationStage.Danmaku),
                    controllerFactory = remember(context) {
                        PlaybackControllerFactory(context)
                    },
                    onProgress = { _, _, _, _ -> },
                    onSelectDefinition = { _, _ -> },
                    onPrevious = {},
                    onNext = {},
                    onRetryExtra = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("player-loading-indicator").assertExists()
        composeRule.onNodeWithText("正在获取弹幕…").assertExists()
    }
}

private fun session() = Session(
    server = SavedServer("server-id", "Home", "http://127.0.0.1:8000"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
