package org.kaloscope.tv.core.designsystem

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.network.ServerImagePolicy
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
            "Failed-image icon should stay visible without a bright highlight; " +
                "brightest blue was $brightestBlue",
            brightestBlue in 120..199,
        )
    }

    @Test
    fun skeletonFreezesDuringHandoffAndMovesAgainWhenLoadingResumes() {
        composeRule.mainClock.autoAdvance = false
        val state = mutableStateOf(ServerImageVisualState.Loading)
        composeRule.setContent {
            ServerImagePlaceholder(state.value, Modifier.size(80.dp))
        }

        composeRule.mainClock.advanceTimeBy(416)
        composeRule.runOnIdle { state.value = ServerImageVisualState.Success }
        composeRule.mainClock.advanceTimeByFrame()
        val frozen = composeRule.onNodeWithTag("server-image-handoff")
            .captureToImage().asAndroidBitmap()
        composeRule.mainClock.advanceTimeBy(160)
        val later = composeRule.onNodeWithTag("server-image-handoff")
            .captureToImage().asAndroidBitmap()
        assertTrue("Skeleton should stay still during the image fade", frozen.sameAs(later))

        composeRule.runOnIdle { state.value = ServerImageVisualState.Loading }
        composeRule.mainClock.advanceTimeBy(160)
        val resumed = composeRule.onNodeWithTag("server-image-loading")
            .captureToImage().asAndroidBitmap()
        assertFalse("Loading should resume the shimmer", frozen.sameAs(resumed))
    }

    @Test
    fun successfulLoadRetainsSkeletonUntilImageIsOpaqueWithoutReloading() {
        MockWebServer().use { server ->
            val releaseImage = CountDownLatch(1)
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    if (releaseImage.await(10, TimeUnit.SECONDS)) {
                        imageResponse()
                    } else {
                        MockResponse().setResponseCode(503)
                    }
            }
            server.start()
            val session = Session(
                server = SavedServer("fixture-server", "Test", server.url("/").toString()),
                token = "fixture-token",
                user = SessionUser(1, "fixture-user", "user"),
            )
            val description = mutableStateOf("Image fixture")
            composeRule.mainClock.autoAdvance = false
            try {
                composeRule.setContent {
                    ServerImage(
                        session = session,
                        rawValue = server.url("/poster.png").toString(),
                        contentDescription = description.value,
                        policy = ServerImagePolicy.Direct,
                        modifier = Modifier.size(80.dp).testTag("image-frame"),
                    )
                }
                composeRule.waitUntil(10_000) {
                    composeRule.mainClock.advanceTimeByFrame()
                    server.requestCount == 1
                }
                composeRule.onNodeWithTag("server-image-loading").assertExists()
                composeRule.mainClock.advanceTimeBy(416)
                releaseImage.countDown()

                composeRule.waitUntil(10_000) {
                    composeRule.mainClock.advanceTimeByFrame()
                    composeRule.onAllNodesWithTag("server-image-handoff")
                        .fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.onNodeWithTag("server-image-loading").assertDoesNotExist()
                composeRule.mainClock.advanceTimeBy(64)
                composeRule.onNodeWithTag("server-image-handoff").assertExists()
                val entering = composeRule.onNodeWithTag("image-frame")
                    .captureToImage().asAndroidBitmap()
                val enteringGreen = AndroidColor.green(
                    entering.getPixel(entering.width / 2, entering.height / 2),
                )
                assertTrue("Image should be partway through its fade", enteringGreen in 80..254)

                composeRule.mainClock.advanceTimeBy(200)
                composeRule.onNodeWithTag("server-image-handoff").assertDoesNotExist()
                composeRule.onNodeWithTag("server-image-failed").assertDoesNotExist()
                val completed = composeRule.onNodeWithTag("image-frame")
                    .captureToImage().asAndroidBitmap()
                assertEquals(
                    AndroidColor.GREEN,
                    completed.getPixel(completed.width / 2, completed.height / 2),
                )

                composeRule.runOnIdle { description.value = "Updated fixture" }
                composeRule.mainClock.advanceTimeBy(32)
                composeRule.onNodeWithContentDescription("Updated fixture").assertExists()
                composeRule.onNodeWithTag("server-image-handoff").assertDoesNotExist()
                assertEquals(1, server.requestCount)
                val request = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
                assertEquals("/poster.png", request.path)
                assertEquals("Token fixture-token", request.getHeader("Authorization"))
            } finally {
                releaseImage.countDown()
                composeRule.mainClock.autoAdvance = true
            }
        }
    }

    private fun imageResponse(): MockResponse {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val bytes = try {
            bitmap.eraseColor(AndroidColor.GREEN)
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
        return MockResponse()
            .setHeader("Content-Type", "image/png")
            .setBody(Buffer().write(bytes))
    }
}
