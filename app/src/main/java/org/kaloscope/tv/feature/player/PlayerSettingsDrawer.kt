package org.kaloscope.tv.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelActionRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelAdjustmentRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSectionHeader
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSelectionRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelSessionHint
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.danmakuBlockSummary
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
    val listState = rememberLazyListState()
    var blockMenuOpen by remember { mutableStateOf(false) }
    var restoreBlockRowFocus by remember { mutableStateOf(false) }
    val initialSubtitleTrackId = selectedSubtitleTrackId
        ?.takeIf { selected -> subtitleTracks.any { it.id == selected } }
        ?: subtitleTracks.firstOrNull()?.id
    val hasFocusableRows = subtitleTracks.isNotEmpty() || danmakuSettings != null
    val initialListIndex = when {
        initialSubtitleTrackId != null -> {
            subtitleTracks.indexOfFirst { it.id == initialSubtitleTrackId } + 1
        }

        danmakuSettings != null -> 1
        else -> null
    }
    val palette = KaloscopeSidePanelPalette(
        panelColor = Panel,
        textColor = OnBackground,
        mutedColor = Muted,
    )

    LaunchedEffect(initialListIndex) {
        if (hasFocusableRows && initialListIndex != null) {
            listState.scrollToItem(initialListIndex)
            withFrameNanos { }
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

    KaloscopeSidePanel(
        title = stringResource(R.string.player_settings_title),
        palette = palette,
        onDismiss = onDismiss,
        dismissEnabled = !blockMenuOpen,
        modifier = Modifier.testTag("player-settings-drawer"),
        footer = {
            KaloscopeSidePanelSessionHint(
                text = stringResource(R.string.player_session_settings_description),
                color = Muted,
                iconTestTag = "player-session-settings-hint-icon",
                textTestTag = "player-session-settings-hint-text",
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("player-settings-list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (subtitleTracks.isNotEmpty()) {
                item {
                    KaloscopeSidePanelSectionHeader(
                        title = stringResource(R.string.subtitle_settings_title),
                        color = Muted,
                    )
                }
                items(subtitleTracks, key = SubtitleTrack::id) { track ->
                    KaloscopeSidePanelSelectionRow(
                        title = track.label,
                        value = track.language.orEmpty(),
                        selected = track.id == selectedSubtitleTrackId,
                        onClick = { onSelectSubtitleTrack(track.id) },
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
                        modifier = Modifier.testTag("player-subtitle-display-mode-row"),
                        adjustmentTestTagPrefix = "player-subtitle-display-mode",
                    )
                }
                item {
                    val decreased = SubtitleSettingsPolicy.adjustFontScale(
                        subtitleSettings,
                        -1,
                    )
                    val increased = SubtitleSettingsPolicy.adjustFontScale(
                        subtitleSettings,
                        1,
                    )
                    KaloscopeSidePanelAdjustmentRow(
                        title = stringResource(R.string.subtitle_font_scale),
                        value = stringResource(
                            R.string.percentage_value,
                            subtitleSettings.fontScalePercent,
                        ),
                        canDecrease = decreased != subtitleSettings,
                        canIncrease = increased != subtitleSettings,
                        onDecrease = { onChangeSubtitleSettings(decreased) },
                        onIncrease = { onChangeSubtitleSettings(increased) },
                        modifier = Modifier.testTag("player-subtitle-font-scale-row"),
                        adjustmentTestTagPrefix = "player-subtitle-font-scale",
                    )
                }
                item {
                    val decreased = SubtitleSettingsPolicy.adjustVerticalPosition(
                        subtitleSettings,
                        -1,
                    )
                    val increased = SubtitleSettingsPolicy.adjustVerticalPosition(
                        subtitleSettings,
                        1,
                    )
                    KaloscopeSidePanelAdjustmentRow(
                        title = stringResource(R.string.subtitle_vertical_position),
                        value = stringResource(
                            R.string.percentage_value,
                            subtitleSettings.verticalPositionPercent,
                        ),
                        canDecrease = decreased != subtitleSettings,
                        canIncrease = increased != subtitleSettings,
                        onDecrease = { onChangeSubtitleSettings(decreased) },
                        onIncrease = { onChangeSubtitleSettings(increased) },
                        modifier = Modifier.testTag("player-subtitle-position-row"),
                        adjustmentTestTagPrefix = "player-subtitle-position",
                    )
                }
                item {
                    val decreased = SubtitleSettingsPolicy.adjustTimeOffset(
                        subtitleSettings,
                        -1,
                    )
                    val increased = SubtitleSettingsPolicy.adjustTimeOffset(
                        subtitleSettings,
                        1,
                    )
                    KaloscopeSidePanelAdjustmentRow(
                        title = stringResource(R.string.subtitle_time_offset),
                        value = formatSubtitleOffset(subtitleSettings.timeOffsetSeconds),
                        canDecrease = decreased != subtitleSettings,
                        canIncrease = increased != subtitleSettings,
                        onDecrease = { onChangeSubtitleSettings(decreased) },
                        onIncrease = { onChangeSubtitleSettings(increased) },
                        modifier = Modifier.testTag("player-subtitle-offset-row"),
                        adjustmentTestTagPrefix = "player-subtitle-offset",
                    )
                }
                item {
                    KaloscopeSidePanelActionRow(
                        title = stringResource(R.string.subtitle_time_offset_reset),
                        onClick = {
                            onChangeSubtitleSettings(
                                subtitleSettings.copy(timeOffsetSeconds = 0f),
                            )
                        },
                        modifier = Modifier.testTag("player-subtitle-offset-reset"),
                    )
                }
            }
            if (danmakuSettings != null) {
                item {
                    KaloscopeSidePanelSectionHeader(
                        title = stringResource(R.string.danmaku_settings_title),
                        color = Muted,
                    )
                }
                item {
                    PlayerSettingsChoiceRow(
                        title = stringResource(R.string.danmaku_text_size),
                        values = DanmakuTextSize.entries,
                        selected = danmakuSettings.textSize,
                        label = ::danmakuTextSizeLabel,
                        onSelect = { value ->
                            onChangeDanmakuSettings(danmakuSettings.copy(textSize = value))
                        },
                        modifier = Modifier
                            .initialFocusWhen(
                                condition = initialSubtitleTrackId == null,
                                requester = initialFocus,
                            )
                            .testTag("player-danmaku-text-size-row"),
                        adjustmentTestTagPrefix = "player-danmaku-text-size",
                    )
                }
                item {
                    PlayerSettingsChoiceRow(
                        title = stringResource(R.string.danmaku_speed),
                        values = DanmakuSpeed.entries,
                        selected = danmakuSettings.speed,
                        label = ::danmakuSpeedLabel,
                        onSelect = { value ->
                            onChangeDanmakuSettings(danmakuSettings.copy(speed = value))
                        },
                        modifier = Modifier.testTag("player-danmaku-speed-row"),
                        adjustmentTestTagPrefix = "player-danmaku-speed",
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
                        modifier = Modifier.testTag("player-danmaku-opacity-row"),
                        adjustmentTestTagPrefix = "player-danmaku-opacity",
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
                        modifier = Modifier.testTag("player-danmaku-display-area-row"),
                        adjustmentTestTagPrefix = "player-danmaku-display-area",
                    )
                }
                item {
                    KaloscopeSidePanelSelectionRow(
                        title = stringResource(R.string.danmaku_block_types),
                        value = danmakuBlockSummary(danmakuSettings),
                        onClick = { blockMenuOpen = true },
                        modifier = Modifier
                            .focusRequester(blockRowFocus)
                            .testTag("player-settings-block-types"),
                    )
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
private fun <T> PlayerSettingsChoiceRow(
    title: String,
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    adjustmentTestTagPrefix: String? = null,
) {
    val selectedIndex = values.indexOf(selected).coerceAtLeast(0)
    KaloscopeSidePanelAdjustmentRow(
        title = title,
        value = label(selected),
        canDecrease = selectedIndex > 0,
        canIncrease = selectedIndex < values.lastIndex,
        onDecrease = {
            values.getOrNull(selectedIndex - 1)?.let(onSelect)
        },
        onIncrease = {
            values.getOrNull(selectedIndex + 1)?.let(onSelect)
        },
        modifier = modifier,
        adjustmentTestTagPrefix = adjustmentTestTagPrefix,
    )
}

private fun Modifier.initialFocusWhen(
    condition: Boolean,
    requester: FocusRequester,
): Modifier = if (condition) focusRequester(requester) else this
