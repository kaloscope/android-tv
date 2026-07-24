package org.kaloscope.tv.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeResolution

class PreferencesSettingsRepositoryTest {
    @Test
    fun `missing preferences return P0 defaults`() = runTest {
        val repository = repository(this)

        val result = repository.getSettings()

        assertEquals(TvSettings(), (result as AppResult.Success).value)
    }

    @Test
    fun `saved settings survive repository recreation`() = runTest {
        val dataStore = dataStore(this)
        val first = PreferencesSettingsRepository(dataStore)
        val expected = TvSettings(
            startPage = StartPage.Library,
            playbackMode = PlaybackMode.Transcode,
            transcodeResolution = TranscodeResolution.P720,
            autoplayNext = false,
            danmakuEnabled = false,
            subtitleEnabled = false,
        )

        assertEquals(AppResult.Success(expected), first.saveSettings(expected))
        val restored = PreferencesSettingsRepository(dataStore).getSettings()

        assertEquals(expected, (restored as AppResult.Success).value)
    }

    private fun repository(
        scope: TestScope,
    ) = PreferencesSettingsRepository(dataStore(scope))

    private fun dataStore(
        scope: TestScope,
        file: File = temporaryFile(),
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope.backgroundScope,
            produceFile = { file },
        )

    private fun temporaryFile(): File =
        File.createTempFile("kaloscope-settings-", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
}
