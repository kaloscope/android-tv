package org.kaloscope.tv.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.SubtitleSettings
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
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
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
            .assertIsSelected()
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
    fun menuSelectionAndSettingFocusRemainIndependent() {
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Playback,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeResolution = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("播放").assertIsSelected()
        composeRule.onNodeWithText("默认播放模式")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        composeRule.onNodeWithText("播放")
            .assertIsSelected()
            .assertIsNotFocused()
    }

    @Test
    fun focusingNextMenuCategorySelectsItWithoutCenter() {
        var state by mutableStateOf(
            SettingsUiState.Content(
                settings = TvSettings(),
                section = SettingsSection.Playback,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = state,
                    onRetry = {},
                    onSelectSection = { section ->
                        state = state.copy(section = section)
                    },
                    onPlaybackMode = {},
                    onTranscodeResolution = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("播放")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithText("弹幕")
            .assertIsFocused()
            .assertIsSelected()
        composeRule.onNodeWithText("默认开启弹幕").assertExists()
    }

    @Test
    fun movingLeftReturnsToTheSelectedMenuCategory() {
        var state by mutableStateOf(
            SettingsUiState.Content(
                settings = TvSettings(),
                section = SettingsSection.Danmaku,
            ),
        )
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = state,
                    onRetry = {},
                    onSelectSection = { section ->
                        state = state.copy(section = section)
                    },
                    onPlaybackMode = {},
                    onTranscodeResolution = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("默认开启弹幕")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }

        composeRule.onNodeWithText("弹幕")
            .assertIsSelected()
            .assertIsFocused()
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
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
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
    fun serverActionsScrollIntoViewWithDpadAtMainShellHeight() {
        var manages = 0
        var logouts = 0
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
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
                        onDanmakuSettings = {},
                        onSubtitleSettings = {},
                        onStartPage = {},
                        onTestConnection = {},
                        onManageServers = { manages += 1 },
                        onLogout = { logouts += 1 },
                    )
                }
            }
        }

        composeRule.onNodeWithText("测试连接")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("切换或添加服务器")
            .assertIsFocused()
            .assertIsDisplayed()
            .performKeyInput { pressKey(Key.Enter) }
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("退出登录")
            .assertIsFocused()
            .assertIsDisplayed()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(1, manages)
            assertEquals(1, logouts)
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
                    onDanmakuSettings = {},
                    onSubtitleSettings = {},
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
    fun playbackSettingsFitMainShellViewport() {
        composeRule.setContent {
            KaloscopeTheme {
                Box(
                    modifier = Modifier
                        .width(872.dp)
                        .height(416.dp),
                ) {
                    SettingsScreen(
                        session = session(),
                        state = SettingsUiState.Content(TvSettings()),
                        onRetry = {},
                        onSelectSection = {},
                        onPlaybackMode = {},
                        onTranscodeResolution = {},
                        onAutoplayNext = {},
                        onDanmakuSettings = {},
                        onSubtitleSettings = {},
                        onStartPage = {},
                        onTestConnection = {},
                        onManageServers = {},
                        onLogout = {},
                    )
                }
            }
        }

        val viewport = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val firstRow = composeRule.onNodeWithText("默认播放模式")
            .fetchSemanticsNode()
            .boundsInRoot
        val lastRow = composeRule.onNodeWithText("自动播放下一集")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(lastRow.left >= viewport.left)
        assertTrue(lastRow.right <= viewport.right)
        assertTrue(lastRow.top >= viewport.top)
        assertTrue(lastRow.bottom <= viewport.bottom)
        assertTrue(lastRow.height >= firstRow.height)

        listOf(
            "自动播放下一集",
            "当前内容结束且存在下一集时自动继续。",
            "开启",
        ).forEach { text ->
            val layoutResults = mutableListOf<TextLayoutResult>()
            composeRule.onNodeWithText(text, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                    it(layoutResults)
                }
            assertFalse(
                "$text must not overflow its text bounds",
                layoutResults.single().hasVisualOverflow,
            )
        }

        composeRule.onNodeWithText("设置").assertDoesNotExist()
        composeRule.onNodeWithText("仅保存在这台设备上").assertDoesNotExist()

        composeRule.onNodeWithText("默认播放模式")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
            }
        composeRule.onNodeWithText("自动播放下一集").assertIsFocused()
    }

    @Test
    fun danmakuCategoryShowsDefaultsAndUpdatesTheWholeModel() {
        var updatedSettings: DanmakuSettings? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Danmaku,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeResolution = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = { updatedSettings = it },
                    onSubtitleSettings = {},
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("默认开启弹幕").assertExists()
        composeRule.onNodeWithText("弹幕字号").assertExists()
        composeRule.onNodeWithText("滚动速度").assertExists()
        composeRule.onNodeWithText("默认开启弹幕")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        composeRule.runOnIdle {
            assertEquals(null, updatedSettings)
        }

        composeRule.onNodeWithText("默认开启弹幕")
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.runOnIdle {
            assertEquals(false, updatedSettings?.enabled)
        }
    }

    @Test
    fun subtitleCategoryShowsDefaultsAndUpdatesTheWholeModel() {
        var updatedSettings: SubtitleSettings? = null
        composeRule.setContent {
            KaloscopeTheme {
                SettingsScreen(
                    session = session(),
                    state = SettingsUiState.Content(
                        settings = TvSettings(),
                        section = SettingsSection.Subtitle,
                    ),
                    onRetry = {},
                    onSelectSection = {},
                    onPlaybackMode = {},
                    onTranscodeResolution = {},
                    onAutoplayNext = {},
                    onDanmakuSettings = {},
                    onSubtitleSettings = { updatedSettings = it },
                    onStartPage = {},
                    onTestConnection = {},
                    onManageServers = {},
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("默认开启字幕").assertExists()
        composeRule.onNodeWithText("首选语言").assertExists()
        composeRule.onNodeWithText("显示样式").assertExists()
        composeRule.onNodeWithText("字幕字号").assertExists()
        composeRule.onNodeWithText("垂直位置").assertExists()
        composeRule.onNodeWithText("默认开启字幕")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag("subtitle-default-settings").performScrollToIndex(5)
        composeRule.onNodeWithText("时间偏移").assertExists()

        composeRule.runOnIdle {
            assertEquals(false, updatedSettings?.enabled)
        }
    }
}

private fun session() = Session(
    server = SavedServer("server-id", "家庭服务器", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv_user", "user"),
)
