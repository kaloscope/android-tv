package org.kaloscope.tv.app

import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.library.hasUnauthorizedError
import org.kaloscope.tv.feature.player.PlayerUiState
import org.kaloscope.tv.feature.reader.ReaderUiState
import org.kaloscope.tv.feature.search.SearchUiState
import org.kaloscope.tv.feature.search.hasUnauthorizedError

internal fun HomeUiState.hasUnauthorized(): Boolean =
    when (this) {
        is HomeUiState.Error -> error == AppError.Unauthorized
        is HomeUiState.Content -> refreshError == AppError.Unauthorized
        else -> false
    }

internal fun LibraryUiState.hasUnauthorized(): Boolean = hasUnauthorizedError()

internal fun SearchUiState.hasUnauthorized(): Boolean = hasUnauthorizedError()

internal fun MediaDetailUiState.hasUnauthorized(): Boolean =
    when (this) {
        is MediaDetailUiState.Error -> error == AppError.Unauthorized
        is MediaDetailUiState.Content -> childDetailError == AppError.Unauthorized
        MediaDetailUiState.Loading -> false
    }

internal fun PlayerUiState.hasUnauthorized(): Boolean =
    when (this) {
        is PlayerUiState.Loading -> progressError == AppError.Unauthorized
        is PlayerUiState.Content ->
            progressError == AppError.Unauthorized ||
                switchError == AppError.Unauthorized ||
                extraFailures.values.any { it == AppError.Unauthorized }
        PlayerUiState.MissingRequest -> false
    }

internal fun ReaderUiState.hasUnauthorized(): Boolean =
    when (this) {
        is ReaderUiState.Error -> error == AppError.Unauthorized
        is ReaderUiState.Image ->
            chapterError == AppError.Unauthorized || pageError == AppError.Unauthorized ||
                progressError == AppError.Unauthorized
        is ReaderUiState.Text -> chapterError == AppError.Unauthorized || progressError == AppError.Unauthorized
        ReaderUiState.Idle, is ReaderUiState.Loading -> false
    }
