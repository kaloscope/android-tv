package org.kaloscope.tv.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.R

class StorageErrorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun retryRemainsReachableAndRegainsFocusAfterAnotherFailure() {
        var showError by mutableStateOf(true)
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                if (showError) {
                    StorageErrorScreen(
                        title = stringResource(R.string.bootstrap_storage_error_title),
                        description = stringResource(R.string.bootstrap_storage_error_description),
                        onRetry = {
                            retries += 1
                            showError = false
                        },
                    )
                } else {
                    LoadingScreen()
                }
            }
        }

        composeRule.onNodeWithText("无法访问本地配置，请重试")
            .assertIsDisplayed()
        val retry = composeRule.onNodeWithText("重试")
        retry.assertIsDisplayed().assertIsFocused()
        listOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)
            .forEach { key ->
                retry.performKeyInput { pressKey(key) }
                retry.assertIsFocused()
            }
        retry.performKeyInput { pressKey(Key.DirectionCenter) }
        retry.assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, retries)
            showError = true
        }

        retry.assertIsDisplayed().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        retry.assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(2, retries)
        }
    }

    @Test
    fun serverListErrorShowsTheFailedOperationAndFocusesRetry() {
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                StorageErrorScreen(
                    title = stringResource(R.string.server_list_error_title),
                    description = stringResource(R.string.server_list_error_description),
                    onRetry = { retries += 1 },
                )
            }
        }

        composeRule.onNodeWithText("无法加载服务器列表").assertIsDisplayed()
        composeRule.onNodeWithText("无法读取已保存的服务器，请重试")
            .assertIsDisplayed()
        composeRule.onNodeWithText("重试")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }

    @Test
    fun sessionClearErrorIdentifiesTheServerAndFocusesRetry() {
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                StorageErrorScreen(
                    title = stringResource(R.string.session_clear_error_title),
                    description = stringResource(R.string.session_clear_error_server, "Server B"),
                    onRetry = { retries += 1 },
                )
            }
        }

        composeRule.onNodeWithText("退出登录失败").assertIsDisplayed()
        composeRule.onNodeWithText("无法清除 Server B 的登录状态，请重试")
            .assertIsDisplayed()
        composeRule.onNodeWithText("重试")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }

    @Test
    fun selectionErrorIdentifiesTheServerAndFocusesRetry() {
        var retries = 0
        composeRule.setContent {
            KaloscopeTheme {
                StorageErrorScreen(
                    title = stringResource(R.string.server_selection_error_title),
                    description = stringResource(R.string.server_selection_error_server, "Server B"),
                    onRetry = { retries += 1 },
                )
            }
        }

        composeRule.onNodeWithText("切换服务器失败").assertIsDisplayed()
        composeRule.onNodeWithText("无法访问 Server B 的本地配置，请重试")
            .assertIsDisplayed()
        composeRule.onNodeWithText("重试")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle {
            assertEquals(1, retries)
        }
    }
}
