package org.kaloscope.tv.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.kaloscope.tv.core.common.trimmedOrNull
import org.kaloscope.tv.core.model.Session

data class ServerImageRequest(
    val url: String,
    val authorization: String?,
)

enum class ServerImagePolicy {
    Direct,
    Auto,
    Proxy,
    Store,
}

object ServerImageResolver {
    fun resolve(
        session: Session,
        rawValue: String?,
        policy: ServerImagePolicy = ServerImagePolicy.Auto,
    ): ServerImageRequest? {
        val raw = rawValue.trimmedOrNull() ?: return null
        val serverOrigin = session.server.origin.removeSuffix("/")
        val absolute = raw.toHttpUrlOrNull()
        val resolvedUrl = when {
            absolute != null -> when (policy) {
                ServerImagePolicy.Direct -> absolute.toString()
                ServerImagePolicy.Auto -> proxyUrl(
                    serverOrigin = serverOrigin,
                    rawValue = raw,
                    store = absolute.queryParameter("proxy") == "store",
                ) ?: return null
                ServerImagePolicy.Proxy -> proxyUrl(
                    serverOrigin = serverOrigin,
                    rawValue = raw,
                    store = false,
                ) ?: return null
                ServerImagePolicy.Store -> proxyUrl(
                    serverOrigin = serverOrigin,
                    rawValue = raw,
                    store = true,
                ) ?: return null
            }
            raw.startsWith("/") -> "$serverOrigin$raw"
            else -> "$serverOrigin/_api/$raw"
        }
        // Authorization is derived after URL resolution to avoid leaking it off-origin.
        val authorization = if (
            OriginAuthPolicy.shouldAttachToken(serverOrigin, resolvedUrl)
        ) {
            session.authorizationHeader()
        } else {
            null
        }
        return ServerImageRequest(resolvedUrl, authorization)
    }
}

private fun proxyUrl(
    serverOrigin: String,
    rawValue: String,
    store: Boolean,
): String? {
    val server = serverOrigin.toHttpUrlOrNull() ?: return null
    return server.newBuilder()
        .addPathSegments("_api/image/proxy")
        .addQueryParameter("store", store.toString())
        .addQueryParameter("url", rawValue)
        .build()
        .toString()
}
