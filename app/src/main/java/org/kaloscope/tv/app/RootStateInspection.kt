package org.kaloscope.tv.app

import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.feature.detail.MediaDetailUiState
import org.kaloscope.tv.feature.home.HomeUiState
import org.kaloscope.tv.feature.library.LibraryItemsState
import org.kaloscope.tv.feature.library.LibraryUiState
import org.kaloscope.tv.feature.player.PlayerUiState
import org.kaloscope.tv.feature.search.SearchResultsState
import org.kaloscope.tv.feature.search.SearchUiState

internal fun HomeUiState.hasUnauthorized(): Boolean =
    this is HomeUiState.Error && error == AppError.Unauthorized

internal fun LibraryUiState.hasUnauthorized(): Boolean =
    when (this) {
        is LibraryUiState.Error -> error == AppError.Unauthorized
        is LibraryUiState.Content -> when (val itemState = items) {
            is LibraryItemsState.Error -> itemState.error == AppError.Unauthorized
            is LibraryItemsState.Content ->
                itemState.loadMoreError == AppError.Unauthorized

            else -> false
        }

        else -> false
    }

internal fun SearchUiState.hasUnauthorized(): Boolean =
    when (this) {
        is SearchUiState.Error -> error == AppError.Unauthorized
        is SearchUiState.Content -> {
            val resultUnauthorized = when (val resultState = results) {
                is SearchResultsState.Error -> resultState.error == AppError.Unauthorized
                is SearchResultsState.Content ->
                    resultState.loadMoreError == AppError.Unauthorized

                else -> false
            }
            resultUnauthorized || playbackError == AppError.Unauthorized
        }

        else -> false
    }

internal fun MediaDetailUiState.hasUnauthorized(): Boolean =
    when (this) {
        is MediaDetailUiState.Error -> error == AppError.Unauthorized
        is MediaDetailUiState.Content -> false
        MediaDetailUiState.Loading -> false
    }

internal fun PlayerUiState.hasUnauthorized(): Boolean =
    this is PlayerUiState.Content &&
        (
            progressError == AppError.Unauthorized ||
                extraFailures.values.any { it == AppError.Unauthorized }
        )
