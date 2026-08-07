package org.kaloscope.tv.feature.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
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
    KaloscopeFocusSurface(
        onClick = onClick,
        selected = focusedTarget,
        shape = RoundedCornerShape(14.dp),
        containerColor = Panel.copy(alpha = 0.74f),
        focusedContainerColor = PanelElevated,
        focusScale = 1.035f,
        modifier = modifier
            .onFocusChanged { if (it.isFocused) onFocused() }
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
                color = OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            (child.aired ?: child.year?.toString())?.let { supportingText ->
                Spacer(Modifier.height(3.dp))
                Text(
                    text = supportingText,
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
