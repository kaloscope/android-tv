package org.kaloscope.tv.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.kaloscope.tv.core.model.Session

data class ServerImageRequest(
    val url: String,
    val authorization: String?,
)

object ServerImageResolver {
    fun resolve(
        session: Session,
        rawValue: String?,
    ): ServerImageRequest? {
        val raw = rawValue?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val serverOrigin = session.server.origin.removeSuffix("/")
        val absolute = raw.toHttpUrlOrNull()
        val resolvedUrl = when {
            absolute != null && absolute.queryParameter("proxy") in setOf("true", "store") -> {
                val server = serverOrigin.toHttpUrlOrNull() ?: return null
                server.newBuilder()
                    .addPathSegments("_api/image/proxy")
                    .addQueryParameter(
                        "store",
                        (absolute.queryParameter("proxy") == "store").toString(),
                    )
                    .addQueryParameter("url", raw)
                    .build()
                    .toString()
            }

            absolute != null -> absolute.toString()
            raw.startsWith("/") -> "$serverOrigin$raw"
            else -> "$serverOrigin/_api/$raw"
        }
        // Authorization is derived after URL resolution to avoid leaking it off-origin.
        val authorization = if (
            OriginAuthPolicy.shouldAttachToken(serverOrigin, resolvedUrl)
        ) {
            "Token ${session.token}"
        } else {
            null
        }
        return ServerImageRequest(resolvedUrl, authorization)
    }
}
