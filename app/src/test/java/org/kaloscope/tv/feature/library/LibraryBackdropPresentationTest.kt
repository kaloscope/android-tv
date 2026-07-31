package org.kaloscope.tv.feature.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.model.MediaSummary

class LibraryBackdropPresentationTest {
    @Test
    fun `valid restore item wins over focused and first items`() {
        val presentation = resolveLibraryBackdropPresentation(
            items = listOf(
                media(id = 1, backdropPath = "/first.jpg"),
                media(id = 2, backdropPath = "/focused.jpg"),
                media(id = 3, backdropPath = "/restored.jpg"),
            ),
            restoreMediaId = 3,
            focusedMediaId = 2,
        )

        assertEquals(
            LibraryBackdropPresentation("/restored.jpg", "媒体3"),
            presentation,
        )
    }

    @Test
    fun `invalid restore item falls through to focused item`() {
        val presentation = resolveLibraryBackdropPresentation(
            items = listOf(
                media(id = 1, backdropPath = "/first.jpg"),
                media(id = 2, backdropPath = "/focused.jpg"),
            ),
            restoreMediaId = 999,
            focusedMediaId = 2,
        )

        assertEquals(
            LibraryBackdropPresentation("/focused.jpg", "媒体2"),
            presentation,
        )
    }

    @Test
    fun `invalid restore and focused items fall through to first item`() {
        val presentation = resolveLibraryBackdropPresentation(
            items = listOf(
                media(id = 1, backdropPath = "/first.jpg"),
                media(id = 2, backdropPath = "/second.jpg"),
            ),
            restoreMediaId = 999,
            focusedMediaId = 998,
        )

        assertEquals(
            LibraryBackdropPresentation("/first.jpg", "媒体1"),
            presentation,
        )
    }

    @Test
    fun `candidate backdrop is preferred over its poster`() {
        val presentation = resolveLibraryBackdropPresentation(
            items = listOf(
                media(
                    id = 1,
                    posterPath = "/poster.jpg",
                    backdropPath = "/backdrop.jpg",
                ),
            ),
            restoreMediaId = null,
            focusedMediaId = null,
        )

        assertEquals(
            LibraryBackdropPresentation("/backdrop.jpg", "媒体1"),
            presentation,
        )
    }

    @Test
    fun `blank backdrop falls through to candidate poster`() {
        val presentation = resolveLibraryBackdropPresentation(
            items = listOf(
                media(
                    id = 1,
                    posterPath = " /poster.jpg ",
                    backdropPath = "  ",
                ),
            ),
            restoreMediaId = null,
            focusedMediaId = null,
        )

        assertEquals(
            LibraryBackdropPresentation("/poster.jpg", "媒体1"),
            presentation,
        )
    }

    @Test
    fun `image-less preferred item falls through in display order`() {
        val presentation = resolveLibraryBackdropPresentation(
            items = listOf(
                media(id = 1),
                media(id = 2, posterPath = "/second-poster.jpg"),
                media(
                    id = 3,
                    posterPath = "/third-poster.jpg",
                    backdropPath = "/third-backdrop.jpg",
                ),
            ),
            restoreMediaId = null,
            focusedMediaId = null,
        )

        assertEquals(
            LibraryBackdropPresentation("/second-poster.jpg", "媒体2"),
            presentation,
        )
    }

    @Test
    fun `fallback item still prefers backdrop over poster`() {
        val presentation = resolveLibraryBackdropPresentation(
            items = listOf(
                media(id = 1),
                media(
                    id = 2,
                    posterPath = "/second-poster.jpg",
                    backdropPath = "/second-backdrop.jpg",
                ),
            ),
            restoreMediaId = null,
            focusedMediaId = null,
        )

        assertEquals(
            LibraryBackdropPresentation("/second-backdrop.jpg", "媒体2"),
            presentation,
        )
    }

    @Test
    fun `presentation title belongs to item supplying fallback image`() {
        val presentation = resolveLibraryBackdropPresentation(
            items = listOf(
                media(id = 1, title = "无图项目"),
                media(
                    id = 2,
                    title = "背景来源",
                    backdropPath = "/source.jpg",
                ),
            ),
            restoreMediaId = null,
            focusedMediaId = null,
        )

        assertEquals(
            LibraryBackdropPresentation("/source.jpg", "背景来源"),
            presentation,
        )
    }

    @Test
    fun `items without a usable image produce no presentation`() {
        val presentation = resolveLibraryBackdropPresentation(
            items = listOf(
                media(id = 1, posterPath = null, backdropPath = " "),
                media(id = 2, posterPath = "\t", backdropPath = null),
            ),
            restoreMediaId = 1,
            focusedMediaId = 2,
        )

        assertNull(presentation)
    }
}

private fun media(
    id: Long,
    title: String = "媒体$id",
    posterPath: String? = null,
    backdropPath: String? = null,
) = MediaSummary(
    id = id,
    title = title,
    path = "/media/$id",
    posterPath = posterPath,
    backdropPath = backdropPath,
    year = null,
    rating = null,
    season = null,
    episode = null,
)
