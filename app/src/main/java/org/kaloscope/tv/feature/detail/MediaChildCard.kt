package org.kaloscope.tv.feature.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.KaloscopeMotion
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Outline
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
internal fun MediaChildCard(
    session: Session,
    child: MediaSummary,
    focusedTarget: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val accentPalette = LocalAccentPalette.current
    var isFocused by remember(child.id) { mutableStateOf(false) }
    val titleColor by animateColorAsState(
        targetValue = if (focusedTarget) accentPalette.primary else OnBackground,
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "episode-title-color",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            focusedTarget -> accentPalette.primary.copy(alpha = 0.7f)
            else -> Outline
        },
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "episode-border-color",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        animationSpec = tween(
            durationMillis = KaloscopeMotion.FocusMillis,
            easing = KaloscopeMotion.ControlEasing,
        ),
        label = "episode-border-width",
    )
    val supportingText = child.aired ?: child.year?.toString()

    KaloscopeFocusSurface(
        onClick = onClick,
        selected = focusedTarget,
        shape = shape,
        containerColor = Panel.copy(alpha = 0.74f),
        focusedContainerColor = PanelElevated,
        focusScale = 1.035f,
        modifier = modifier
            .border(borderWidth, borderColor, shape)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .testTag("media-child-card-${child.id}"),
    ) {
        Column(Modifier.padding(8.dp)) {
            ServerImage(
                session = session,
                rawValue = child.posterPath,
                fallbackText = child.title,
                contentDescription = null,
                policy = ServerImagePolicy.Store,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = mediaChildDisplayTitle(child),
                color = titleColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("media-child-title-${child.id}"),
            )
            Spacer(Modifier.height(3.dp))
            Box(Modifier.height(16.dp)) {
                Text(
                    text = supportingText.orEmpty(),
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
