package org.kaloscope.tv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.feature.home.HomeUiState

class MainShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeNavigationReceivesInitialFocus() {
        composeRule.setContent {
            KaloscopeTheme {
                MainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    onRefresh = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(hasText("首页") and hasClickAction()).assertIsFocused()
    }

    @Test
    fun settingsGearStaysSelectedAndShowsCurrentAccount() {
        composeRule.setContent {
            KaloscopeTheme {
                MainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    onRefresh = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNode(hasText("首页") and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithContentDescription("设置")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.onNodeWithText("服务器与账号").assertExists()
        composeRule.onNodeWithText("家庭服务器").assertExists()
        composeRule.onNodeWithText("tv_user").assertExists()
        composeRule.onNodeWithContentDescription("设置").assertIsSelected()
    }

    @Test
    fun unfinishedDestinationsAreNotInteractive() {
        composeRule.setContent {
            KaloscopeTheme {
                MainShell(
                    session = session(),
                    homeState = HomeUiState.Empty,
                    onRefresh = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("网络搜索").assertIsNotEnabled()
        composeRule.onNodeWithText("媒体库").assertIsNotEnabled()
    }
}

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
