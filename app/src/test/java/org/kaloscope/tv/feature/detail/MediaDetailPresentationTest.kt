package org.kaloscope.tv.feature.detail

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaLibrary
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.core.model.MediaSummary

class MediaDetailPresentationTest {
    @Test
    fun `focused child artwork follows WebUI fallback order`() {
        val parent = detail(
            type = MediaLibraryType.TvShow,
            poster = "parent-poster",
            backdrop = "parent-backdrop",
        )
        val focused = child(
            id = 301,
            title = "Episode 1",
            poster = "child-poster",
            backdrop = "child-backdrop",
            season = 1,
            episode = 1,
        )

        assertEquals("child-backdrop", resolveDetailBackdrop(parent, focused))
        assertEquals(
            "parent-backdrop",
            resolveDetailBackdrop(parent, focused.copy(backdropPath = null)),
        )
        assertEquals(
            "child-poster",
            resolveDetailBackdrop(
                parent.copy(backdropPath = null),
                focused.copy(backdropPath = null),
            ),
        )
        assertEquals(
            "parent-poster",
            resolveDetailBackdrop(
                parent.copy(backdropPath = null),
                focused.copy(backdropPath = null, posterPath = null),
            ),
        )
    }

    @Test
    fun `library type selects episode or part semantics`() {
        assertEquals(
            MediaChildSectionKind.Episodes,
            childSectionKind(detail(MediaLibraryType.TvShow)),
        )
        assertEquals(
            MediaChildSectionKind.Parts,
            childSectionKind(detail(MediaLibraryType.Movie)),
        )
        assertEquals(
            MediaChildSectionKind.Parts,
            childSectionKind(detail(MediaLibraryType.Unknown)),
        )
    }
}

private fun detail(
    type: MediaLibraryType,
    poster: String? = null,
    backdrop: String? = null,
) = MediaDetail(
    id = 201,
    library = MediaLibrary(21, "Fixture library", type),
    title = "Fixture title",
    path = "/media/fixture",
    posterPath = poster,
    backdropPath = backdrop,
    year = 2026,
    rating = 8.5,
    season = null,
    episode = null,
    aired = null,
    plot = "Fixture plot",
    genres = emptyList(),
    directors = emptyList(),
    writers = emptyList(),
    studios = emptyList(),
    actors = emptyList(),
    children = emptyList(),
)

private fun child(
    id: Long,
    title: String,
    poster: String? = null,
    backdrop: String? = null,
    season: Int? = 1,
    episode: Int? = 1,
) = MediaSummary(
    id = id,
    title = title,
    path = "/media/$id.mkv",
    posterPath = poster,
    backdropPath = backdrop,
    year = 2026,
    rating = null,
    season = season,
    episode = episode,
    aired = "2026-01-01",
)
