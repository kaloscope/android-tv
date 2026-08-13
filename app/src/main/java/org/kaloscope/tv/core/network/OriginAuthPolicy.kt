package org.kaloscope.tv.core.network

import java.net.URI
import org.kaloscope.tv.core.model.Session

/**
 * Formats Kaloscope's token scheme after a caller has bound or validated the request origin.
 */
internal fun Session.authorizationHeader(): String = "Token $token"

/**
 * Prevents a Kaloscope token from being attached to third-party resource URLs.
 */
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
    // Explicit and implicit default ports represent the same HTTP origin.
    val port = when {
        uri.port >= 0 -> uri.port
        scheme == "http" -> 80
        scheme == "https" -> 443
        else -> return null
    }
    return Origin(scheme = scheme, host = host, port = port)
}
