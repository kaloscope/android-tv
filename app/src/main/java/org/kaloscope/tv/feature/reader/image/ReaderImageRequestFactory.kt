package org.kaloscope.tv.feature.reader.image

import android.content.Context
import coil3.request.ImageRequest
import org.kaloscope.tv.core.designsystem.toCoilRequest
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
    ): ImageRequest? = resolve(session, rawUrl)?.toCoilRequest(context)
}
