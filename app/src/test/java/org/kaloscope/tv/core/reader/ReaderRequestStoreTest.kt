package org.kaloscope.tv.core.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderImageContent

class ReaderRequestStoreTest {
    @Test
    fun `request is resolved by id and removed explicitly`() {
        val store = ReaderRequestStore()
        val request = imageRequest()

        store.put(request)

        assertEquals(request, store.get(request.requestId))
        store.remove(request.requestId)
        assertNull(store.get(request.requestId))
    }

    @Test
    fun `clearing one server preserves requests from another`() {
        val store = ReaderRequestStore()
        val first = imageRequest("first", "server-a")
        val second = imageRequest("second", "server-b")
        store.put(first)
        store.put(second)

        store.clearServer("server-a")

        assertNull(store.get("first"))
        assertEquals(second, store.get("second"))
    }

    private fun imageRequest(
        requestId: String = "reader-1",
        serverId: String = "server-a",
    ) = ReaderRequest.Image(
        requestId = requestId,
        serverId = serverId,
        content = ReaderImageContent.network(
            indexerId = 11,
            resourceId = "comic-1",
            title = "Comic",
            images = listOf("one.jpg"),
            imageCount = 1,
        ),
        settings = ImageReaderSettings(),
        chapterOrder = ReaderChapterOrder.Ascending,
    )
}
