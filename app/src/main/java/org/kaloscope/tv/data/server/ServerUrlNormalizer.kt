package org.kaloscope.tv.data.server

import java.net.URI
import java.net.URISyntaxException

enum class ServerUrlError {
    Empty,
    UnsupportedScheme,
    MissingHost,
    CredentialsNotAllowed,
    InvalidPort,
    PathNotAllowed,
}

class InvalidServerUrl(
    val reason: ServerUrlError,
) : IllegalArgumentException(reason.name)

object ServerUrlNormalizer {
    fun normalize(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        if (trimmed.isEmpty()) {
            throw InvalidServerUrl(ServerUrlError.Empty)
        }

        val uri = try {
            URI(trimmed)
        } catch (_: URISyntaxException) {
            throw InvalidServerUrl(ServerUrlError.MissingHost)
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw InvalidServerUrl(ServerUrlError.UnsupportedScheme)
        }
        if (uri.host.isNullOrBlank()) {
            throw InvalidServerUrl(ServerUrlError.MissingHost)
        }
        if (uri.userInfo != null) {
            throw InvalidServerUrl(ServerUrlError.CredentialsNotAllowed)
        }
        if (uri.port == 0 || uri.port > 65_535) {
            throw InvalidServerUrl(ServerUrlError.InvalidPort)
        }
        if (uri.path?.isNotEmpty() == true || uri.query != null || uri.fragment != null) {
            throw InvalidServerUrl(ServerUrlError.PathNotAllowed)
        }

        val host = if (uri.host.contains(':')) "[${uri.host}]" else uri.host
        val port = if (uri.port == -1) "" else ":${uri.port}"
        return "$scheme://${host.lowercase()}$port"
    }
}
