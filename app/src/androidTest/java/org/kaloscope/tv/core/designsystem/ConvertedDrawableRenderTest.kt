package org.kaloscope.tv.core.designsystem

import android.graphics.Color as AndroidColor
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.R
import org.kaloscope.tv.test.captureToImage

class ConvertedDrawableRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compoundSourceIconsKeepExpectedTransparentInteriors() {
        composeRule.setContent {
            Row {
                cutoutProbes.forEach { probe ->
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(Color(0xFF101A2E)),
                    ) {
                        Icon(
                            painter = painterResource(probe.resource),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(96.dp)
                                .testTag(probe.tag),
                        )
                    }
                }
            }
        }

        cutoutProbes.forEach { probe ->
            val bitmap = composeRule.onNodeWithTag(
                testTag = probe.tag,
                useUnmergedTree = true,
            ).captureToImage().asAndroidBitmap()
            val sampleX = (bitmap.width * probe.viewportX / 24f).toInt()
                .coerceIn(0, bitmap.width - 1)
            val sampleY = (bitmap.height * probe.viewportY / 24f).toInt()
                .coerceIn(0, bitmap.height - 1)
            val interior = bitmap.getPixel(sampleX, sampleY)
            val brightestRed = (0 until bitmap.height).maxOf { y ->
                (0 until bitmap.width).maxOf { x ->
                    AndroidColor.red(bitmap.getPixel(x, y))
                }
            }

            assertTrue(
                "${probe.tag} expected a transparent interior at " +
                    "(${probe.viewportX}, ${probe.viewportY})",
                AndroidColor.red(interior) < 80,
            )
            assertTrue("${probe.tag} outline should remain visible", brightestRed > 200)
        }
    }

    private data class CutoutProbe(
        val tag: String,
        @DrawableRes val resource: Int,
        val viewportX: Float,
        val viewportY: Float,
    )

    private companion object {
        val cutoutProbes = listOf(
            CutoutProbe("audited-info-icon", R.drawable.ic_info, 12f, 5f),
            CutoutProbe("audited-delete-icon", R.drawable.ic_delete, 12f, 12f),
            CutoutProbe("audited-delete-left-interior", R.drawable.ic_delete, 8f, 12f),
            CutoutProbe("audited-image-broken-icon", R.drawable.ic_image_broken, 8f, 8f),
        )
    }
}
