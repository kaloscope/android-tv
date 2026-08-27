package org.kaloscope.tv.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
private fun ServerImageSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "image-skeleton")
    val offset = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "image-skeleton-offset",
    ).value
    val base = Color(0xFF202B40)
    val highlight = Color(0xFF34425E)
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to base,
            (offset - 0.18f).coerceIn(0f, 1f) to base,
            offset.coerceIn(0f, 1f) to highlight,
            (offset + 0.18f).coerceIn(0f, 1f) to base,
            1f to base,
        ),
    )
    Box(modifier = modifier.background(brush))
}

@Composable
internal fun ServerImagePlaceholder(
    state: ServerImageVisualState,
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
            ServerImageVisualState.Loading -> ServerImageSkeleton(Modifier.fillMaxSize())
            ServerImageVisualState.Missing,
            ServerImageVisualState.Failed -> ServerImageBrokenIcon()
            ServerImageVisualState.Success -> Unit
        }
    }
}

@Composable
private fun ServerImageBrokenIcon() {
    Image(
        painter = painterResource(R.drawable.ic_image_broken),
        contentDescription = null,
        colorFilter = ColorFilter.tint(Color(0xFFBAC6E8).copy(alpha = 0.55f)),
        modifier = Modifier.testTag("server-image-broken-icon"),
    )
}

@Composable
fun ServerImage(
    session: Session,
    rawValue: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    policy: ServerImagePolicy = ServerImagePolicy.Auto,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val request = remember(session.server.origin, session.token, rawValue, policy) {
        ServerImageResolver.resolve(session, rawValue, policy)
    }
    if (request == null) {
        ServerImagePlaceholder(
            state = ServerImageVisualState.Missing,
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
                    modifier = Modifier.fillMaxSize(),
                )
            }
            val context = LocalContext.current
            val imageRequest = remember(context, request.url, request.authorization) {
                ImageRequest.Builder(context)
                    .data(request.url)
                    .apply {
                        request.authorization?.let { authorization ->
                            httpHeaders(
                                NetworkHeaders.Builder()
                                    .set("Authorization", authorization)
                                    .build(),
                            )
                        }
                    }
                    .build()
            }
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
