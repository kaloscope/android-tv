package org.kaloscope.tv.core.designsystem

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
fun ServerBackdrop(
    session: Session,
    backdropPath: String?,
    modifier: Modifier = Modifier,
    policy: ServerImagePolicy = ServerImagePolicy.Auto,
) {
    Crossfade(
        targetState = backdropPath,
        animationSpec = tween(KaloscopeMotion.BackgroundMillis),
        label = "server-backdrop",
        modifier = modifier,
    ) { path ->
        if (!path.isNullOrBlank()) {
            ServerImage(
                session = session,
                rawValue = path,
                contentDescription = null,
                policy = policy,
                modifier = Modifier.fillMaxSize().testTag("detail-backdrop-$path"),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
