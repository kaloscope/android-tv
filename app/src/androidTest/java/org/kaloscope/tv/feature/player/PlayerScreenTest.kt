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

class PlayerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadingShowsOnlyCenteredIndicator() {
        composeRule.setContent {
            KaloscopeTheme {
                val context = LocalContext.current
                PlayerScreen(
                    session = session(),
                    state = PlayerUiState.Loading,
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
        composeRule.onNodeWithText("正在准备播放").assertDoesNotExist()
        composeRule.onNodeWithText("正在加载播放地址、字幕和弹幕。").assertDoesNotExist()
    }
}

private fun session() = Session(
    server = SavedServer("server-id", "Home", "http://127.0.0.1:8000"),
    token = "fixture-token",
    user = SessionUser(1, "tv", "user"),
)
