package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.tv.material3.Text
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy
import org.kaloscope.tv.core.network.ServerImageResolver
import org.kaloscope.tv.R

internal enum class ServerImageVisualState {
    Loading,
    Missing,
    Failed,
    Success,
}

@Composable
internal fun ServerImagePlaceholder(
    state: ServerImageVisualState,
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .testTag(
                when (state) {
                    ServerImageVisualState.Loading -> "server-image-loading"
                    ServerImageVisualState.Missing -> "server-image-missing"
                    ServerImageVisualState.Failed -> "server-image-failed"
                    ServerImageVisualState.Success -> "server-image-success"
                },
            )
            .background(Color(0xFF25334D)),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ServerImageVisualState.Loading -> KaloscopeSkeleton(Modifier.fillMaxSize())
            ServerImageVisualState.Missing -> Text(
                text = fallbackText.take(1).ifBlank { "K" },
                color = Color(0xFFBAC6E8),
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
            )
            ServerImageVisualState.Failed -> Image(
                painter = painterResource(R.drawable.ic_image_broken),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFFBAC6E8)),
            )
            ServerImageVisualState.Success -> Unit
        }
    }
}

@Composable
fun ServerImage(
    session: Session,
    rawValue: String?,
    fallbackText: String,
    contentDescription: String?,
    policy: ServerImagePolicy = ServerImagePolicy.Auto,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val request = remember(session.server.origin, session.token, rawValue, policy) {
        ServerImageResolver.resolve(session, rawValue, policy)
    }
    if (request == null) {
        ServerImagePlaceholder(
            state = ServerImageVisualState.Missing,
            fallbackText = fallbackText,
            modifier = modifier,
        )
    } else {
        var visualState by remember(request.url) {
            mutableStateOf(ServerImageVisualState.Loading)
        }
        val imageAlpha by animateFloatAsState(
            targetValue = if (visualState == ServerImageVisualState.Success) 1f else 0f,
            animationSpec = tween(KaloscopeMotion.ImageMillis),
            label = "server-image-alpha",
        )
        Box(modifier = modifier.background(Color(0xFF25334D))) {
            if (visualState != ServerImageVisualState.Success) {
                ServerImagePlaceholder(
                    state = visualState,
                    fallbackText = fallbackText,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            request.let { resolved ->
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
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("server-image-success"),
                contentScale = contentScale,
                alpha = imageAlpha,
                onState = { state ->
                    visualState = when (state) {
                        is AsyncImagePainter.State.Error -> ServerImageVisualState.Failed
                        is AsyncImagePainter.State.Success -> ServerImageVisualState.Success
                        else -> ServerImageVisualState.Loading
                    }
                },
            )
        }
        }
    }
}
