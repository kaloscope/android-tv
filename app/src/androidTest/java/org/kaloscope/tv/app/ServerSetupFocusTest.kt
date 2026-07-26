package org.kaloscope.tv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.feature.server.ServerSetupState

class ServerSetupFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun serverSetupShowsTwoStepProgress() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithText("配置服务器").assertExists()
        composeRule.onNodeWithText("登录账户").assertExists()
        composeRule.onNodeWithText("连接到你的服务器").assertExists()
    }

    @Test
    fun successfulConnectionPrefixesVersionWithV() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(
                        name = "家庭服务器",
                        url = "http://192.168.1.2:8000",
                        verifiedOrigin = "http://192.168.1.2:8000",
                        serverVersion = "1.0.0",
                    ),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithText("v1.0.0", substring = true).assertExists()
    }

    @Test
    fun testConnectionLabelIsHorizontallyCentered() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        val buttonCenter = composeRule.onNodeWithText("测试连接")
            .fetchSemanticsNode()
            .boundsInRoot
            .center
            .x
        val labelCenter = composeRule.onNodeWithText(
            text = "测试连接",
            useUnmergedTree = true,
        ).fetchSemanticsNode()
            .boundsInRoot
            .center
            .x

        assertTrue(abs(buttonCenter - labelCenter) < 1f)
    }

    @Test
    fun serverNameEntersEditModeOnlyAfterClick() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithTag("server-name-editor").assertDoesNotExist()
        composeRule.onNodeWithTag("server-name-selector")
            .assertIsFocused()
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("server-name-editor").assertExists()
        composeRule.onNodeWithText("例如：家庭服务器").assertIsFocused()
    }

    @Test
    fun serverNameReturnsToNavigationModeOnBack() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithTag("server-name-selector")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("server-name-editor")
            .performKeyInput { pressKey(Key.Back) }

        composeRule.onNodeWithTag("server-name-editor").assertDoesNotExist()
        composeRule.onNodeWithTag("server-name-selector").assertIsFocused()
    }

    @Test
    fun dpadDownMovesFromServerNameToUrl() {
        composeRule.setContent {
            KaloscopeTheme {
                ServerSetupScreen(
                    savedServers = emptyList(),
                    state = ServerSetupState(),
                    onNameChange = {},
                    onUrlChange = {},
                    onTest = {},
                    onSave = {},
                    onSelectServer = {},
                )
            }
        }

        composeRule.onNodeWithText("例如：家庭服务器")
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithText("例如：http://192.168.1.2:8000").assertIsFocused()
    }
}
