package org.kaloscope.tv.test

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage as captureComposeToImage
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.ceil

/** Retries only Compose WindowCapture's fixed redraw timeout; all other failures propagate. */
internal fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
    var lastFailure: ComposeTimeoutException? = null
    repeat(3) { attempt ->
        try {
            return captureComposeToImage()
        } catch (failure: ComposeTimeoutException) {
            lastFailure = failure
            if (attempt < 2) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                SystemClock.sleep(100)
            }
        }
    }
    throw checkNotNull(lastFailure)
}

/**
 * Uses the device framebuffer for pixel assertions because Compose WindowCapture's fixed redraw
 * timeout is unreliable on the API 28 TV emulator. Callers must settle any manual Compose clock
 * before capturing.
 */
internal fun SemanticsNodeInteraction.captureScreenRegion(): Bitmap {
    val bounds = fetchSemanticsNode().boundsInRoot
    val screenshot = captureDeviceScreen()
    val left = bounds.left.toInt().coerceIn(0, screenshot.width - 1)
    val top = bounds.top.toInt().coerceIn(0, screenshot.height - 1)
    val right = ceil(bounds.right.toDouble()).toInt().coerceIn(left + 1, screenshot.width)
    val bottom = ceil(bounds.bottom.toDouble()).toInt().coerceIn(top + 1, screenshot.height)
    val region = Bitmap.createBitmap(
        screenshot,
        left,
        top,
        right - left,
        bottom - top,
    )
    if (region !== screenshot) screenshot.recycle()
    return region
}

internal fun captureDeviceScreen(): Bitmap {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    return instrumentation.uiAutomation.takeScreenshot()
}
