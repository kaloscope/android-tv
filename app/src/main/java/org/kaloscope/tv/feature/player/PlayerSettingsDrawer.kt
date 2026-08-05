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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.testTag
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
import org.kaloscope.tv.core.designsystem.danmakuSpeedLabel
import org.kaloscope.tv.core.designsystem.danmakuTextSizeLabel
import org.kaloscope.tv.core.designsystem.formatSubtitleOffset
import org.kaloscope.tv.core.designsystem.subtitleDisplayModeLabel
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleSettingsPolicy
import org.kaloscope.tv.core.model.SubtitleTrack

@Composable
internal fun PlayerSettingsDrawer(
    subtitleTracks: List<SubtitleTrack>,
    selectedSubtitleTrackId: String?,
    subtitleSettings: SubtitleSettings,
    danmakuSettings: DanmakuSettings?,
    onSelectSubtitleTrack: (String) -> Unit,
    onChangeSubtitleSettings: (SubtitleSettings) -> Unit,
    onChangeDanmakuSettings: (DanmakuSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    val blockRowFocus = remember { FocusRequester() }
    var blockMenuOpen by remember { mutableStateOf(false) }
    var restoreBlockRowFocus by remember { mutableStateOf(false) }
    val initialSubtitleTrackId = selectedSubtitleTrackId
        ?.takeIf { selected -> subtitleTracks.any { it.id == selected } }
        ?: subtitleTracks.firstOrNull()?.id
    val hasFocusableRows = subtitleTracks.isNotEmpty() || danmakuSettings != null

    LaunchedEffect(Unit) {
        if (hasFocusableRows) {
            repeat(2) { withFrameNanos { } }
            initialFocus.requestFocus()
        }
    }
    LaunchedEffect(restoreBlockRowFocus) {
        if (restoreBlockRowFocus) {
            withFrameNanos { }
            blockRowFocus.requestFocus()
            restoreBlockRowFocus = false
        }
    }
    BackHandler(enabled = !blockMenuOpen, onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(440.dp)
                .background(Panel.copy(alpha = 0.97f))
                .padding(horizontal = 28.dp, vertical = 34.dp),
        ) {
            Text(
                text = stringResource(R.string.player_settings_title),
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("player-settings-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (subtitleTracks.isNotEmpty()) {
                    item {
                        PlayerSettingsSectionHeader(
                            stringResource(R.string.subtitle_settings_title),
                        )
                    }
                    items(subtitleTracks, key = SubtitleTrack::id) { track ->
                        PlayerSettingsRow(
                            title = track.label,
                            value = track.language.orEmpty(),
                            selected = track.id == selectedSubtitleTrackId,
                            onClick = { onSelectSubtitleTrack(track.id) },
                            isFirstFocusable = track.id == subtitleTracks.first().id,
                            modifier = Modifier.initialFocusWhen(
                                condition = track.id == initialSubtitleTrackId,
                                requester = initialFocus,
                            ),
                        )
                    }
                    item {
                        PlayerSettingsChoiceRow(
                            title = stringResource(R.string.subtitle_display_mode),
                            values = SubtitleDisplayMode.entries,
                            selected = subtitleSettings.displayMode,
                            label = ::subtitleDisplayModeLabel,
                            onSelect = { value ->
                                onChangeSubtitleSettings(
                                    subtitleSettings.copy(displayMode = value),
                                )
                            },
                        )
                    }
                    item {
                        PlayerSettingsAdjustRow(
                            title = stringResource(R.string.subtitle_font_scale),
                            value = stringResource(
                                R.string.percentage_value,
                                subtitleSettings.fontScalePercent,
                            ),
                            onDecrease = {
                                onChangeSubtitleSettings(
                                    SubtitleSettingsPolicy.adjustFontScale(
                                        subtitleSettings,
                                        -1,
                                    ),
                                )
                            },
                            onIncrease = {
                                onChangeSubtitleSettings(
                                    SubtitleSettingsPolicy.adjustFontScale(
                                        subtitleSettings,
                                        1,
                                    ),
                                )
                            },
                        )
                    }
                    item {
                        PlayerSettingsAdjustRow(
                            title = stringResource(R.string.subtitle_vertical_position),
                            value = stringResource(
                                R.string.percentage_value,
                                subtitleSettings.verticalPositionPercent,
                            ),
                            onDecrease = {
                                onChangeSubtitleSettings(
                                    SubtitleSettingsPolicy.adjustVerticalPosition(
                                        subtitleSettings,
                                        -1,
                                    ),
                                )
                            },
                            onIncrease = {
                                onChangeSubtitleSettings(
                                    SubtitleSettingsPolicy.adjustVerticalPosition(
                                        subtitleSettings,
                                        1,
                                    ),
                                )
                            },
                        )
                    }
                    item {
                        PlayerSettingsAdjustRow(
                            title = stringResource(R.string.subtitle_time_offset),
                            value = formatSubtitleOffset(
                                subtitleSettings.timeOffsetSeconds,
                            ),
                            onDecrease = {
                                onChangeSubtitleSettings(
                                    SubtitleSettingsPolicy.adjustTimeOffset(
                                        subtitleSettings,
                                        -1,
                                    ),
                                )
                            },
                            onIncrease = {
                                onChangeSubtitleSettings(
                                    SubtitleSettingsPolicy.adjustTimeOffset(
                                        subtitleSettings,
                                        1,
                                    ),
                                )
                            },
                            onClick = {
                                onChangeSubtitleSettings(
                                    subtitleSettings.copy(timeOffsetSeconds = 0f),
                                )
                            },
                            isLastFocusable = danmakuSettings == null,
                        )
                    }
                }
                if (danmakuSettings != null) {
                    item {
                        PlayerSettingsSectionHeader(
                            stringResource(R.string.danmaku_settings_title),
                        )
                    }
                    item {
                        PlayerSettingsChoiceRow(
                            title = stringResource(R.string.danmaku_text_size),
                            values = DanmakuTextSize.entries,
                            selected = danmakuSettings.textSize,
                            label = ::danmakuTextSizeLabel,
                            onSelect = { value ->
                                onChangeDanmakuSettings(
                                    danmakuSettings.copy(textSize = value),
                                )
                            },
                            modifier = Modifier.initialFocusWhen(
                                condition = initialSubtitleTrackId == null,
                                requester = initialFocus,
                            ),
                            isFirstFocusable = subtitleTracks.isEmpty(),
                        )
                    }
                    item {
                        PlayerSettingsChoiceRow(
                            title = stringResource(R.string.danmaku_speed),
                            values = DanmakuSpeed.entries,
                            selected = danmakuSettings.speed,
                            label = ::danmakuSpeedLabel,
                            onSelect = { value ->
                                onChangeDanmakuSettings(
                                    danmakuSettings.copy(speed = value),
                                )
                            },
                        )
                    }
                    item {
                        PlayerSettingsChoiceRow(
                            title = stringResource(R.string.danmaku_opacity),
                            values = listOf(25, 50, 75, 100),
                            selected = danmakuSettings.opacityPercent,
                            label = { value ->
                                stringResource(R.string.percentage_value, value)
                            },
                            onSelect = { value ->
                                onChangeDanmakuSettings(
                                    danmakuSettings.copy(opacityPercent = value),
                                )
                            },
                        )
                    }
                    item {
                        PlayerSettingsChoiceRow(
                            title = stringResource(R.string.danmaku_display_area),
                            values = listOf(25, 50, 75, 100),
                            selected = danmakuSettings.displayAreaPercent,
                            label = { value ->
                                stringResource(R.string.percentage_value, value)
                            },
                            onSelect = { value ->
                                onChangeDanmakuSettings(
                                    danmakuSettings.copy(displayAreaPercent = value),
                                )
                            },
                        )
                    }
                    item {
                        PlayerSettingsRow(
                            title = stringResource(R.string.danmaku_block_types),
                            value = blockSummary(danmakuSettings),
                            selected = false,
                            onClick = { blockMenuOpen = true },
                            isLastFocusable = true,
                            modifier = Modifier
                                .focusRequester(blockRowFocus)
                                .testTag("player-settings-block-types"),
                        )
                    }
                }
            }
        }
    }

    if (blockMenuOpen && danmakuSettings != null) {
        PlayerDanmakuBlockMenu(
            settings = danmakuSettings,
            onChange = onChangeDanmakuSettings,
            onDismiss = {
                blockMenuOpen = false
                restoreBlockRowFocus = true
            },
        )
    }
}

@Composable
private fun PlayerSettingsRow(
    title: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFirstFocusable: Boolean = false,
    isLastFocusable: Boolean = false,
) {
    KaloscopeButton(
        onClick = onClick,
        selected = selected,
        size = KaloscopeControlSize.Row,
        modifier = modifier
            .fillMaxWidth()
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
                if (isFirstFocusable) {
                    up = FocusRequester.Cancel
                }
                if (isLastFocusable) {
                    down = FocusRequester.Cancel
                }
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (value.isNotBlank()) {
                Text(value, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun <T> PlayerSettingsChoiceRow(
    title: String,
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isFirstFocusable: Boolean = false,
    isLastFocusable: Boolean = false,
) {
    val index = values.indexOf(selected).coerceAtLeast(0)
    fun selectOffset(offset: Int) {
        values.getOrNull((index + offset).coerceIn(0, values.lastIndex))
            ?.let(onSelect)
    }
    PlayerSettingsRow(
        title = title,
        value = "‹  ${label(selected)}  ›",
        selected = false,
        onClick = { selectOffset(1) },
        isFirstFocusable = isFirstFocusable,
        isLastFocusable = isLastFocusable,
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
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
private fun PlayerSettingsAdjustRow(
    title: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isFirstFocusable: Boolean = false,
    isLastFocusable: Boolean = false,
) {
    PlayerSettingsRow(
        title = title,
        value = "‹  $value  ›",
        selected = false,
        onClick = onClick,
        isFirstFocusable = isFirstFocusable,
        isLastFocusable = isLastFocusable,
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> {
                    onDecrease()
                    true
                }
                Key.DirectionRight -> {
                    onIncrease()
                    true
                }
                else -> false
            }
        },
    )
}

@Composable
private fun PlayerSettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Muted,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun blockSummary(settings: DanmakuSettings): String {
    val selected = PlayerDanmakuBlockPolicy.selected(settings)
    return if (selected.isEmpty()) {
        stringResource(R.string.danmaku_block_none)
    } else {
        selected.map { playerDanmakuBlockLabel(it) }.joinToString("、")
    }
}

private fun Modifier.initialFocusWhen(
    condition: Boolean,
    requester: FocusRequester,
): Modifier = if (condition) focusRequester(requester) else this
