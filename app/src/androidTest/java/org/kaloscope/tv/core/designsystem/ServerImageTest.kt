package org.kaloscope.tv.core.designsystem

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.test.captureToImage

class ServerImageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun placeholdersExposeDistinctStableStates() {
        composeRule.setContent {
            androidx.compose.foundation.layout.Row {
                ServerImagePlaceholder(ServerImageVisualState.Loading, Modifier.size(80.dp))
                ServerImagePlaceholder(ServerImageVisualState.Missing, Modifier.size(80.dp))
                ServerImagePlaceholder(ServerImageVisualState.Failed, Modifier.size(80.dp))
            }
        }

        composeRule.onNodeWithTag("server-image-loading").assertExists()
        composeRule.onNodeWithTag("server-image-missing").assertExists()
        composeRule.onNodeWithTag("server-image-failed").assertExists()
    }

    @Test
    fun missingPlaceholderUsesExistingBrokenImageIcon() {
        composeRule.setContent {
            ServerImagePlaceholder(
                state = ServerImageVisualState.Missing,
                modifier = Modifier.size(80.dp),
            )
        }

        composeRule.onNodeWithTag("server-image-broken-icon", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun failedPlaceholderUsesSoftenedIconTint() {
        composeRule.setContent {
            ServerImagePlaceholder(
                state = ServerImageVisualState.Failed,
                modifier = Modifier.size(80.dp),
            )
        }

        val bitmap = composeRule.onNodeWithTag("server-image-failed")
            .captureToImage()
            .asAndroidBitmap()
        val brightestBlue = (0 until bitmap.width).maxOf { x ->
            (0 until bitmap.height).maxOf { y ->
                AndroidColor.blue(bitmap.getPixel(x, y))
            }
        }

        assertTrue(
            "Failed-image icon should stay visible but softer than the full #BAC6E8 tint; " +
                "brightest blue was $brightestBlue",
            brightestBlue in 120..199,
        )
    }
}
