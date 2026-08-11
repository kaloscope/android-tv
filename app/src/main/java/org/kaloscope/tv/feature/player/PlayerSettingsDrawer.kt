package org.kaloscope.tv.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialog
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialogOption
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelActionRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelAdjustmentRow
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelChoiceRow
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
import org.kaloscope.tv.core.model.DanmakuSettingsPolicy
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
    var activeChoice by remember { mutableStateOf<PlayerSettingsChoice?>(null) }
    var choiceTrigger by remember { mutableStateOf<FocusRequester?>(null) }
    var focusToRestore by remember { mutableStateOf<FocusRequester?>(null) }
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
    LaunchedEffect(focusToRestore) {
        val requester = focusToRestore ?: return@LaunchedEffect
        withFrameNanos { }
        requester.requestFocus()
        focusToRestore = null
    }

    fun dismissChoice() {
        activeChoice = null
        focusToRestore = choiceTrigger
        choiceTrigger = null
    }

    fun openChoice(trigger: FocusRequester, choice: PlayerSettingsChoice) {
        choiceTrigger = trigger
        activeChoice = choice
    }

    KaloscopeSidePanel(
        title = stringResource(R.string.player_settings_title),
        palette = palette,
        onDismiss = onDismiss,
        dismissEnabled = !blockMenuOpen && activeChoice == null,
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
                        onOpenChoice = ::openChoice,
                        optionTestTag = {
                            "player-subtitle-display-mode-option-${it.name.lowercase()}"
                        },
                        modifier = Modifier.testTag("player-subtitle-display-mode-row"),
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
                        onOpenChoice = ::openChoice,
                        optionTestTag = {
                            "player-danmaku-text-size-option-${it.name.lowercase()}"
                        },
                        focusRequester = if (initialSubtitleTrackId == null) {
                            initialFocus
                        } else {
                            null
                        },
                        modifier = Modifier.testTag("player-danmaku-text-size-row"),
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
                        onOpenChoice = ::openChoice,
                        optionTestTag = {
                            "player-danmaku-speed-option-${it.name.lowercase()}"
                        },
                        modifier = Modifier.testTag("player-danmaku-speed-row"),
                    )
                }
                item {
                    val decreased = DanmakuSettingsPolicy.adjustOpacity(
                        danmakuSettings,
                        -1,
                    )
                    val increased = DanmakuSettingsPolicy.adjustOpacity(
                        danmakuSettings,
                        1,
                    )
                    KaloscopeSidePanelAdjustmentRow(
                        title = stringResource(R.string.danmaku_opacity),
                        value = stringResource(
                            R.string.percentage_value,
                            danmakuSettings.opacityPercent,
                        ),
                        canDecrease = decreased != danmakuSettings,
                        canIncrease = increased != danmakuSettings,
                        onDecrease = { onChangeDanmakuSettings(decreased) },
                        onIncrease = { onChangeDanmakuSettings(increased) },
                        modifier = Modifier.testTag("player-danmaku-opacity-row"),
                        adjustmentTestTagPrefix = "player-danmaku-opacity",
                    )
                }
                item {
                    val decreased = DanmakuSettingsPolicy.adjustDisplayArea(
                        danmakuSettings,
                        -1,
                    )
                    val increased = DanmakuSettingsPolicy.adjustDisplayArea(
                        danmakuSettings,
                        1,
                    )
                    KaloscopeSidePanelAdjustmentRow(
                        title = stringResource(R.string.danmaku_display_area),
                        value = stringResource(
                            R.string.percentage_value,
                            danmakuSettings.displayAreaPercent,
                        ),
                        canDecrease = decreased != danmakuSettings,
                        canIncrease = increased != danmakuSettings,
                        onDecrease = { onChangeDanmakuSettings(decreased) },
                        onIncrease = { onChangeDanmakuSettings(increased) },
                        modifier = Modifier.testTag("player-danmaku-display-area-row"),
                        adjustmentTestTagPrefix = "player-danmaku-display-area",
                    )
                }
                item {
                    KaloscopeSidePanelChoiceRow(
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
    activeChoice?.let { choice ->
        PlayerSettingsChoiceMenu(
            choice = choice,
            onDismiss = ::dismissChoice,
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
    onOpenChoice: (FocusRequester, PlayerSettingsChoice) -> Unit,
    modifier: Modifier = Modifier,
    optionTestTag: ((T) -> String)? = null,
    focusRequester: FocusRequester? = null,
) {
    val internalFocus = remember { FocusRequester() }
    val rowFocus = focusRequester ?: internalFocus
    val choice = PlayerSettingsChoice(
        title = title,
        options = values.map { option ->
            KaloscopeChoiceDialogOption(
                label = label(option),
                selected = { option == selected },
                testTag = optionTestTag?.invoke(option),
                onSelect = { onSelect(option) },
            )
        },
    )
    KaloscopeSidePanelChoiceRow(
        title = title,
        value = label(selected),
        onClick = { onOpenChoice(rowFocus, choice) },
        modifier = modifier.focusRequester(rowFocus),
    )
}

@Composable
private fun PlayerSettingsChoiceMenu(
    choice: PlayerSettingsChoice,
    onDismiss: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        KaloscopeChoiceDialog(
            title = choice.title,
            options = choice.options,
            viewportSize = DpSize(maxWidth, maxHeight),
            onDismiss = onDismiss,
        )
    }
}

private data class PlayerSettingsChoice(
    val title: String,
    val options: List<KaloscopeChoiceDialogOption>,
)

private fun Modifier.initialFocusWhen(
    condition: Boolean,
    requester: FocusRequester,
): Modifier = if (condition) focusRequester(requester) else this
