package org.kaloscope.tv.test.golden

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton

private data class LocalActionIconGoldenSpec(
    val label: String,
    @DrawableRes val resource: Int,
    val selected: Boolean,
)

@Composable
internal fun LocalActionIconGoldenSheet() {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text(
                text = "WebUI 操作图标",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            actionIconSpecs.forEach { spec ->
                LocalActionIconGoldenRow(spec)
            }
        }
    }
}

@Composable
private fun LocalActionIconGoldenRow(spec: LocalActionIconGoldenSpec) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text(
            text = spec.label,
            modifier = Modifier.width(160.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            painter = painterResource(spec.resource),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Icon(
            painter = painterResource(spec.resource),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        KaloscopeIconButton(
            onClick = {},
            selected = spec.selected,
            modifier = Modifier.size(52.dp),
            variant = KaloscopeControlVariant.Filled,
            size = KaloscopeControlSize.Compact,
        ) {
            Icon(
                painter = painterResource(spec.resource),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private val actionIconSpecs = listOf(
    LocalActionIconGoldenSpec(
        label = "搜索",
        resource = R.drawable.ic_action_search,
        selected = false,
    ),
    LocalActionIconGoldenSpec(
        label = "筛选（启用）",
        resource = R.drawable.ic_action_filter,
        selected = true,
    ),
)
