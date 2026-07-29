package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError

@Composable
internal fun appErrorText(error: AppError): String =
    when (error) {
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        AppError.Forbidden -> stringResource(R.string.error_forbidden)
        AppError.NotFound -> stringResource(R.string.error_not_found)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.Offline -> stringResource(R.string.error_offline)
        is AppError.Api -> stringResource(R.string.error_api, error.code.orEmpty())
        is AppError.InvalidData -> stringResource(R.string.error_invalid_data)
    }
