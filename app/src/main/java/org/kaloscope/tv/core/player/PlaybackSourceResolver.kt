package org.kaloscope.tv.core.player

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.kaloscope.tv.core.model.Session

object PlaybackSourceResolver {
    fun directStreamUrl(
        session: Session,
        path: String,
    ): String =
        "${session.server.origin}/_api/media/stream"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("path", path)
            .build()
            .toString()

    fun resolveServerResource(
        session: Session,
        rawUrl: String,
    ): String =
        when {
            rawUrl.startsWith("/") -> "${session.server.origin}$rawUrl"
            else -> rawUrl
        }
}
