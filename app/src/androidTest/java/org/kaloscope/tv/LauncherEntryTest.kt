package org.kaloscope.tv

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherEntryTest {
    @Test
    fun genericLauncherCanResolveMainActivity() {
        assertMainActivityResolvesFor(Intent.CATEGORY_LAUNCHER)
    }

    @Test
    fun tvLauncherCanResolveMainActivity() {
        assertMainActivityResolvesFor(Intent.CATEGORY_LEANBACK_LAUNCHER)
    }

    private fun assertMainActivityResolvesFor(category: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launcherIntent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(category)
                .setPackage(context.packageName)
        val resolvedActivities =
            context.packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.MATCH_ALL,
            )

        assertTrue(
            "$category does not resolve MainActivity",
            resolvedActivities.any { it.activityInfo.name == MainActivity::class.java.name },
        )
    }
}
