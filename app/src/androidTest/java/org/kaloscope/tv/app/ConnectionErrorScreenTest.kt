package org.kaloscope.tv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.SavedServer

class ConnectionErrorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dpadMovesFromRetryToSwitchServerAndInvokesIt() {
        var retries = 0
        var serverSwitches = 0
        composeRule.setContent {
            KaloscopeTheme {
                ConnectionErrorScreen(
                    server = SavedServer(
                        id = "server-id",
                        name = "家庭服务器",
                        origin = "https://media.example",
                    ),
                    error = AppError.Timeout,
                    onRetry = { retries += 1 },
                    onSwitchServer = { serverSwitches += 1 },
                )
            }
        }

        composeRule.onNodeWithText("重试")
            .assertIsDisplayed()
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("切换服务器")
            .assertIsDisplayed()
            .assertIsFocused()
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(0, retries)
        assertEquals(1, serverSwitches)
    }
}
