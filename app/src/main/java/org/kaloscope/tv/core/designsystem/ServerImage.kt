package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImageResolver

@Composable
fun ServerImage(
    session: Session,
    rawValue: String?,
    fallbackText: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val request = remember(session.server.origin, session.token, rawValue) {
        ServerImageResolver.resolve(session, rawValue)
    }
    Box(
        modifier = modifier.background(Color(0xFF25334D)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = fallbackText.take(1).ifBlank { "K" },
            color = Color(0xFFBAC6E8),
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
        )
        request?.let { resolved ->
            val headers = resolved.authorization?.let { authorization ->
                NetworkHeaders.Builder()
                    .set("Authorization", authorization)
                    .build()
            }
            val imageRequest = ImageRequest.Builder(LocalContext.current)
                .data(resolved.url)
                .apply {
                    if (headers != null) {
                        httpHeaders(headers)
                    }
                }
                .build()
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}
