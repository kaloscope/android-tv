package org.kaloscope.tv.feature.settings

import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kaloscope.tv.BuildConfig
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.common.UpdateFailure
import org.kaloscope.tv.core.model.AppUpdateRelease
import org.kaloscope.tv.data.update.AppUpdateDownloadExporter
import org.kaloscope.tv.data.update.AppUpdateRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `checking is explicit single flight and download requires confirmation`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository()
        val checked = CompletableDeferred<AppResult<AppUpdateRelease?>>()
        repository.checkBlock = { checked.await() }
        val viewModel = AppUpdateViewModel(repository, FakeDownloadExporter())
        assertEquals(0, repository.checks)
        viewModel.check()
        viewModel.check()
        runCurrent()
        assertEquals(AppUpdatePhase.Checking, viewModel.uiState.value.phase)
        assertEquals(1, repository.checks)
        assertEquals(BuildConfig.VERSION_NAME, repository.checkedVersion)
        checked.complete(AppResult.Success(release))
        runCurrent()
        assertTrue(viewModel.uiState.value.confirmationOpen)
        assertEquals(0, repository.downloads)
        viewModel.dismissConfirmation()
        viewModel.download()
        runCurrent()
        assertEquals(0, repository.downloads)
        viewModel.promptDownload()
        viewModel.download()
        viewModel.download()
        advanceUntilIdle()
        assertEquals(1, repository.downloads)
        assertEquals(AppUpdatePhase.Ready, viewModel.uiState.value.phase)
    }

    @Test
    fun `equal or newer installed version displays up to date`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository().apply { checkBlock = { AppResult.Success(null) } }
        val viewModel = AppUpdateViewModel(repository, FakeDownloadExporter())
        viewModel.check()
        advanceUntilIdle()
        assertEquals(AppUpdatePhase.UpToDate, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.confirmationOpen)
        assertNull(viewModel.uiState.value.installRequestId)
    }

    @Test
    fun `check failures are visible and retry can succeed`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository().apply { checkBlock = { AppResult.Failure(AppError.Timeout) } }
        val viewModel = AppUpdateViewModel(repository, FakeDownloadExporter())
        viewModel.check()
        advanceUntilIdle()
        assertEquals(AppError.Timeout, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.confirmationOpen)
        repository.checkBlock = { AppResult.Success(release) }
        viewModel.check()
        advanceUntilIdle()
        assertEquals(AppUpdatePhase.Available, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `failed download preserves release and retries only after new confirmation`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository().apply { downloadBlock = { AppResult.Failure(AppError.Offline) } }
        val viewModel = AppUpdateViewModel(repository, FakeDownloadExporter())
        viewModel.check()
        runCurrent()
        viewModel.download()
        advanceUntilIdle()
        assertEquals(AppUpdatePhase.Available, viewModel.uiState.value.phase)
        assertEquals(release, viewModel.uiState.value.release)
        assertEquals(AppError.Offline, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.installRequestId)
        viewModel.download()
        runCurrent()
        assertEquals(1, repository.downloads)
        repository.downloadBlock = { AppResult.Success(File("verified.apk")) }
        viewModel.promptDownload()
        viewModel.download()
        advanceUntilIdle()
        assertEquals(AppUpdatePhase.Ready, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `download completion requests installation once and manual retry creates a new request`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository()
        val viewModel = AppUpdateViewModel(repository, FakeDownloadExporter())
        viewModel.check()
        runCurrent()
        viewModel.download()
        advanceUntilIdle()
        val first = requireNotNull(viewModel.uiState.value.installRequestId)
        assertEquals(100, viewModel.uiState.value.progress)
        assertNotNull(viewModel.uiState.value.downloadedApk)
        viewModel.consumeInstall(first + 1)
        assertEquals(first, viewModel.uiState.value.installRequestId)
        viewModel.consumeInstall(first)
        assertNull(viewModel.uiState.value.installRequestId)
        viewModel.installError(AppError.Offline)
        viewModel.install()
        assertTrue(requireNotNull(viewModel.uiState.value.installRequestId) > first)
        assertNull(viewModel.uiState.value.error)
        assertEquals(1, repository.downloads)
    }

    @Test
    fun `cancelled checks cannot reopen a dialog after leaving the page`() = runTest(dispatcher) {
        val pending = CompletableDeferred<AppResult<AppUpdateRelease?>>()
        val repository = FakeUpdateRepository().apply {
            checkBlock = { withContext(NonCancellable) { pending.await() } }
        }
        val viewModel = AppUpdateViewModel(repository, FakeDownloadExporter())
        viewModel.check()
        runCurrent()
        viewModel.cancel()
        repository.checkBlock = { AppResult.Success(null) }
        viewModel.check()
        runCurrent()
        pending.complete(AppResult.Success(release))
        advanceUntilIdle()
        assertEquals(AppUpdatePhase.UpToDate, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.confirmationOpen)
    }

    @Test
    fun `cancelled downloads discard late progress and cannot launch installation`() = runTest(dispatcher) {
        val pending = CompletableDeferred<AppResult<File>>()
        val repository = FakeUpdateRepository().apply {
            downloadBlock = { withContext(NonCancellable) { pending.await() } }
        }
        val viewModel = AppUpdateViewModel(repository, FakeDownloadExporter())
        viewModel.check()
        runCurrent()
        viewModel.download()
        runCurrent()
        repository.progress?.invoke(35)
        runCurrent()
        assertEquals(35, viewModel.uiState.value.progress)
        viewModel.cancel()
        repository.progress?.invoke(100)
        pending.complete(AppResult.Success(File("stale.apk")))
        advanceUntilIdle()
        assertEquals(AppUpdatePhase.Available, viewModel.uiState.value.phase)
        assertEquals(35, viewModel.uiState.value.progress)
        assertNull(viewModel.uiState.value.downloadedApk)
        assertNull(viewModel.uiState.value.installRequestId)
    }

    @Test
    fun `manual export reuses verified download and presents its saved name without another install request`() =
        runTest(dispatcher) {
            val repository = FakeUpdateRepository()
            val exporter = FakeDownloadExporter()
            val viewModel = AppUpdateViewModel(repository, exporter)
            viewModel.check()
            runCurrent()
            viewModel.download()
            advanceUntilIdle()
            viewModel.installError(AppError.Update(UpdateFailure.InstallPermission))

            viewModel.saveToDownloads()
            advanceUntilIdle()

            assertEquals(File("verified.apk"), exporter.savedApk)
            assertEquals(release.version, exporter.savedVersion)
            assertEquals("kaloscope-tv-0.3.23.apk", viewModel.uiState.value.savedDownloadName)
            assertTrue(viewModel.uiState.value.manualInstallNoticeOpen)
            assertFalse(viewModel.uiState.value.savingToDownloads)
            assertNull(viewModel.uiState.value.installRequestId)
            assertNull(viewModel.uiState.value.error)
            assertEquals(1, repository.downloads)
            viewModel.dismissManualInstallNotice()
            assertFalse(viewModel.uiState.value.manualInstallNoticeOpen)
            viewModel.saveToDownloads()
            runCurrent()
            assertTrue(viewModel.uiState.value.manualInstallNoticeOpen)
            assertEquals(1, exporter.saves)
        }

    @Test
    fun `manual export is unavailable until download has completed`() = runTest(dispatcher) {
        val exporter = FakeDownloadExporter()
        val viewModel = AppUpdateViewModel(FakeUpdateRepository(), exporter)
        viewModel.saveToDownloads()
        viewModel.check()
        advanceUntilIdle()
        viewModel.saveToDownloads()
        runCurrent()
        assertEquals(0, exporter.saves)
    }

    @Test
    fun `pending export prevents duplicate copies checks and installation`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository()
        val pending = CompletableDeferred<AppResult<String>>()
        val exporter = FakeDownloadExporter().apply { saveBlock = { pending.await() } }
        val viewModel = AppUpdateViewModel(repository, exporter)
        viewModel.check()
        runCurrent()
        viewModel.download()
        advanceUntilIdle()

        viewModel.saveToDownloads()
        runCurrent()
        viewModel.saveToDownloads()
        viewModel.check()
        viewModel.install()
        runCurrent()

        assertTrue(viewModel.uiState.value.savingToDownloads)
        assertEquals(1, exporter.saves)
        assertEquals(1, repository.checks)
        assertNull(viewModel.uiState.value.installRequestId)
        pending.complete(AppResult.Success("kaloscope-tv-0.3.23.apk"))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.savingToDownloads)
    }

    @Test
    fun `export failure preserves the private APK and can retry without downloading`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository()
        val exporter = FakeDownloadExporter().apply {
            saveBlock = { AppResult.Failure(AppError.Update(UpdateFailure.DownloadExport)) }
        }
        val viewModel = AppUpdateViewModel(repository, exporter)
        viewModel.check()
        runCurrent()
        viewModel.download()
        advanceUntilIdle()
        viewModel.saveToDownloads()
        advanceUntilIdle()

        assertEquals(AppError.Update(UpdateFailure.DownloadExport), viewModel.uiState.value.error)
        assertEquals(File("verified.apk"), viewModel.uiState.value.downloadedApk)
        assertEquals(AppUpdatePhase.Ready, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.manualInstallNoticeOpen)
        assertFalse(viewModel.uiState.value.savingToDownloads)

        exporter.saveBlock = { AppResult.Success("kaloscope-tv-0.3.23.apk") }
        viewModel.saveToDownloads()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.manualInstallNoticeOpen)
        assertEquals(2, exporter.saves)
        assertEquals(1, repository.downloads)
    }

    @Test
    fun `leaving during export discards a late result and cannot open its notice`() = runTest(dispatcher) {
        val pending = CompletableDeferred<AppResult<String>>()
        val exporter = FakeDownloadExporter().apply {
            saveBlock = { withContext(NonCancellable) { pending.await() } }
        }
        val viewModel = AppUpdateViewModel(FakeUpdateRepository(), exporter)
        viewModel.check()
        runCurrent()
        viewModel.download()
        advanceUntilIdle()
        viewModel.saveToDownloads()
        runCurrent()
        viewModel.cancel()
        pending.complete(AppResult.Success("kaloscope-tv-0.3.23.apk"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.savingToDownloads)
        assertFalse(viewModel.uiState.value.manualInstallNoticeOpen)
        assertNull(viewModel.uiState.value.savedDownloadName)
        assertEquals(File("verified.apk"), viewModel.uiState.value.downloadedApk)
    }
}

private val release = AppUpdateRelease("0.3.23", "https://example.com/update.apk", 3, "a".repeat(64), null)

private class FakeUpdateRepository : AppUpdateRepository {
    var checks = 0
    var downloads = 0
    var checkedVersion: String? = null
    var progress: ((Int) -> Unit)? = null
    var checkBlock: suspend () -> AppResult<AppUpdateRelease?> = { AppResult.Success(release) }
    var downloadBlock: suspend () -> AppResult<File> = { AppResult.Success(File("verified.apk")) }

    override suspend fun check(currentVersion: String): AppResult<AppUpdateRelease?> {
        checks++
        checkedVersion = currentVersion
        return checkBlock()
    }

    override suspend fun download(release: AppUpdateRelease, onProgress: (Int) -> Unit): AppResult<File> {
        downloads++
        progress = onProgress
        return downloadBlock()
    }
}

private class FakeDownloadExporter : AppUpdateDownloadExporter {
    var saves = 0
    var savedApk: File? = null
    var savedVersion: String? = null
    var saveBlock: suspend () -> AppResult<String> = { AppResult.Success("kaloscope-tv-0.3.23.apk") }

    override suspend fun save(apk: File, version: String): AppResult<String> {
        saves++
        savedApk = apk
        savedVersion = version
        return saveBlock()
    }
}
