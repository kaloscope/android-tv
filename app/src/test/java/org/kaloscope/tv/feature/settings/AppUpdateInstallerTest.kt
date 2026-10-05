package org.kaloscope.tv.feature.settings

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateInstallerTest {
    @Test
    fun `supported app permission page opens once with this package`() {
        val attempts = mutableListOf<Pair<String, String?>>()

        val opened = AppUpdateInstaller.openPermissionSettings("org.kaloscope.tv") { action, data ->
            attempts += action to data
            true
        }

        assertTrue(opened)
        assertEquals(
            listOf(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES to "package:org.kaloscope.tv"),
            attempts,
        )
    }

    @Test
    fun `TV without app permission page opens sources list without package URI`() {
        val attempts = mutableListOf<Pair<String, String?>>()

        val opened = AppUpdateInstaller.openPermissionSettings("org.kaloscope.tv") { action, data ->
            attempts += action to data
            action == Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES && data == null
        }

        assertTrue(opened)
        assertEquals(
            listOf(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES to "package:org.kaloscope.tv",
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES to null,
            ),
            attempts,
        )
    }

    @Test
    fun `TV without unknown app sources pages falls back to security settings`() {
        val attempts = mutableListOf<Pair<String, String?>>()

        val opened = AppUpdateInstaller.openPermissionSettings("org.kaloscope.tv") { action, data ->
            attempts += action to data
            action == Settings.ACTION_SECURITY_SETTINGS
        }

        assertTrue(opened)
        assertEquals(3, attempts.size)
        assertEquals(Settings.ACTION_SECURITY_SETTINGS to null, attempts.last())
    }

    @Test
    fun `TV with only general settings opens it as the last fallback`() {
        val attempts = mutableListOf<Pair<String, String?>>()

        val opened = AppUpdateInstaller.openPermissionSettings("org.kaloscope.tv") { action, data ->
            attempts += action to data
            action == Settings.ACTION_SETTINGS
        }

        assertTrue(opened)
        assertEquals(4, attempts.size)
        assertEquals(Settings.ACTION_SETTINGS to null, attempts.last())
    }

    @Test
    fun `unavailable or blocked settings report failure after trying all fallbacks`() {
        var attempts = 0

        val opened = AppUpdateInstaller.openPermissionSettings("org.kaloscope.tv") { _, _ ->
            attempts++
            false
        }

        assertFalse(opened)
        assertEquals(4, attempts)
    }
}
