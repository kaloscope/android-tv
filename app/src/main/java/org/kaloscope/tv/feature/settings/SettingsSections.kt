package org.kaloscope.tv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleSettingsPolicy
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeResolution

@Composable
internal fun PlaybackSettings(
    state: SettingsUiState.Content,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onPlaybackMode: (PlaybackMode) -> Unit,
    onTranscodeResolution: (TranscodeResolution) -> Unit,
    onAutoplayNext: (Boolean) -> Unit,
) {
    ChoiceSettingRow(
        title = stringResource(R.string.default_playback_mode),
        description = stringResource(R.string.default_playback_mode_description),
        value = playbackModeLabel(state.settings.playbackMode),
        interactionsEnabled = interactionsEnabled,
        createChoice = {
            SettingsChoice(
                title = stringResource(R.string.default_playback_mode),
                options = PlaybackMode.entries.map { mode ->
                    SettingsChoiceOption(
                        label = playbackModeLabel(mode),
                        selected = mode == state.settings.playbackMode,
                        onSelect = { onPlaybackMode(mode) },
                    )
                },
            )
        },
        onOpenChoice = onOpenChoice,
    )
    Spacer(Modifier.height(10.dp))
    ChoiceSettingRow(
        title = stringResource(R.string.default_transcode_resolution),
        description = stringResource(R.string.default_transcode_resolution_description),
        value = resolutionLabel(state.settings.transcodeResolution),
        interactionsEnabled = interactionsEnabled,
        createChoice = {
            SettingsChoice(
                title = stringResource(R.string.default_transcode_resolution),
                options = TranscodeResolution.entries.map { resolution ->
                    SettingsChoiceOption(
                        label = resolutionLabel(resolution),
                        selected = resolution == state.settings.transcodeResolution,
                        onSelect = { onTranscodeResolution(resolution) },
                    )
                },
            )
        },
        onOpenChoice = onOpenChoice,
    )
    Spacer(Modifier.height(10.dp))
    ToggleSettingRow(
        title = stringResource(R.string.autoplay_next),
        description = stringResource(R.string.autoplay_next_description),
        checked = state.settings.autoplayNext,
        interactionsEnabled = interactionsEnabled,
        onToggle = { onAutoplayNext(!state.settings.autoplayNext) },
    )
}

@Composable
internal fun SubtitleDefaultSettings(
    settings: SubtitleSettings,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onOpenLanguage: (FocusRequester) -> Unit,
    onChange: (SubtitleSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subtitle-default-settings"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ToggleSettingRow(
                title = stringResource(R.string.default_subtitle),
                description = stringResource(R.string.default_subtitle_description),
                checked = settings.enabled,
                interactionsEnabled = interactionsEnabled,
                onToggle = { onChange(settings.copy(enabled = !settings.enabled)) },
            )
        }
        item {
            SubtitleLanguageSettingRow(
                value = settings.languagePreference,
                interactionsEnabled = interactionsEnabled,
                onOpen = onOpenLanguage,
            )
        }
        item {
            ChoiceSettingRow(
                title = stringResource(R.string.subtitle_display_mode),
                description = stringResource(R.string.subtitle_display_mode_description),
                value = subtitleDisplayModeLabel(settings.displayMode),
                interactionsEnabled = interactionsEnabled,
                createChoice = {
                    SettingsChoice(
                        title = stringResource(R.string.subtitle_display_mode),
                        options = SubtitleDisplayMode.entries.map { mode ->
                            SettingsChoiceOption(
                                label = subtitleDisplayModeLabel(mode),
                                selected = mode == settings.displayMode,
                                onSelect = { onChange(settings.copy(displayMode = mode)) },
                            )
                        },
                    )
                },
                onOpenChoice = onOpenChoice,
            )
        }
        item {
            AdjustableSettingRow(
                title = stringResource(R.string.subtitle_font_scale),
                description = stringResource(R.string.subtitle_font_scale_description),
                value = stringResource(R.string.percentage_value, settings.fontScalePercent),
                interactionsEnabled = interactionsEnabled,
                onDecrease = {
                    onChange(SubtitleSettingsPolicy.adjustFontScale(settings, -1))
                },
                onIncrease = {
                    onChange(SubtitleSettingsPolicy.adjustFontScale(settings, 1))
                },
            )
        }
        item {
            AdjustableSettingRow(
                title = stringResource(R.string.subtitle_vertical_position),
                description = stringResource(R.string.subtitle_vertical_position_description),
                value = stringResource(
                    R.string.percentage_value,
                    settings.verticalPositionPercent,
                ),
                interactionsEnabled = interactionsEnabled,
                onDecrease = {
                    onChange(SubtitleSettingsPolicy.adjustVerticalPosition(settings, -1))
                },
                onIncrease = {
                    onChange(SubtitleSettingsPolicy.adjustVerticalPosition(settings, 1))
                },
            )
        }
        item {
            AdjustableSettingRow(
                title = stringResource(R.string.subtitle_time_offset),
                description = stringResource(R.string.subtitle_time_offset_description),
                value = formatSubtitleOffset(settings.timeOffsetSeconds),
                interactionsEnabled = interactionsEnabled,
                onDecrease = {
                    onChange(SubtitleSettingsPolicy.adjustTimeOffset(settings, -1))
                },
                onIncrease = {
                    onChange(SubtitleSettingsPolicy.adjustTimeOffset(settings, 1))
                },
                onClick = {
                    onChange(settings.copy(timeOffsetSeconds = 0f))
                },
            )
        }
    }
}

@Composable
private fun SubtitleLanguageSettingRow(
    value: String,
    interactionsEnabled: Boolean,
    onOpen: (FocusRequester) -> Unit,
) {
    val focus = remember { FocusRequester() }
    KaloscopeButton(
        onClick = {
            if (interactionsEnabled) {
                onOpen(focus)
            }
        },
        size = KaloscopeControlSize.Row,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
    ) {
        SettingRowContent(
            title = stringResource(R.string.subtitle_language_preference),
            description = stringResource(R.string.subtitle_language_preference_description),
            value = value.ifBlank {
                stringResource(R.string.subtitle_language_preference_any)
            } + "  ›",
        )
    }
}

@Composable
private fun AdjustableSettingRow(
    title: String,
    description: String,
    value: String,
    interactionsEnabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onClick: () -> Unit = {},
) {
    KaloscopeButton(
        onClick = {
            if (interactionsEnabled) {
                onClick()
            }
        },
        size = KaloscopeControlSize.Row,
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (interactionsEnabled) {
                            onDecrease()
                        }
                        true
                    }

                    Key.DirectionRight -> {
                        if (interactionsEnabled) {
                            onIncrease()
                        }
                        true
                    }

                    else -> false
                }
            },
    ) {
        SettingRowContent(title, description, "‹  $value  ›")
    }
}

@Composable
internal fun DanmakuDefaultSettings(
    settings: DanmakuSettings,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onChange: (DanmakuSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val percentages = listOf(25, 50, 75, 100)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ToggleSettingRow(
                title = stringResource(R.string.default_danmaku),
                description = stringResource(R.string.default_danmaku_description),
                checked = settings.enabled,
                interactionsEnabled = interactionsEnabled,
                onToggle = { onChange(settings.copy(enabled = !settings.enabled)) },
            )
        }
        item {
            ChoiceSettingRow(
                title = stringResource(R.string.danmaku_text_size),
                description = stringResource(R.string.danmaku_text_size_description),
                value = danmakuTextSizeLabel(settings.textSize),
                interactionsEnabled = interactionsEnabled,
                createChoice = {
                    SettingsChoice(
                        title = stringResource(R.string.danmaku_text_size),
                        options = DanmakuTextSize.entries.map { size ->
                            SettingsChoiceOption(
                                label = danmakuTextSizeLabel(size),
                                selected = size == settings.textSize,
                                onSelect = { onChange(settings.copy(textSize = size)) },
                            )
                        },
                    )
                },
                onOpenChoice = onOpenChoice,
            )
        }
        item {
            ChoiceSettingRow(
                title = stringResource(R.string.danmaku_speed),
                description = stringResource(R.string.danmaku_speed_description),
                value = danmakuSpeedLabel(settings.speed),
                interactionsEnabled = interactionsEnabled,
                createChoice = {
                    SettingsChoice(
                        title = stringResource(R.string.danmaku_speed),
                        options = DanmakuSpeed.entries.map { speed ->
                            SettingsChoiceOption(
                                label = danmakuSpeedLabel(speed),
                                selected = speed == settings.speed,
                                onSelect = { onChange(settings.copy(speed = speed)) },
                            )
                        },
                    )
                },
                onOpenChoice = onOpenChoice,
            )
        }
        item {
            DanmakuPercentageSetting(
                title = stringResource(R.string.danmaku_opacity),
                description = stringResource(R.string.danmaku_opacity_description),
                value = settings.opacityPercent,
                percentages = percentages,
                interactionsEnabled = interactionsEnabled,
                onOpenChoice = onOpenChoice,
                onSelect = { onChange(settings.copy(opacityPercent = it)) },
            )
        }
        item {
            DanmakuPercentageSetting(
                title = stringResource(R.string.danmaku_display_area),
                description = stringResource(R.string.danmaku_display_area_description),
                value = settings.displayAreaPercent,
                percentages = percentages,
                interactionsEnabled = interactionsEnabled,
                onOpenChoice = onOpenChoice,
                onSelect = { onChange(settings.copy(displayAreaPercent = it)) },
            )
        }
        DanmakuDisplayMode.entries.forEach { mode ->
            item {
                ToggleSettingRow(
                    title = danmakuModeLabel(mode),
                    description = danmakuModeDescription(mode),
                    checked = mode in settings.visibleModes,
                    interactionsEnabled = interactionsEnabled,
                    onToggle = {
                        val visibleModes = if (mode in settings.visibleModes) {
                            settings.visibleModes - mode
                        } else {
                            settings.visibleModes + mode
                        }
                        onChange(settings.copy(visibleModes = visibleModes))
                    },
                )
            }
        }
    }
}

@Composable
private fun DanmakuPercentageSetting(
    title: String,
    description: String,
    value: Int,
    percentages: List<Int>,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onSelect: (Int) -> Unit,
) {
    ChoiceSettingRow(
        title = title,
        description = description,
        value = stringResource(R.string.percentage_value, value),
        interactionsEnabled = interactionsEnabled,
        createChoice = {
            SettingsChoice(
                title = title,
                options = percentages.map { percentage ->
                    SettingsChoiceOption(
                        label = stringResource(R.string.percentage_value, percentage),
                        selected = percentage == value,
                        onSelect = { onSelect(percentage) },
                    )
                },
            )
        },
        onOpenChoice = onOpenChoice,
    )
}

@Composable
internal fun BehaviorSettings(
    state: SettingsUiState.Content,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onStartPage: (StartPage) -> Unit,
) {
    ChoiceSettingRow(
        title = stringResource(R.string.default_start_page),
        description = stringResource(R.string.default_start_page_description),
        value = startPageLabel(state.settings.startPage),
        interactionsEnabled = interactionsEnabled,
        createChoice = {
            SettingsChoice(
                title = stringResource(R.string.default_start_page),
                options = StartPage.entries.map { page ->
                    SettingsChoiceOption(
                        label = startPageLabel(page),
                        selected = page == state.settings.startPage,
                        onSelect = { onStartPage(page) },
                    )
                },
            )
        },
        onOpenChoice = onOpenChoice,
    )
}

@Composable
internal fun ServerAccountSettings(
    session: Session,
    connection: SettingsConnection,
    interactionsEnabled: Boolean,
    onTestConnection: () -> Unit,
    onManageServers: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
    ) {
        item {
            SettingValue(
                label = stringResource(R.string.current_server),
                value = session.server.name,
                detail = session.server.origin,
            )
        }
        item { Spacer(Modifier.height(14.dp)) }
        item {
            SettingValue(
                label = stringResource(R.string.current_account),
                value = session.user.username,
                detail = session.user.role,
            )
        }
        item { Spacer(Modifier.height(18.dp)) }
        item {
            SettingActionRow(
                title = stringResource(R.string.test_connection),
                description = connectionDescription(connection),
                interactionsEnabled = interactionsEnabled &&
                    connection != SettingsConnection.Testing,
                danger = false,
                onClick = onTestConnection,
            )
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            SettingActionRow(
                title = stringResource(R.string.manage_servers),
                description = stringResource(R.string.manage_servers_description),
                interactionsEnabled = interactionsEnabled,
                danger = false,
                onClick = onManageServers,
            )
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            SettingActionRow(
                title = stringResource(R.string.logout),
                description = stringResource(R.string.logout_description),
                interactionsEnabled = interactionsEnabled,
                danger = true,
                onClick = onLogout,
            )
        }
    }
}
