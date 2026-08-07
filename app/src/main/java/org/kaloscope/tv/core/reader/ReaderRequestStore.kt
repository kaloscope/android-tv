package org.kaloscope.tv.core.reader

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderRequestStore @Inject constructor() {
    private val requests = ConcurrentHashMap<String, ReaderRequest>()

    fun put(request: ReaderRequest) {
        requests[request.requestId] = request
    }

    fun get(requestId: String): ReaderRequest? = requests[requestId]

    fun remove(requestId: String) {
        requests.remove(requestId)
    }

    fun clearServer(serverId: String) {
        requests.entries
            .filter { it.value.serverId == serverId }
            .forEach { requests.remove(it.key, it.value) }
    }
}
