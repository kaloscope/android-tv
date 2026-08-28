package org.kaloscope.tv.data.server

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal object ServerConnectionOriginPolicy {
    fun resolve(
        requestedOrigin: String,
        finalUrl: HttpUrl,
    ): String? {
        val requestedUrl = requestedOrigin.toHttpUrl()
        val sameOrigin = requestedUrl.scheme == finalUrl.scheme &&
            requestedUrl.host == finalUrl.host &&
            requestedUrl.port == finalUrl.port
        if (sameOrigin) {
            return requestedOrigin
        }

        // Only a same-host HTTPS upgrade may replace the origin selected by the user.
        return if (
            requestedUrl.scheme == "http" &&
            finalUrl.scheme == "https" &&
            requestedUrl.host == finalUrl.host
        ) {
            finalUrl.origin
        } else {
            null
        }
    }
}

private val HttpUrl.origin: String
    get() {
        val serializedHost = if (host.contains(':')) "[$host]" else host
        val defaultPort = if (scheme == "http") 80 else 443
        val serializedPort = if (port == defaultPort) "" else ":$port"
        return "$scheme://$serializedHost$serializedPort"
    }
