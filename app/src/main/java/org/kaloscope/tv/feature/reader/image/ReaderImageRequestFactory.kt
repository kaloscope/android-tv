package org.kaloscope.tv.feature.reader.image

import android.content.Context
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy
import org.kaloscope.tv.core.network.ServerImageRequest
import org.kaloscope.tv.core.network.ServerImageResolver

internal object ReaderImageRequestFactory {

    fun resolve(session: Session, rawUrl: String): ServerImageRequest? =
        ServerImageResolver.resolve(session, rawUrl, ServerImagePolicy.Auto)

    fun create(
        context: Context,
        session: Session,
        rawUrl: String,
    ): ImageRequest? {
        val request = resolve(session, rawUrl) ?: return null
        val headers = request.authorization?.let { authorization ->
            NetworkHeaders.Builder()
                .set("Authorization", authorization)
                .build()
        }
        return ImageRequest.Builder(context)
            .data(request.url)
            .apply { if (headers != null) httpHeaders(headers) }
            .build()
    }
}
