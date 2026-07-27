package org.kaloscope.tv.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.ServerImage
import org.kaloscope.tv.core.model.MediaActor
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.network.ServerImagePolicy

@Composable
internal fun CastStrip(
    session: Session,
    actors: List<MediaActor>,
    modifier: Modifier = Modifier,
) {
    if (actors.isEmpty()) return
    Column(modifier.testTag("cast-strip")) {
        Text(
            text = stringResource(R.string.cast_title),
            color = OnBackground,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            actors.take(8).forEachIndexed { index, actor ->
                Column(
                    modifier = Modifier.width(88.dp).testTag("cast-item-$index"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ServerImage(
                        session = session,
                        rawValue = actor.thumbPath,
                        fallbackText = actor.name,
                        contentDescription = null,
                        policy = ServerImagePolicy.Store,
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = actor.name,
                        color = OnBackground,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    actor.role?.takeIf(String::isNotBlank)?.let { role ->
                        Text(
                            text = role,
                            color = Muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
