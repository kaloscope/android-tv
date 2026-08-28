package org.kaloscope.tv.feature.settings

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.SavedServer
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.SessionUser
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeQuality
import org.kaloscope.tv.data.server.ServerConnectionInfo
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
    fun `setting update is visible while its save is pending`() = runTest {
        val saveStarted = CompletableDeferred<TvSettings>()
        val saveResult = CompletableDeferred<AppResult<TvSettings>>()
        val repository = FakeSettingsRepository(
            settings = TvSettings(),
            saveBlock = { settings ->
                saveStarted.complete(settings)
                saveResult.await()
            },
        )
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()

        val saveJob = launch {
            coordinator.setPlaybackMode(PlaybackMode.Direct)
        }
        saveStarted.await()

        val saving = coordinator.state.value as SettingsUiState.Content
        assertEquals(PlaybackMode.Direct, saving.settings.playbackMode)
        assertTrue(saving.isSaving)

        saveResult.complete(
            AppResult.Success(TvSettings(playbackMode = PlaybackMode.Direct)),
        )
        saveJob.join()
        assertFalse((coordinator.state.value as SettingsUiState.Content).isSaving)
    }

    @Test
    fun `transcode quality persists through the settings update path`() = runTest {
        val repository = FakeSettingsRepository(TvSettings())
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()

        coordinator.setTranscodeQuality(TranscodeQuality.High)

        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(TranscodeQuality.High, state.settings.transcodeQuality)
        assertEquals(TranscodeQuality.High, repository.saved?.transcodeQuality)
        assertFalse(state.isSaving)
    }

    @Test
    fun `accent color persists through the settings update path`() = runTest {
        val repository = FakeSettingsRepository(TvSettings())
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()

        coordinator.setAccentColor(AccentColor.Green)

        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(AccentColor.Green, state.settings.accentColor)
        assertEquals(AccentColor.Green, repository.saved?.accentColor)
        assertFalse(state.isSaving)
    }

    @Test
    fun `failed accent save keeps the prior color`() = runTest {
        val error = AppError.InvalidData("settings_write")
        val repository = FakeSettingsRepository(
            settings = TvSettings(accentColor = AccentColor.Purple),
            saveResult = AppResult.Failure(error),
        )
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()

        coordinator.setAccentColor(AccentColor.Orange)

        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(AccentColor.Purple, state.settings.accentColor)
        assertEquals(error, state.saveError)
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
    fun `player subtitle preferences preserve session-only defaults`() = runTest {
        val initial = SubtitleSettings(
            enabled = false,
            languagePreference = "chs|zh-CN",
            displayMode = SubtitleDisplayMode.Stroke,
            timeOffsetSeconds = 0f,
            fontScalePercent = 100,
            verticalPositionPercent = 2,
        )
        val repository = FakeSettingsRepository(TvSettings(subtitle = initial))
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()

        coordinator.setPlayerSubtitlePreferences(
            SubtitleSettings(
                enabled = true,
                languagePreference = "en",
                displayMode = SubtitleDisplayMode.Background,
                timeOffsetSeconds = 1.5f,
                fontScalePercent = 125,
                verticalPositionPercent = 8,
            ),
        )

        val expected = initial.copy(
            displayMode = SubtitleDisplayMode.Background,
            fontScalePercent = 125,
            verticalPositionPercent = 8,
        )
        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(expected, state.settings.subtitle)
        assertEquals(expected, repository.saved?.subtitle)
    }

    @Test
    fun `player subtitle offset does not trigger a global save`() = runTest {
        val initial = SubtitleSettings(timeOffsetSeconds = 0f)
        val repository = FakeSettingsRepository(TvSettings(subtitle = initial))
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()

        coordinator.setPlayerSubtitlePreferences(
            initial.copy(timeOffsetSeconds = 1.5f),
        )

        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(initial, state.settings.subtitle)
        assertNull(repository.saved)
        assertFalse(state.isSaving)
    }

    @Test
    fun `player danmaku preferences preserve the session toggle`() = runTest {
        val initial = DanmakuSettings(enabled = false)
        val repository = FakeSettingsRepository(TvSettings(danmaku = initial))
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()

        coordinator.setPlayerDanmakuPreferences(
            DanmakuSettings(
                enabled = true,
                textSize = DanmakuTextSize.Large,
                speed = DanmakuSpeed.Fast,
                opacityPercent = 50,
                displayAreaPercent = 25,
                visibleModes = setOf(
                    DanmakuDisplayMode.Scroll,
                    DanmakuDisplayMode.Top,
                ),
                blockColored = true,
            ),
        )

        val expected = initial.copy(
            textSize = DanmakuTextSize.Large,
            speed = DanmakuSpeed.Fast,
            opacityPercent = 50,
            displayAreaPercent = 25,
            visibleModes = setOf(
                DanmakuDisplayMode.Scroll,
                DanmakuDisplayMode.Top,
            ),
            blockColored = true,
        )
        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(expected, state.settings.danmaku)
        assertEquals(expected, repository.saved?.danmaku)
    }

    @Test
    fun `reader defaults persist and text values are sanitized`() = runTest {
        val repository = FakeSettingsRepository(TvSettings())
        val coordinator = SettingsCoordinator(repository, FakeServerRepository())
        coordinator.load()

        coordinator.setReaderChapterOrder(ReaderChapterOrder.Descending)
        coordinator.setImageReaderSettings(
            ImageReaderSettings(readMode = ImageReadMode.Paged),
        )
        coordinator.setTextReaderSettings(
            TextReaderSettings(
                fontSizeSp = 31,
                lineHeight = 3.2f,
                paragraphSpacingDp = -1,
                horizontalPaddingDp = 50,
            ),
        )

        val state = coordinator.state.value as SettingsUiState.Content
        assertEquals(ReaderChapterOrder.Descending, state.settings.readerChapterOrder)
        assertEquals(ImageReadMode.Paged, state.settings.imageReader.readMode)
        assertEquals(32, state.settings.textReader.fontSizeSp)
        assertEquals(3f, state.settings.textReader.lineHeight)
        assertEquals(0, state.settings.textReader.paragraphSpacingDp)
        assertEquals(48, state.settings.textReader.horizontalPaddingDp)
        assertEquals(state.settings, repository.saved)
    }

    @Test
    fun `reading section can be selected`() = runTest {
        val coordinator = coordinator(TvSettings())
        coordinator.load()

        coordinator.selectSection(SettingsSection.Reading)

        assertEquals(
            SettingsSection.Reading,
            (coordinator.state.value as SettingsUiState.Content).section,
        )
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
    private val saveBlock: (suspend (TvSettings) -> AppResult<TvSettings>)? = null,
) : SettingsRepository {
    var saved: TvSettings? = null

    override suspend fun getSettings(): AppResult<TvSettings> = AppResult.Success(settings)

    override suspend fun saveSettings(settings: TvSettings): AppResult<TvSettings> {
        saved = settings
        return saveBlock?.invoke(settings) ?: saveResult ?: AppResult.Success(settings)
    }
}

private class FakeServerRepository(
    private val connectionResult: AppResult<String> = AppResult.Success(""),
) : ServerRepository {
    var testedOrigin: String? = null

    override suspend fun testConnection(origin: String): AppResult<ServerConnectionInfo> {
        testedOrigin = origin
        return when (connectionResult) {
            is AppResult.Success -> AppResult.Success(
                ServerConnectionInfo(origin = origin, version = connectionResult.value),
            )

            is AppResult.Failure -> connectionResult
        }
    }

    override suspend fun saveServer(server: SavedServer) = error("Not used")

    override suspend fun deleteServer(serverId: String): List<SavedServer> =
        error("Not used")

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
