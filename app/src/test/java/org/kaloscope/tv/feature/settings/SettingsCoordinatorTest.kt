package org.kaloscope.tv.feature.settings

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.data.server.ServerRepository
import org.kaloscope.tv.data.settings.SettingsRepository

class SettingsCoordinatorTest {
    @Test
    fun `load exposes persisted settings`() = runTest {
        val expected = TvSettings(startPage = StartPage.Search)
        val coordinator = coordinator(settings = expected)

        coordinator.load()

        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(expected, state.settings)
        assertEquals(SettingsSection.Playback, state.section)
    }

    @Test
    fun `failed save keeps prior value and exposes recoverable error`() = runTest {
        val repository = FakeSettingsRepository(
            settings = TvSettings(),
            saveResult = AppResult.Failure(AppError.InvalidData("settings_write")),
        )
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()

        coordinator.setPlaybackMode(PlaybackMode.Direct)

        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(PlaybackMode.Auto, state.settings.playbackMode)
        assertEquals(AppError.InvalidData("settings_write"), state.saveError)
        assertFalse(state.isSaving)
    }

    @Test
    fun `danmaku settings persist as one value`() = runTest {
        val repository = FakeSettingsRepository(TvSettings())
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()
        val expected = DanmakuSettings(
            enabled = false,
            speed = DanmakuSpeed.Fast,
            opacityPercent = 50,
        )

        coordinator.setDanmakuSettings(expected)

        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(expected, state.settings.danmaku)
        assertEquals(expected, repository.saved?.danmaku)
    }

    @Test
    fun `subtitle defaults persist as one value`() = runTest {
        val repository = FakeSettingsRepository(TvSettings())
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()
        val expected = SubtitleSettings(
            enabled = false,
            languagePreference = "chs|zh-CN",
            displayMode = SubtitleDisplayMode.Background,
            timeOffsetSeconds = 0.5f,
            fontScalePercent = 120,
            verticalPositionPercent = 5,
        )

        coordinator.setSubtitleSettings(expected)

        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(expected, state.settings.subtitle)
        assertEquals(expected, repository.saved?.subtitle)
    }

    @Test
    fun `connection test reports success without changing settings`() = runTest {
        val serverRepository = FakeServerRepository(
            connectionResult = AppResult.Success("0.1.0"),
        )
        val coordinator = SettingsCoordinator(
            FakeSettingsRepository(TvSettings()),
            serverRepository,
        )
        coordinator.load()

        coordinator.testConnection(session())

        val state = coordinator.state.value as SettingsUiState.Content
        assertTrue(state.connection is SettingsConnection.Success)
        assertEquals("http://127.0.0.1:8000", serverRepository.testedOrigin)
    }
}

private class FakeSettingsRepository(
    private val settings: TvSettings,
    private val saveResult: AppResult<TvSettings>? = null,
) : SettingsRepository {
    var saved: TvSettings? = null

    override suspend fun getSettings(): AppResult<TvSettings> = AppResult.Success(settings)

    override suspend fun saveSettings(settings: TvSettings): AppResult<TvSettings> {
        saved = settings
        return saveResult ?: AppResult.Success(settings)
    }
}

private class FakeServerRepository(
    private val connectionResult: AppResult<String> = AppResult.Success(""),
) : ServerRepository {
    var testedOrigin: String? = null

    override suspend fun testConnection(origin: String): AppResult<String> {
        testedOrigin = origin
        return connectionResult
    }

    override suspend fun saveServer(server: SavedServer) = error("Not used")

    override suspend fun setActiveServer(serverId: String) = error("Not used")
}

private fun coordinator(settings: TvSettings) = SettingsCoordinator(
    FakeSettingsRepository(settings),
    FakeServerRepository(),
)

private fun session() = Session(
    server = SavedServer("server-1", "Home", "http://127.0.0.1:8000"),
    token = "token",
    user = SessionUser(1, "tv", "user"),
)
