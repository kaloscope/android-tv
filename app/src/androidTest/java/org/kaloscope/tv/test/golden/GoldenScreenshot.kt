package org.kaloscope.tv.test.golden

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.fail

internal fun assertGolden(name: String, actual: Bitmap) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val arguments = InstrumentationRegistry.getArguments()
    val outputDirectory = File(
        instrumentation.targetContext.getExternalFilesDir(null),
        "goldens",
    ).apply { mkdirs() }
    val actualFile = File(outputDirectory, "$name.png")
    if (arguments.getString("updateGoldens") == "true") {
        FileOutputStream(actualFile).use {
            actual.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return
    }
    val expected = runCatching {
        instrumentation.context.assets.open("goldens/$name.png").use(BitmapFactory::decodeStream)
    }.getOrElse {
        fail("Missing golden assets/goldens/$name.png; run scripts/update-tv-goldens.sh")
        return
    }
    val comparison = compareGolden(expected, actual)
    if (!comparison.passed) {
        FileOutputStream(actualFile).use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val diffFile = File(outputDirectory, "$name-diff.png")
        FileOutputStream(diffFile).use {
            comparison.diff.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        fail(
            "Golden $name changed by ${"%.3f".format(comparison.changedPixelRatio * 100)}%; " +
                "actual=$actualFile diff=$diffFile",
        )
    }
}
