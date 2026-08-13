package org.kaloscope.tv.core.designsystem

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.kaloscope.tv.app.KaloscopeTheme
import org.kaloscope.tv.core.model.AccentColor

class KaloscopeSwitchIndicatorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun checkedSwitchFillsTrackWithCurrentAccentColor() {
        setSwitchContent(checked = true, accentColor = AccentColor.Green)

        val track = composeRule.onNodeWithTag("setting-switch-indicator")
            .captureToImage()
            .asAndroidBitmap()
        val trackPixel = track.getPixel(
            (track.width / 5).coerceIn(0, track.width - 1),
            track.height / 2,
        )

        assertColorNear(
            expected = AndroidColor.rgb(0x4D, 0xD9, 0x90),
            actual = trackPixel,
        )
    }

    @Test
    fun switchUsesCompactReferenceGeometry() {
        composeRule.setContent {
            KaloscopeTheme {
                CompositionLocalProvider(LocalContentColor provides OnBackground) {
                    Column(
                        modifier = Modifier.background(Background),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 20.dp)
                                .background(OnBackground)
                                .testTag("compact-switch-reference"),
                        )
                        KaloscopeSwitchIndicator(
                            checked = false,
                            modifier = Modifier.testTag("compact-switch-under-test"),
                        )
                    }
                }
            }
        }

        val track = composeRule.onNodeWithTag("compact-switch-under-test")
            .captureToImage()
            .asAndroidBitmap()
        val reference = composeRule.onNodeWithTag("compact-switch-reference")
            .captureToImage()
            .asAndroidBitmap()

        assertEquals(reference.width, track.width)
        assertEquals(reference.height, track.height)
    }

    @Test
    fun thumbLeavesVisibleGapWithinTrack() {
        composeRule.setContent {
            KaloscopeTheme {
                CompositionLocalProvider(LocalContentColor provides OnBackground) {
                    Column(
                        modifier = Modifier.background(Background),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(OnBackground)
                                .testTag("gapped-thumb-reference"),
                        )
                        KaloscopeSwitchIndicator(checked = false)
                    }
                }
            }
        }

        val thumb = composeRule.onNodeWithTag("setting-switch-thumb")
            .captureToImage()
            .asAndroidBitmap()
        val reference = composeRule.onNodeWithTag("gapped-thumb-reference")
            .captureToImage()
            .asAndroidBitmap()

        assertEquals(reference.width, thumb.width)
        assertEquals(reference.height, thumb.height)
    }

    @Test
    fun checkedSwitchUsesLightThumb() {
        setSwitchContent(checked = true, accentColor = AccentColor.Green)

        val thumb = composeRule.onNodeWithTag("setting-switch-thumb")
            .captureToImage()
            .asAndroidBitmap()

        assertColorNear(
            expected = AndroidColor.rgb(0xF7, 0xF8, 0xFC),
            actual = thumb.getPixel(thumb.width / 2, thumb.height / 2),
        )
    }

    @Test
    fun uncheckedSwitchLeavesTrackInteriorTransparent() {
        setSwitchContent(
            checked = false,
            modifier = Modifier.testTag("unchecked-switch"),
        )

        val track = composeRule.onNodeWithTag("unchecked-switch")
            .captureToImage()
            .asAndroidBitmap()

        assertColorNear(
            expected = AndroidColor.rgb(0x06, 0x09, 0x12),
            actual = track.getPixel(track.width * 3 / 4, track.height / 2),
        )
    }

    @Test
    fun uncheckedSwitchUsesMutedOutline() {
        setSwitchContent(
            checked = false,
            modifier = Modifier.testTag("outlined-switch"),
        )

        val track = composeRule.onNodeWithTag("outlined-switch")
            .captureToImage()
            .asAndroidBitmap()

        assertColorNear(
            expected = AndroidColor.rgb(0x7F, 0x81, 0x87),
            actual = track.getPixel(track.width / 2, 1.coerceAtMost(track.height - 1)),
            tolerance = 8,
        )
    }

    @Test
    fun thumbStaysVerticallyCenteredWithinTrack() {
        setSwitchContent(
            checked = false,
            modifier = Modifier.testTag("centered-switch"),
        )

        val trackBounds = composeRule.onNodeWithTag("centered-switch")
            .fetchSemanticsNode()
            .boundsInRoot
        val thumbBounds = composeRule.onNodeWithTag("setting-switch-thumb")
            .fetchSemanticsNode()
            .boundsInRoot
        val topGap = thumbBounds.top - trackBounds.top
        val bottomGap = trackBounds.bottom - thumbBounds.bottom

        assertEquals(topGap, bottomGap, 0.5f)
    }

    @Test
    fun uncheckedSwitchUsesMutedThumb() {
        setSwitchContent(checked = false)

        val thumb = composeRule.onNodeWithTag("setting-switch-thumb")
            .captureToImage()
            .asAndroidBitmap()

        assertColorNear(
            expected = AndroidColor.rgb(0x7F, 0x81, 0x87),
            actual = thumb.getPixel(thumb.width / 2, thumb.height / 2),
            tolerance = 4,
        )
    }

    private fun setSwitchContent(
        checked: Boolean,
        accentColor: AccentColor = AccentColor.Blue,
        modifier: Modifier = Modifier,
    ) {
        composeRule.setContent {
            KaloscopeTheme(accentColor = accentColor) {
                CompositionLocalProvider(LocalContentColor provides OnBackground) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Background),
                        contentAlignment = Alignment.Center,
                    ) {
                        KaloscopeSwitchIndicator(
                            checked = checked,
                            modifier = modifier,
                        )
                    }
                }
            }
        }
    }
}

private fun assertColorNear(
    expected: Int,
    actual: Int,
    tolerance: Int = 3,
) {
    val channelDifferences = listOf(
        kotlin.math.abs(AndroidColor.red(expected) - AndroidColor.red(actual)),
        kotlin.math.abs(AndroidColor.green(expected) - AndroidColor.green(actual)),
        kotlin.math.abs(AndroidColor.blue(expected) - AndroidColor.blue(actual)),
    )
    assertTrue(
        "Expected ${Integer.toHexString(expected)} but was ${Integer.toHexString(actual)}",
        channelDifferences.all { it <= tolerance },
    )
}
