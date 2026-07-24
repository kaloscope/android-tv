package org.kaloscope.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackRequestStoreTest {
    @Test
    fun `request is resolved by id and removed explicitly`() {
        val store = PlaybackRequestStore()
        val request = localPlaybackRequest()

        store.put(request)

        assertEquals(request, store.get(request.requestId))
        store.remove(request.requestId)
        assertNull(store.get(request.requestId))
    }

    @Test
    fun `clearing a server leaves other server requests intact`() {
        val store = PlaybackRequestStore()
        val first = localPlaybackRequest(requestId = "first", serverId = "server-a")
        val second = localPlaybackRequest(requestId = "second", serverId = "server-b")
        store.put(first)
        store.put(second)

        store.clearServer("server-a")

        assertNull(store.get("first"))
        assertEquals(second, store.get("second"))
    }
}

private fun localPlaybackRequest(
    requestId: String = "request-1",
    serverId: String = "server-a",
) = PlaybackRequest.LocalMedia(
    requestId = requestId,
    serverId = serverId,
    mediaId = 301,
    path = "/media/video.mkv",
    title = "Episode 1",
    resumePositionSeconds = 42,
    origin = PlaybackOrigin.MediaDetail,
)
