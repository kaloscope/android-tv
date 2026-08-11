package org.kaloscope.tv.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.feature.detail.MediaDetailUiState

class RootStateInspectionTest {
    @Test
    fun `child detail authorization failure invalidates the ready session`() {
        val content = MediaDetailUiState.Content(
            parent = detail(),
            childDetailError = AppError.Unauthorized,
        )

        assertTrue(content.hasUnauthorized())
        assertFalse(content.copy(childDetailError = AppError.Offline).hasUnauthorized())
    }
}

private fun detail() = MediaDetail(
    id = 201,
    library = null,
    title = "群星档案",
    path = "/media/201",
    posterPath = null,
    backdropPath = null,
    year = 2026,
    rating = null,
    season = null,
    episode = null,
    aired = null,
    plot = "简介",
    genres = emptyList(),
    directors = emptyList(),
    writers = emptyList(),
    studios = emptyList(),
    actors = emptyList(),
    children = emptyList(),
)
