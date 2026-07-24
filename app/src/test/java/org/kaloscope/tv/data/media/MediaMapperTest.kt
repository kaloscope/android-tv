package org.kaloscope.tv.data.media

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.model.MediaLibraryType
import org.kaloscope.tv.data.media.remote.MediaActorData
import org.kaloscope.tv.data.media.remote.MediaItemData
import org.kaloscope.tv.data.media.remote.MediaLibraryData
import org.kaloscope.tv.data.media.remote.MediaMetadataData
import org.kaloscope.tv.data.media.remote.MediaPageData

class MediaMapperTest {
    @Test
    fun `maps only the media library fields used by TV`() {
        val model = MediaLibraryData(
            id = 21,
            name = "剧集库",
            libraryType = "tv_show",
        ).toModel()

        assertEquals(21L, model.id)
        assertEquals("剧集库", model.name)
        assertEquals(MediaLibraryType.TvShow, model.type)
    }

    @Test
    fun `media page falls back to file name and accepts string rating`() {
        val page = MediaPageData(
            total = 1,
            items = listOf(
                MediaItemData(
                    id = 201,
                    name = "Stellar Archive",
                    path = "/media/Stellar Archive",
                    title = " ",
                    rating = JsonPrimitive("8.8"),
                ),
            ),
        ).toModel(pageNumber = 1, pageSize = 20)

        assertEquals("Stellar Archive", page.items.single().title)
        assertEquals(8.8, page.items.single().rating)
        assertEquals(false, page.hasNext)
    }

    @Test
    fun `detail hides invisible children and sorts playable parts`() {
        val detail = MediaItemData(
            id = 201,
            name = "Series",
            path = "/media/Series",
            title = "群星档案",
            poster = "posters/series.webp",
            children = listOf(
                child(id = 303, season = 1, episode = 2, title = "回声"),
                child(id = 304, season = 1, episode = 3, title = "隐藏", visible = false),
                child(id = 301, season = 1, episode = 1, title = "启程"),
                child(id = 302, season = null, episode = null, title = "序章"),
            ),
            metadata = MediaMetadataData(
                plot = "来自服务器的简介",
                genres = listOf("剧情", "科幻"),
                directors = listOf("林屿"),
                actors = listOf(MediaActorData(name = "沈川", role = "队长")),
            ),
        ).toDetail()

        checkNotNull(detail)
        assertEquals(listOf(302L, 301L, 303L), detail.children.map { it.id })
        assertEquals("来自服务器的简介", detail.plot)
        assertEquals(listOf("剧情", "科幻"), detail.genres)
        assertEquals(listOf("林屿"), detail.directors)
        assertEquals("沈川", detail.actors.single().name)
    }

    @Test
    fun `invalid detail does not create a dead destination`() {
        val detail = MediaItemData(
            id = 0,
            name = "",
            path = "",
        ).toDetail()

        assertNull(detail)
    }

    private fun child(
        id: Long,
        season: Int?,
        episode: Int?,
        title: String,
        visible: Boolean = true,
    ) = MediaItemData(
        id = id,
        name = "S${season}E${episode}.mkv",
        path = "/media/$id.mkv",
        visible = visible,
        title = title,
        season = season,
        episode = episode,
    )
}
