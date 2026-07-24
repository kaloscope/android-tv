package org.kaloscope.tv.core.player

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRequestStore @Inject constructor() {
    private val requests = ConcurrentHashMap<String, PlaybackRequest>()

    fun put(request: PlaybackRequest) {
        requests[request.requestId] = request
    }

    fun get(requestId: String): PlaybackRequest? = requests[requestId]

    fun remove(requestId: String) {
        requests.remove(requestId)
    }

    fun clearServer(serverId: String) {
        requests.entries
            .filter { it.value.serverId == serverId }
            .forEach { requests.remove(it.key, it.value) }
    }
}
