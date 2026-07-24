package org.kaloscope.tv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.feature.server.ServerSetupState

class ServerSetupFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

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
            .performClick()
            .performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithText("例如：http://192.168.1.2:8000").assertIsFocused()
    }
}
