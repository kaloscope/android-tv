package org.kaloscope.tv.core.designsystem

import android.content.Context
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import org.kaloscope.tv.core.network.ServerImageRequest

internal fun ServerImageRequest.toCoilRequest(context: Context): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .apply {
            authorization?.let { authorization ->
                httpHeaders(
                    NetworkHeaders.Builder()
                        .set("Authorization", authorization)
                        .build(),
                )
            }
        }
        .build()
