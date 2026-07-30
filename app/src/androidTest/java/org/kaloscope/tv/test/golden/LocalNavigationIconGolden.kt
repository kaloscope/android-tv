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
import org.kaloscope.tv.core.designsystem.KaloscopeNavigationIcon

private data class LocalIconGoldenSpec(
    val label: String,
    @DrawableRes val resource: Int,
)

@Composable
internal fun LocalNavigationIconGoldenSheet() {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LocalIconGoldenGroup("顶部导航", topNavigationIcons)
            LocalIconGoldenGroup("设置菜单", settingsIcons)
            LocalIconGoldenGroup("媒体库类型", libraryIcons)
        }
    }
}

@Composable
private fun LocalIconGoldenGroup(
    title: String,
    specs: List<LocalIconGoldenSpec>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            specs.forEach { spec ->
                Column(
                    modifier = Modifier.width(90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        KaloscopeNavigationIcon(iconRes = spec.resource)
                        Icon(
                            painter = painterResource(spec.resource),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    Text(
                        text = spec.label,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val topNavigationIcons = listOf(
    LocalIconGoldenSpec("首页 默认", R.drawable.ic_nav_home),
    LocalIconGoldenSpec("首页 选中", R.drawable.ic_nav_home_filled),
    LocalIconGoldenSpec("搜索 默认", R.drawable.ic_nav_search),
    LocalIconGoldenSpec("搜索 选中", R.drawable.ic_nav_search_filled),
    LocalIconGoldenSpec("媒体库 默认", R.drawable.ic_nav_library),
    LocalIconGoldenSpec("媒体库 选中", R.drawable.ic_nav_library_filled),
    LocalIconGoldenSpec("设置 默认", R.drawable.ic_nav_settings),
    LocalIconGoldenSpec("设置 选中", R.drawable.ic_nav_settings_filled),
)

private val settingsIcons = listOf(
    LocalIconGoldenSpec("播放", R.drawable.ic_settings_playback),
    LocalIconGoldenSpec("弹幕", R.drawable.ic_settings_danmaku),
    LocalIconGoldenSpec("字幕", R.drawable.ic_settings_subtitle),
    LocalIconGoldenSpec("行为", R.drawable.ic_settings_behavior),
    LocalIconGoldenSpec("服务器", R.drawable.ic_settings_server_account),
)

private val libraryIcons = listOf(
    LocalIconGoldenSpec("电影", R.drawable.ic_library_movie),
    LocalIconGoldenSpec("电视", R.drawable.ic_library_tv_show),
    LocalIconGoldenSpec("其他", R.drawable.ic_library_unknown),
)
