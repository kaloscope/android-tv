package org.kaloscope.tv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kaloscope.tv.BuildConfig
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.AppUpdateRelease
import org.kaloscope.tv.data.update.AppUpdateRepository

enum class AppUpdatePhase { Idle, Checking, UpToDate, Available, Downloading, Ready }

data class AppUpdateUiState(
    val phase: AppUpdatePhase = AppUpdatePhase.Idle,
    val release: AppUpdateRelease? = null,
    val progress: Int = 0,
    val error: AppError? = null,
    val confirmationOpen: Boolean = false,
    val downloadedApk: File? = null,
    val installRequestId: Long? = null,
)

data class AppUpdateActions(
    val check: () -> Unit = {},
    val promptDownload: () -> Unit = {},
    val dismissConfirmation: () -> Unit = {},
    val confirmDownload: () -> Unit = {},
    val cancel: () -> Unit = {},
    val install: () -> Unit = {},
    val consumeInstall: (Long) -> Unit = {},
    val installError: (AppError) -> Unit = {},
    val leave: () -> Unit = {},
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AppUpdateUiState())
    val uiState = mutableState.asStateFlow()
    private var operation: Job? = null
    private var generation = 0L
    private var nextInstallRequest = 0L

    fun check() {
        if (operation?.isActive == true) return
        val request = ++generation
        mutableState.value = AppUpdateUiState(phase = AppUpdatePhase.Checking)
        operation = viewModelScope.launch {
            val result = repository.check(BuildConfig.VERSION_NAME)
            if (request != generation) return@launch
            mutableState.value = when (result) {
                is AppResult.Success -> AppUpdateUiState(
                    phase = if (result.value == null) AppUpdatePhase.UpToDate else AppUpdatePhase.Available,
                    release = result.value,
                    confirmationOpen = result.value != null,
                )
                is AppResult.Failure -> AppUpdateUiState(error = result.error)
            }
        }
    }

    fun promptDownload() {
        if (mutableState.value.phase == AppUpdatePhase.Available) {
            mutableState.value = mutableState.value.copy(confirmationOpen = true, error = null)
        }
    }

    fun dismissConfirmation() {
        mutableState.value = mutableState.value.copy(confirmationOpen = false)
    }

    fun download() {
        val state = mutableState.value
        val release = state.release ?: return
        if (!state.confirmationOpen || state.phase != AppUpdatePhase.Available) return
        val request = ++generation
        mutableState.value = state.copy(
            phase = AppUpdatePhase.Downloading,
            confirmationOpen = false,
            error = null,
            progress = 0,
        )
        operation = viewModelScope.launch {
            val result = repository.download(release) { progress ->
                viewModelScope.launch {
                    if (request == generation && mutableState.value.phase == AppUpdatePhase.Downloading) {
                        mutableState.value = mutableState.value.copy(progress = progress.coerceIn(0, 100))
                    }
                }
            }
            if (request != generation) return@launch
            mutableState.value = when (result) {
                is AppResult.Success -> mutableState.value.copy(
                    phase = AppUpdatePhase.Ready,
                    progress = 100,
                    downloadedApk = result.value,
                    installRequestId = ++nextInstallRequest,
                )
                is AppResult.Failure -> mutableState.value.copy(
                    phase = AppUpdatePhase.Available,
                    error = result.error,
                )
            }
        }
    }

    fun cancel() {
        ++generation
        operation?.cancel()
        operation = null
        val state = mutableState.value
        mutableState.value = state.copy(
            phase = when (state.phase) {
                AppUpdatePhase.Checking -> AppUpdatePhase.Idle
                AppUpdatePhase.Downloading -> AppUpdatePhase.Available
                else -> state.phase
            },
            confirmationOpen = false,
            installRequestId = null,
        )
    }

    fun install() {
        if (mutableState.value.phase == AppUpdatePhase.Ready) {
            mutableState.value = mutableState.value.copy(
                installRequestId = ++nextInstallRequest,
                error = null,
            )
        }
    }

    fun consumeInstall(requestId: Long) {
        if (mutableState.value.installRequestId == requestId) {
            mutableState.value = mutableState.value.copy(installRequestId = null)
        }
    }

    fun installError(error: AppError) {
        mutableState.value = mutableState.value.copy(error = error, installRequestId = null)
    }
}
