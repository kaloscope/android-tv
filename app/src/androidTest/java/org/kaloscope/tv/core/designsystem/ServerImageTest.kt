package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ServerImageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun placeholdersExposeDistinctStableStates() {
        composeRule.setContent {
            androidx.compose.foundation.layout.Row {
                ServerImagePlaceholder(ServerImageVisualState.Loading, "L", Modifier.size(80.dp))
                ServerImagePlaceholder(ServerImageVisualState.Missing, "M", Modifier.size(80.dp))
                ServerImagePlaceholder(ServerImageVisualState.Failed, "F", Modifier.size(80.dp))
            }
        }

        composeRule.onNodeWithTag("server-image-loading").assertExists()
        composeRule.onNodeWithTag("server-image-missing").assertExists()
        composeRule.onNodeWithTag("server-image-failed").assertExists()
    }
}
