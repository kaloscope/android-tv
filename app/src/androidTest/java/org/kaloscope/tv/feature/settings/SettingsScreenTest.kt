package org.kaloscope.tv.feature.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackMode

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playbackCategoryHasInitialFocusAndChoiceUpdatesSetting() {
        var selectedMode: PlaybackMode? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(TvSettings()),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = { selectedMode = it },
                    onTranscodeResolution = {},
                    onAutoplayNext = {},
                    onDanmakuEnabled = {},
                    onSubtitleEnabled = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("播放").assertIsFocused()
        composeRule.onNodeWithText("默认播放模式")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("自动")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }
            .assertIsFocused()
        composeRule.onNodeWithText("直连")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(PlaybackMode.Direct, selectedMode)
        }
    }

    @Test
    fun serverCategoryExposesRealAccountActions() {
        var manages = 0
        var tests = 0
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.ServerAccount,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeResolution = {},
                    onAutoplayNext = {},
                    onDanmakuEnabled = {},
                    onSubtitleEnabled = {},
                    onStartPage = {},
                    onTestConnection = { tests += 1 },
                    onManageServers = { manages += 1 },
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("家庭服务器").assertExists()
        composeRule.onNodeWithText("tv_user").assertExists()
        composeRule.onNodeWithText("切换或添加服务器")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("测试连接")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, manages)
            assertEquals(1, tests)
        }
    }

    @Test
    fun loadErrorFocusesRetryAndInvokesCallback() {
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Error(AppError.Offline),
                    onRetry = { retries += 1 },
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeResolution = {},
                    onAutoplayNext = {},
                    onDanmakuEnabled = {},
                    onSubtitleEnabled = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
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
    fun settingRowsStayInsideTvViewport() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(TvSettings()),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeResolution = {},
                    onAutoplayNext = {},
                    onDanmakuEnabled = {},
                    onSubtitleEnabled = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        val viewport = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val settingRow = composeRule.onNodeWithText("默认播放模式")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(settingRow.left >= viewport.left)
        assertTrue(settingRow.right <= viewport.right)
    }
}

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
