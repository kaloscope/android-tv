package org.kaloscope.tv.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class LoadingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bootstrapLoadingUsesCenteredIndicator() {
        composeRule.setContent {
            KaloscopeTheme {
                LoadingScreen()
            }
        }

        composeRule.onNodeWithTag("app-loading-indicator").assertExists()
        composeRule.onNodeWithText("正在连接服务器…").assertDoesNotExist()
    }
}
