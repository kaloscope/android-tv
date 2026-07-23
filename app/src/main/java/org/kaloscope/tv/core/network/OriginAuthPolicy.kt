package org.kaloscope.tv.core.network

import java.net.URI

object OriginAuthPolicy {
    fun shouldAttachToken(
        serverOrigin: String,
        requestUrl: String,
    ): Boolean {
        val server = serverOrigin.toOrigin() ?: return false
        val request = requestUrl.toOrigin() ?: return false
        return server == request
    }
}

private data class Origin(
    val scheme: String,
    val host: String,
    val port: Int,
)

private fun String.toOrigin(): Origin? {
    val uri = runCatching { URI(this) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    val host = uri.host?.lowercase() ?: return null
    val port = when {
        uri.port >= 0 -> uri.port
        scheme == "http" -> 80
        scheme == "https" -> 443
        else -> return null
    }
    return Origin(scheme = scheme, host = host, port = port)
}
