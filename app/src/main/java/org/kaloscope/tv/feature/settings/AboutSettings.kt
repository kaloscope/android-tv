package org.kaloscope.tv.feature.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
    var restoreFocus by remember { mutableStateOf<FocusRequester?>(null) }
    var projectLink by remember { mutableStateOf<AboutProjectLink?>(null) }
    val busy = state.phase == AppUpdatePhase.Checking || state.phase == AppUpdatePhase.Downloading
    val latestActions by rememberUpdatedState(actions)
    val errorDescription = state.error?.let {
        when (it) {
            AppError.Timeout -> stringResource(R.string.update_error_timeout)
            AppError.Offline -> stringResource(R.string.update_error_network)
            else -> appErrorText(it)
        }
    }
    val checkError = errorDescription?.takeIf { state.release == null }

    DisposableEffect(Unit) {
        onDispose { latestActions.leave() }
    }
    LaunchedEffect(state.confirmationOpen, projectLink) {
        if (!state.confirmationOpen && projectLink == null) {
            restoreFocus?.let {
                withFrameNanos { }
                it.requestFocus()
                restoreFocus = null
            }
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
                    errorDescription != null -> errorDescription
                    downloading -> stringResource(R.string.update_downloading, state.progress)
                    ready -> stringResource(R.string.update_ready)
                    else -> stringResource(R.string.update_download_description)
                },
                descriptionColor = when {
                    errorDescription != null -> Danger
                    ready -> Success
                    else -> null
                },
                interactionsEnabled = interactionsEnabled && !state.confirmationOpen,
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
    AppUpdateInstallation(state, actions, downloadFocus)
}

@Composable
private fun AppUpdateInstallation(
    state: AppUpdateUiState,
    actions: AppUpdateActions,
    installFocus: FocusRequester,
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
        } catch (_: SecurityException) {
            latestActions.installError(AppError.Update(UpdateFailure.InstallPermission))
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
            else -> permissionPrompt = true
        }
    }
    LaunchedEffect(restoreFocus, permissionPrompt) {
        if (restoreFocus && !permissionPrompt && state.release != null) {
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
                permissionPrompt = false
                restoreFocus = true
                try {
                    // Per-app install permission settings are only available from Android O.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        permission.launch(Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            "package:${context.packageName}".toUri(),
                        ))
                    }
                } catch (_: ActivityNotFoundException) {
                    actions.installError(AppError.Update(UpdateFailure.InstallPermission))
                } catch (_: SecurityException) {
                    actions.installError(AppError.Update(UpdateFailure.InstallPermission))
                }
            },
        )
    }
}
