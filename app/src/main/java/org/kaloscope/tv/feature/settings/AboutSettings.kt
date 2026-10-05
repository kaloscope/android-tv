package org.kaloscope.tv.feature.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kaloscope.tv.BuildConfig
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.UpdateFailure
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeConfirmDialog
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Success
import org.kaloscope.tv.core.designsystem.appErrorText

@Composable
internal fun AboutSettings(
    state: AppUpdateUiState,
    actions: AppUpdateActions,
    interactionsEnabled: Boolean,
) {
    val checkFocus = remember { FocusRequester() }
    val downloadFocus = remember { FocusRequester() }
    val manualInstallFocus = remember { FocusRequester() }
    val saveToDownloads = rememberUpdateDownloadSaveAction(state, actions)
    var restoreFocus by remember { mutableStateOf<FocusRequester?>(null) }
    var projectLink by remember { mutableStateOf<AboutProjectLink?>(null) }
    val busy = state.phase == AppUpdatePhase.Checking || state.phase == AppUpdatePhase.Downloading ||
        state.savingToDownloads
    val latestActions by rememberUpdatedState(actions)
    val errorDescription = state.error?.let {
        when (it) {
            AppError.Timeout -> stringResource(R.string.update_error_timeout)
            AppError.Offline -> stringResource(R.string.update_error_network)
            else -> appErrorText(it)
        }
    }
    val checkError = errorDescription?.takeIf { state.release == null }
    val exportError = (state.error as? AppError.Update)?.reason == UpdateFailure.DownloadExport

    DisposableEffect(Unit) {
        onDispose { latestActions.leave() }
    }
    LaunchedEffect(state.confirmationOpen, state.manualInstallNoticeOpen, projectLink) {
        if (!state.confirmationOpen && !state.manualInstallNoticeOpen && projectLink == null) {
            restoreFocus?.let {
                withFrameNanos { }
                it.requestFocus()
                restoreFocus = null
            }
        }
    }
    LaunchedEffect(exportError) {
        if (exportError && state.phase == AppUpdatePhase.Ready) {
            withFrameNanos { }
            manualInstallFocus.requestFocus()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("settings-about"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = OnBackground,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.about_current_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.testTag("about-current-version"),
            )
        }
        Text(
            stringResource(R.string.about_summary),
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        SettingActionRow(
            title = stringResource(R.string.update_check),
            description = when {
                checkError != null -> checkError
                state.phase == AppUpdatePhase.Checking -> stringResource(R.string.update_checking)
                state.phase == AppUpdatePhase.UpToDate -> stringResource(R.string.update_up_to_date)
                state.release != null -> stringResource(R.string.update_available_version, state.release.version)
                else -> stringResource(R.string.update_check_description)
            },
            descriptionColor = when {
                checkError != null -> Danger
                state.phase == AppUpdatePhase.UpToDate || state.release != null -> Success
                else -> null
            },
            interactionsEnabled = interactionsEnabled && !busy && !state.confirmationOpen,
            modifier = Modifier.focusRequester(checkFocus).testTag("update-check"),
            loadingIndicatorTestTag = if (state.phase == AppUpdatePhase.Checking) "update-checking" else null,
            onClick = {
                restoreFocus = checkFocus
                actions.check()
            },
        )
        if (state.release != null) {
            val downloading = state.phase == AppUpdatePhase.Downloading
            val ready = state.phase == AppUpdatePhase.Ready
            SettingActionRow(
                title = stringResource(when {
                    downloading -> R.string.update_cancel_download
                    ready -> R.string.update_install
                    else -> R.string.update_download_install
                }),
                description = when {
                    errorDescription != null && !exportError -> errorDescription
                    downloading -> stringResource(R.string.update_downloading, state.progress)
                    ready -> stringResource(R.string.update_ready)
                    else -> stringResource(R.string.update_download_description)
                },
                descriptionColor = when {
                    errorDescription != null && !exportError -> Danger
                    ready -> Success
                    else -> null
                },
                interactionsEnabled = interactionsEnabled && !state.confirmationOpen && !state.savingToDownloads,
                modifier = Modifier.focusRequester(downloadFocus).testTag("update-download"),
                loadingIndicatorTestTag = if (downloading) "update-downloading" else null,
                onClick = {
                    when {
                        downloading -> actions.cancel()
                        ready -> actions.install()
                        else -> {
                            restoreFocus = downloadFocus
                            actions.promptDownload()
                        }
                    }
                },
            )
            if (ready) {
                SettingActionRow(
                    title = stringResource(
                        if (state.savedDownloadName == null) R.string.update_save_downloads
                        else R.string.update_manual_install_title,
                    ),
                    description = when {
                        exportError -> errorDescription.orEmpty()
                        state.savingToDownloads -> stringResource(R.string.update_saving_downloads)
                        state.savedDownloadName != null -> stringResource(
                            R.string.update_manual_install_saved,
                            state.savedDownloadName,
                        )
                        else -> stringResource(R.string.update_save_downloads_description)
                    },
                    descriptionColor = when {
                        exportError -> Danger
                        state.savedDownloadName != null -> Success
                        else -> null
                    },
                    interactionsEnabled = interactionsEnabled && !busy && !state.manualInstallNoticeOpen,
                    modifier = Modifier.focusRequester(manualInstallFocus).testTag("update-save-downloads"),
                    loadingIndicatorTestTag = if (state.savingToDownloads) "update-saving-downloads" else null,
                    onClick = saveToDownloads,
                )
            }
        }
        AboutProjectLinks(
            interactionsEnabled = interactionsEnabled && !busy && !state.confirmationOpen && projectLink == null,
            onOpen = { link, focus ->
                restoreFocus = focus
                projectLink = link
            },
        )
        Text(
            stringResource(R.string.about_copyright),
            color = Muted,
            fontSize = 12.sp,
        )
    }
    if (state.confirmationOpen) {
        state.release?.let { release ->
            KaloscopeConfirmDialog(
                title = stringResource(R.string.update_found),
                message = stringResource(R.string.update_confirmation, BuildConfig.VERSION_NAME, release.version),
                cancelLabel = stringResource(R.string.cancel),
                confirmLabel = stringResource(R.string.update_download_install),
                onDismiss = actions.dismissConfirmation,
                onConfirm = actions.confirmDownload,
            )
        }
    }
    projectLink?.let { link ->
        AboutProjectLinkPanel(link = link, onDismiss = { projectLink = null })
    }
    if (state.manualInstallNoticeOpen) {
        state.savedDownloadName?.let { name ->
            val dismiss = {
                restoreFocus = manualInstallFocus
                actions.dismissManualInstallNotice()
            }
            KaloscopeConfirmDialog(
                title = stringResource(R.string.update_manual_install_title),
                message = stringResource(R.string.update_manual_install_description, name),
                cancelLabel = stringResource(R.string.close),
                confirmLabel = stringResource(R.string.update_manual_install_done),
                showCancel = false,
                onDismiss = dismiss,
                onConfirm = dismiss,
            )
        }
    }
    AppUpdateInstallation(state, actions, downloadFocus, saveToDownloads)
}

@Composable
private fun rememberUpdateDownloadSaveAction(state: AppUpdateUiState, actions: AppUpdateActions): () -> Unit {
    val context = LocalContext.current
    val latestState by rememberUpdatedState(state)
    val latestActions by rememberUpdatedState(actions)
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) latestActions.saveToDownloads()
        else latestActions.installError(AppError.Update(UpdateFailure.DownloadExport))
    }
    return {
        if (latestState.savedDownloadName != null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            latestActions.saveToDownloads()
        } else {
            try {
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } catch (_: ActivityNotFoundException) {
                latestActions.installError(AppError.Update(UpdateFailure.DownloadExport))
            } catch (_: SecurityException) {
                latestActions.installError(AppError.Update(UpdateFailure.DownloadExport))
            }
        }
    }
}

@Composable
private fun AppUpdateInstallation(
    state: AppUpdateUiState,
    actions: AppUpdateActions,
    installFocus: FocusRequester,
    saveToDownloads: () -> Unit,
) {
    val context = LocalContext.current
    val latestState by rememberUpdatedState(state)
    val latestActions by rememberUpdatedState(actions)
    var permissionPrompt by remember { mutableStateOf(false) }
    var restoreFocus by remember { mutableStateOf(false) }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        restoreFocus = true
        // A cancelled system dialog leaves the verified APK available for another attempt.
        if (result.resultCode != Activity.RESULT_OK && result.resultCode != Activity.RESULT_CANCELED) {
            latestActions.installError(AppError.Update(UpdateFailure.InstallFailed))
        }
    }

    fun launchInstaller() {
        val apk = latestState.downloadedApk
        if (apk == null || !apk.isFile) {
            latestActions.installError(AppError.Update(UpdateFailure.FileMissing))
            return
        }
        try {
            installer.launch(AppUpdateInstaller.intent(context, apk))
        } catch (_: ActivityNotFoundException) {
            latestActions.installError(AppError.Update(UpdateFailure.InstallerUnavailable))
            saveToDownloads()
        } catch (_: SecurityException) {
            latestActions.installError(AppError.Update(UpdateFailure.InstallPermission))
            saveToDownloads()
        } catch (_: IllegalArgumentException) {
            latestActions.installError(AppError.Update(UpdateFailure.FileMissing))
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        restoreFocus = true
        if (AppUpdateInstaller.hasInstallPermission(context)) {
            launchInstaller()
        } else {
            latestActions.installError(AppError.Update(UpdateFailure.InstallPermission))
        }
    }
    LaunchedEffect(state.installRequestId) {
        val requestId = state.installRequestId ?: return@LaunchedEffect
        val apk = state.downloadedApk ?: return@LaunchedEffect
        val release = state.release ?: return@LaunchedEffect
        val error = withContext(Dispatchers.IO) {
            AppUpdateInstaller.validate(context, apk, release.version)
        }
        actions.consumeInstall(requestId)
        when {
            error != null -> actions.installError(AppError.Update(error))
            AppUpdateInstaller.hasInstallPermission(context) -> launchInstaller()
            else -> {
                permissionPrompt = true
            }
        }
    }
    LaunchedEffect(restoreFocus, permissionPrompt, state.savingToDownloads, state.manualInstallNoticeOpen, state.error) {
        if (state.savingToDownloads || state.manualInstallNoticeOpen ||
            (state.error as? AppError.Update)?.reason == UpdateFailure.DownloadExport
        ) {
            // The manual-install notice or export error owns focus restoration after fallback.
            restoreFocus = false
        } else if (restoreFocus && !permissionPrompt && state.release != null) {
            withFrameNanos { }
            installFocus.requestFocus()
            restoreFocus = false
        }
    }
    if (permissionPrompt) {
        KaloscopeConfirmDialog(
            title = stringResource(R.string.update_permission_title),
            message = stringResource(R.string.update_permission_description),
            cancelLabel = stringResource(R.string.cancel),
            confirmLabel = stringResource(R.string.update_open_permission),
            onDismiss = {
                permissionPrompt = false
                restoreFocus = true
                actions.installError(AppError.Update(UpdateFailure.InstallPermission))
            },
            onConfirm = {
                if (AppUpdateInstaller.hasInstallPermission(context)) {
                    permissionPrompt = false
                    restoreFocus = true
                    launchInstaller()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val opened = AppUpdateInstaller.openPermissionSettings(context.packageName) { action, data ->
                        try {
                            permission.launch(Intent(action).apply { this.data = data?.toUri() })
                            true
                        } catch (_: ActivityNotFoundException) {
                            false
                        } catch (_: SecurityException) {
                            false
                        }
                    }
                    if (opened) {
                        permissionPrompt = false
                        restoreFocus = true
                    } else {
                        permissionPrompt = false
                        restoreFocus = true
                        latestActions.installError(AppError.Update(UpdateFailure.InstallPermission))
                        saveToDownloads()
                    }
                }
            },
        )
    }
}
