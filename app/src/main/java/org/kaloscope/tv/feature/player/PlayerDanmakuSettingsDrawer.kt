package org.kaloscope.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize

@Composable
internal fun PlayerDanmakuSettingsDrawer(
    settings: DanmakuSettings,
    onChange: (DanmakuSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        initialFocus.requestFocus()
    }
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(430.dp)
                .background(Panel.copy(alpha = 0.97f))
                .padding(horizontal = 28.dp, vertical = 34.dp),
        ) {
            Text(
                text = stringResource(R.string.danmaku_settings_title),
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.player_danmaku_temporary_description),
                color = Muted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    DrawerToggleRow(
                        title = stringResource(R.string.player_danmaku_enabled),
                        checked = settings.enabled,
                        onToggle = {
                            onChange(settings.copy(enabled = !settings.enabled))
                        },
                        modifier = Modifier
                            .focusRequester(initialFocus)
                            .focusProperties { up = FocusRequester.Cancel },
                    )
                }
                item {
                    DrawerChoiceRow(
                        title = stringResource(R.string.danmaku_text_size),
                        values = DanmakuTextSize.entries,
                        selected = settings.textSize,
                        label = { danmakuTextSizeLabel(it) },
                        onSelect = { onChange(settings.copy(textSize = it)) },
                    )
                }
                item {
                    DrawerChoiceRow(
                        title = stringResource(R.string.danmaku_speed),
                        values = DanmakuSpeed.entries,
                        selected = settings.speed,
                        label = { danmakuSpeedLabel(it) },
                        onSelect = { onChange(settings.copy(speed = it)) },
                    )
                }
                item {
                    DrawerChoiceRow(
                        title = stringResource(R.string.danmaku_opacity),
                        values = listOf(25, 50, 75, 100),
                        selected = settings.opacityPercent,
                        label = { stringResource(R.string.percentage_value, it) },
                        onSelect = { onChange(settings.copy(opacityPercent = it)) },
                    )
                }
                item {
                    DrawerChoiceRow(
                        title = stringResource(R.string.danmaku_display_area),
                        values = listOf(25, 50, 75, 100),
                        selected = settings.displayAreaPercent,
                        label = { stringResource(R.string.percentage_value, it) },
                        onSelect = { onChange(settings.copy(displayAreaPercent = it)) },
                    )
                }
                DanmakuDisplayMode.entries.forEachIndexed { index, mode ->
                    item {
                        DrawerToggleRow(
                            title = danmakuModeLabel(mode),
                            checked = mode in settings.visibleModes,
                            onToggle = {
                                val modes = if (mode in settings.visibleModes) {
                                    settings.visibleModes - mode
                                } else {
                                    settings.visibleModes + mode
                                }
                                onChange(settings.copy(visibleModes = modes))
                            },
                            modifier = if (index == DanmakuDisplayMode.entries.lastIndex) {
                                Modifier.focusProperties { down = FocusRequester.Cancel }
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerToggleRow(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DrawerRow(
        title = title,
        value = if (checked) {
            stringResource(R.string.enabled)
        } else {
            stringResource(R.string.disabled)
        },
        onClick = onToggle,
        modifier = modifier.onPreviewKeyEvent { event ->
            event.type == KeyEventType.KeyDown &&
                (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
        },
        active = checked,
    )
}

@Composable
private fun <T> DrawerChoiceRow(
    title: String,
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    val index = values.indexOf(selected).coerceAtLeast(0)
    fun selectOffset(offset: Int) {
        values.getOrNull((index + offset).coerceIn(0, values.lastIndex))?.let(onSelect)
    }
    DrawerRow(
        title = title,
        value = "‹  ${label(selected)}  ›",
        onClick = { selectOffset(1) },
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) {
                return@onPreviewKeyEvent false
            }
            when (event.key) {
                Key.DirectionLeft -> {
                    selectOffset(-1)
                    true
                }

                Key.DirectionRight -> {
                    selectOffset(1)
                    true
                }

                else -> false
            }
        },
    )
}

@Composable
private fun DrawerRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    KaloscopeButton(
        onClick = onClick,
        selected = active,
        size = KaloscopeControlSize.Row,
        modifier = modifier
            .fillMaxWidth()
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(text = value, fontSize = 14.sp)
        }
    }
}

@Composable
private fun danmakuTextSizeLabel(size: DanmakuTextSize): String =
    when (size) {
        DanmakuTextSize.Small -> stringResource(R.string.danmaku_size_small)
        DanmakuTextSize.Medium -> stringResource(R.string.danmaku_size_medium)
        DanmakuTextSize.Large -> stringResource(R.string.danmaku_size_large)
        DanmakuTextSize.ExtraLarge -> stringResource(R.string.danmaku_size_extra_large)
    }

@Composable
private fun danmakuSpeedLabel(speed: DanmakuSpeed): String =
    when (speed) {
        DanmakuSpeed.Slow -> stringResource(R.string.danmaku_speed_slow)
        DanmakuSpeed.Standard -> stringResource(R.string.danmaku_speed_standard)
        DanmakuSpeed.Fast -> stringResource(R.string.danmaku_speed_fast)
    }

@Composable
private fun danmakuModeLabel(mode: DanmakuDisplayMode): String =
    when (mode) {
        DanmakuDisplayMode.Scroll -> stringResource(R.string.danmaku_mode_scroll)
        DanmakuDisplayMode.Top -> stringResource(R.string.danmaku_mode_top)
        DanmakuDisplayMode.Bottom -> stringResource(R.string.danmaku_mode_bottom)
    }
