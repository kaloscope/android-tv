package org.kaloscope.tv.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeFocusSurface
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.Session

@Composable
internal fun EpisodeCard(
    session: Session,
    episode: MediaSummary,
    selected: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KaloscopeFocusSurface(
        onClick = onClick,
        selected = selected,
        enabled = !loading,
        shape = RoundedCornerShape(14.dp),
        containerColor = Panel.copy(alpha = 0.74f),
        focusedContainerColor = PanelElevated,
        focusScale = 1.04f,
        modifier = modifier.width(230.dp).testTag("episode-card-${episode.id}"),
    ) {
        Column(Modifier.padding(8.dp)) {
            Box {
                ServerImage(
                    session = session,
                    rawValue = episode.posterPath,
                    fallbackText = episode.title,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp)),
                )
                episode.episode?.let { number ->
                    Text(
                        text = stringResource(R.string.episode_number, number),
                        color = OnBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.TopStart)
                            .padding(7.dp)
                            .background(Color(0xD9121824), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = episode.title,
                color = OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            (if (loading) stringResource(R.string.loading) else episode.aired)
                ?.let { secondary ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = secondary,
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
        }
    }
}
