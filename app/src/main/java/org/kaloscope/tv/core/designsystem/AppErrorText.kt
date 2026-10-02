package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.UpdateFailure

@Composable
internal fun appErrorText(error: AppError): String =
    when (error) {
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        AppError.Forbidden -> stringResource(R.string.error_forbidden)
        AppError.NotFound -> stringResource(R.string.error_not_found)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.Offline -> stringResource(R.string.error_offline)
        AppError.SessionSaveFailed -> stringResource(R.string.error_session_save)
        is AppError.Api -> stringResource(R.string.error_api, error.code.orEmpty())
        is AppError.InvalidData -> stringResource(R.string.error_invalid_data)
        is AppError.Update -> stringResource(when (error.reason) {
            UpdateFailure.NoRelease -> R.string.update_error_no_release
            UpdateFailure.InvalidRelease -> R.string.update_error_release
            UpdateFailure.RateLimited -> R.string.update_error_rate_limit
            UpdateFailure.ServiceUnavailable -> R.string.update_error_service
            UpdateFailure.Storage -> R.string.update_error_storage
            UpdateFailure.Integrity -> R.string.update_error_integrity
            UpdateFailure.IncompatiblePackage -> R.string.update_error_package
            UpdateFailure.SignatureMismatch -> R.string.update_error_signature
            UpdateFailure.InstallPermission -> R.string.update_error_permission
            UpdateFailure.InstallerUnavailable -> R.string.update_error_installer
            UpdateFailure.InstallFailed -> R.string.update_error_install
            UpdateFailure.FileMissing -> R.string.update_error_file
        })
    }
