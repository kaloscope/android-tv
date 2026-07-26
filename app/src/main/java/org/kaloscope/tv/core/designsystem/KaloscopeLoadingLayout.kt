package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun KaloscopeGridSkeleton(testTag: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().testTag(testTag).padding(24.dp),
    ) {
        KaloscopeSkeleton(
            Modifier.fillMaxWidth(0.56f).height(48.dp).clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.height(24.dp))
        repeat(2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                repeat(5) {
                    Column(Modifier.weight(1f)) {
                        KaloscopeSkeleton(
                            Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        Spacer(Modifier.height(8.dp))
                        KaloscopeSkeleton(
                            Modifier.fillMaxWidth(0.8f).height(14.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun KaloscopeDetailSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxSize().testTag("detail-loading-skeleton").padding(30.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        KaloscopeSkeleton(
            Modifier.width(250.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(16.dp)),
        )
        Column(Modifier.weight(1f)) {
            KaloscopeSkeleton(
                Modifier.fillMaxWidth(0.62f).height(40.dp).clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.height(18.dp))
            repeat(4) { index ->
                KaloscopeSkeleton(
                    Modifier.fillMaxWidth(if (index == 3) 0.58f else 0.88f)
                        .height(18.dp).clip(RoundedCornerShape(5.dp)),
                )
                Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Box(
                        Modifier.width(210.dp).aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp)),
                    ) {
                        KaloscopeSkeleton(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
