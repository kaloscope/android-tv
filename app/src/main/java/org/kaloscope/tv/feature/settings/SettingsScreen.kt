package org.kaloscope.tv.feature.settings

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import java.util.Locale
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.PanelSelected
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeResolution

@Composable
fun SettingsScreen(
    session: Session,
    state: SettingsUiState,
    onRetry: () -> Unit,
    onSelectSection: (SettingsSection) -> Unit,
    onPlaybackMode: (PlaybackMode) -> Unit,
    onTranscodeResolution: (TranscodeResolution) -> Unit,
    onAutoplayNext: (Boolean) -> Unit,
    onDanmakuSettings: (DanmakuSettings) -> Unit,
    onSubtitleSettings: (SubtitleSettings) -> Unit,
    onStartPage: (StartPage) -> Unit,
    onTestConnection: () -> Unit,
    onManageServers: () -> Unit,
    onLogout: () -> Unit,
) {
    when (state) {
        SettingsUiState.Loading -> SettingsStatus(
            title = stringResource(R.string.loading_settings),
            description = stringResource(R.string.loading_settings_description),
        )

        is SettingsUiState.Error -> SettingsStatus(
            title = stringResource(R.string.settings_load_failed),
            description = settingsErrorText(state.error),
            onRetry = onRetry,
        )

        is SettingsUiState.Content -> SettingsContent(
            session = session,
            state = state,
            onSelectSection = onSelectSection,
            onPlaybackMode = onPlaybackMode,
            onTranscodeResolution = onTranscodeResolution,
            onAutoplayNext = onAutoplayNext,
            onDanmakuSettings = onDanmakuSettings,
            onSubtitleSettings = onSubtitleSettings,
            onStartPage = onStartPage,
            onTestConnection = onTestConnection,
            onManageServers = onManageServers,
            onLogout = onLogout,
        )
    }
}

@Composable
private fun SettingsContent(
    session: Session,
    state: SettingsUiState.Content,
    onSelectSection: (SettingsSection) -> Unit,
    onPlaybackMode: (PlaybackMode) -> Unit,
    onTranscodeResolution: (TranscodeResolution) -> Unit,
    onAutoplayNext: (Boolean) -> Unit,
    onDanmakuSettings: (DanmakuSettings) -> Unit,
    onSubtitleSettings: (SubtitleSettings) -> Unit,
    onStartPage: (StartPage) -> Unit,
    onTestConnection: () -> Unit,
    onManageServers: () -> Unit,
    onLogout: () -> Unit,
) {
    var choice by remember { mutableStateOf<SettingsChoice?>(null) }
    var languageEditorOpen by remember { mutableStateOf(false) }
    var restoreFocus by remember { mutableStateOf<FocusRequester?>(null) }
    val selectedSectionFocus = remember { FocusRequester() }
    val controlsEnabled = choice == null && !languageEditorOpen && !state.isSaving

    LaunchedEffect(Unit) {
        selectedSectionFocus.requestFocus()
    }
    LaunchedEffect(choice, languageEditorOpen, restoreFocus) {
        if (choice == null && !languageEditorOpen) {
            restoreFocus?.let { requester ->
                withFrameNanos { }
                requester.requestFocus()
                restoreFocus = null
            }
        }
    }
    BackHandler(enabled = choice != null) {
        choice = null
    }
    BackHandler(enabled = languageEditorOpen) {
        languageEditorOpen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.settings),
                color = OnBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.settings_device_only),
                color = Muted,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SettingsMenu(
                    selected = state.section,
                    enabled = controlsEnabled,
                    selectedFocus = selectedSectionFocus,
                    onSelect = onSelectSection,
                )
                SettingsPanel(
                    session = session,
                    state = state,
                    enabled = controlsEnabled,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 720.dp),
                    onOpenChoice = { focus, requestedChoice ->
                        restoreFocus = focus
                        choice = requestedChoice
                    },
                    onAutoplayNext = onAutoplayNext,
                    onDanmakuSettings = onDanmakuSettings,
                    onSubtitleSettings = onSubtitleSettings,
                    onOpenSubtitleLanguage = { focus ->
                        restoreFocus = focus
                        languageEditorOpen = true
                    },
                    onTestConnection = onTestConnection,
                    onManageServers = onManageServers,
                    onLogout = onLogout,
                    onPlaybackMode = onPlaybackMode,
                    onTranscodeResolution = onTranscodeResolution,
                    onStartPage = onStartPage,
                )
            }
        }
        choice?.let { current ->
            SettingsChoiceDialog(
                choice = current,
                onDismiss = { choice = null },
                onSelect = { option ->
                    choice = null
                    option.onSelect()
                },
            )
        }
        if (languageEditorOpen) {
            SubtitleLanguageDialog(
                initialValue = state.settings.subtitle.languagePreference,
                onDismiss = { languageEditorOpen = false },
                onSave = { language ->
                    languageEditorOpen = false
                    onSubtitleSettings(
                        state.settings.subtitle.copy(
                            languagePreference = language.trim(),
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun SettingsMenu(
    selected: SettingsSection,
    enabled: Boolean,
    selectedFocus: FocusRequester,
    onSelect: (SettingsSection) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(210.dp)
            .fillMaxHeight()
            .background(Panel.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsSection.entries.forEach { section ->
            Button(
                onClick = { onSelect(section) },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (section == selected) {
                            Modifier.focusRequester(selectedFocus)
                        } else {
                            Modifier
                        },
                    ),
                colors = ButtonDefaults.colors(
                    containerColor = if (selected == section) {
                        PanelSelected
                    } else {
                        Color.Transparent
                    },
                    focusedContainerColor = Primary,
                ),
            ) {
                Text(sectionLabel(section))
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    session: Session,
    state: SettingsUiState.Content,
    enabled: Boolean,
    modifier: Modifier,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onAutoplayNext: (Boolean) -> Unit,
    onDanmakuSettings: (DanmakuSettings) -> Unit,
    onSubtitleSettings: (SubtitleSettings) -> Unit,
    onOpenSubtitleLanguage: (FocusRequester) -> Unit,
    onTestConnection: () -> Unit,
    onManageServers: () -> Unit,
    onLogout: () -> Unit,
    onPlaybackMode: (PlaybackMode) -> Unit,
    onTranscodeResolution: (TranscodeResolution) -> Unit,
    onStartPage: (StartPage) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Panel.copy(alpha = 0.82f), RoundedCornerShape(22.dp))
            .testTag("settings-panel")
            .padding(horizontal = 28.dp, vertical = 26.dp),
    ) {
        Text(
            text = sectionTitle(state.section),
            color = OnBackground,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = sectionDescription(state.section),
            color = Muted,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(20.dp))
        when (state.section) {
            SettingsSection.Playback -> PlaybackSettings(
                state = state,
                enabled = enabled,
                onOpenChoice = onOpenChoice,
                onPlaybackMode = onPlaybackMode,
                onTranscodeResolution = onTranscodeResolution,
                onAutoplayNext = onAutoplayNext,
            )

            SettingsSection.Danmaku -> DanmakuDefaultSettings(
                settings = state.settings.danmaku,
                enabled = enabled,
                onOpenChoice = onOpenChoice,
                onChange = onDanmakuSettings,
                modifier = Modifier.weight(1f),
            )

            SettingsSection.Subtitle -> SubtitleDefaultSettings(
                settings = state.settings.subtitle,
                enabled = enabled,
                onOpenChoice = onOpenChoice,
                onOpenLanguage = onOpenSubtitleLanguage,
                onChange = onSubtitleSettings,
                modifier = Modifier.weight(1f),
            )

            SettingsSection.Behavior -> BehaviorSettings(
                state = state,
                enabled = enabled,
                onOpenChoice = onOpenChoice,
                onStartPage = onStartPage,
            )

            SettingsSection.ServerAccount -> ServerAccountSettings(
                session = session,
                connection = state.connection,
                enabled = enabled,
                onTestConnection = onTestConnection,
                onManageServers = onManageServers,
                onLogout = onLogout,
            )
        }
        state.saveError?.let {
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.settings_save_failed),
                color = Danger,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
internal fun ChoiceSettingRow(
    title: String,
    description: String,
    value: String,
    enabled: Boolean,
    createChoice: @Composable () -> SettingsChoice,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
) {
    val focus = remember { FocusRequester() }
    val choice = createChoice()
    Button(
        onClick = { onOpenChoice(focus, choice) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
        colors = settingRowColors(),
    ) {
        SettingRowContent(title, description, "$value  ›")
    }
}

@Composable
internal fun ToggleSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Button(
        onClick = onToggle,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = settingRowColors(),
    ) {
        SettingRowContent(
            title = title,
            description = description,
            value = if (checked) {
                stringResource(R.string.enabled)
            } else {
                stringResource(R.string.disabled)
            },
        )
    }
}

@Composable
internal fun SettingActionRow(
    title: String,
    description: String,
    enabled: Boolean,
    danger: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF202738),
            focusedContainerColor = if (danger) Danger else Primary,
        ),
    ) {
        SettingRowContent(title, description, "")
    }
}

@Composable
internal fun SettingRowContent(
    title: String,
    description: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 13.sp)
        }
        if (value.isNotEmpty()) {
            Text(value, fontSize = 15.sp)
        }
    }
}

@Composable
internal fun SettingValue(
    label: String,
    value: String,
    detail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Muted, fontSize = 13.sp)
            Text(value, color = OnBackground, fontSize = 18.sp)
        }
        Text(detail, color = Muted, fontSize = 14.sp)
    }
}

@Composable
private fun SubtitleLanguageDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val textFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        textFocus.requestFocus()
    }
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC050812)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(PanelElevated, RoundedCornerShape(22.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.subtitle_language_preference),
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.subtitle_language_editor_hint),
                color = Muted,
                fontSize = 14.sp,
            )
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(textFocus)
                    .background(Color(0xFF202738), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                textStyle = TextStyle(
                    color = OnBackground,
                    fontSize = 18.sp,
                ),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = { onSave(value) }) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun SettingsChoiceDialog(
    choice: SettingsChoice,
    onDismiss: () -> Unit,
    onSelect: (SettingsChoiceOption) -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(choice) {
        withFrameNanos { }
        initialFocus.requestFocus()
    }
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC050812)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .background(PanelElevated, RoundedCornerShape(22.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = choice.title,
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            choice.options.forEachIndexed { index, option ->
                Button(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties {
                            left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                            if (index == 0) {
                                up = FocusRequester.Cancel
                            }
                            if (index == choice.options.lastIndex) {
                                down = FocusRequester.Cancel
                            }
                        }
                        .then(
                            if (option.selected || choice.options.none { it.selected } && index == 0) {
                                Modifier.focusRequester(initialFocus)
                            } else {
                                Modifier
                            },
                        ),
                    colors = ButtonDefaults.colors(
                        containerColor = if (option.selected) {
                            PanelSelected
                        } else {
                            Panel
                        },
                        focusedContainerColor = Primary,
                    ),
                ) {
                    Text(option.label)
                }
            }
        }
    }
}

@Composable
private fun SettingsStatus(
    title: String,
    description: String,
    onRetry: (() -> Unit)? = null,
) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(onRetry) {
        if (onRetry != null) {
            retryFocus.requestFocus()
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = OnBackground, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(description, color = Muted, fontSize = 15.sp)
            onRetry?.let {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = it,
                    modifier = Modifier.focusRequester(retryFocus),
                    colors = ButtonDefaults.colors(focusedContainerColor = Primary),
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
internal fun settingRowColors() = ButtonDefaults.colors(
    containerColor = PanelElevated,
    focusedContainerColor = Primary,
)

@Composable
private fun sectionLabel(section: SettingsSection): String =
    when (section) {
        SettingsSection.Playback -> stringResource(R.string.playback_settings)
        SettingsSection.Danmaku -> stringResource(R.string.danmaku)
        SettingsSection.Subtitle -> stringResource(R.string.subtitle)
        SettingsSection.Behavior -> stringResource(R.string.client_behavior)
        SettingsSection.ServerAccount -> stringResource(R.string.server_and_account)
    }

@Composable
private fun sectionTitle(section: SettingsSection): String =
    when (section) {
        SettingsSection.Playback -> stringResource(R.string.playback_settings_title)
        SettingsSection.Danmaku -> stringResource(R.string.danmaku_settings_title)
        SettingsSection.Subtitle -> stringResource(R.string.subtitle_settings_title)
        SettingsSection.Behavior -> stringResource(R.string.client_behavior)
        SettingsSection.ServerAccount -> stringResource(R.string.server_and_account)
    }

@Composable
private fun sectionDescription(section: SettingsSection): String =
    when (section) {
        SettingsSection.Playback -> stringResource(R.string.playback_settings_description)
        SettingsSection.Danmaku -> stringResource(R.string.danmaku_settings_description)
        SettingsSection.Subtitle -> stringResource(R.string.subtitle_settings_description)
        SettingsSection.Behavior -> stringResource(R.string.client_behavior_description)
        SettingsSection.ServerAccount -> stringResource(R.string.server_account_description)
    }

@Composable
internal fun playbackModeLabel(mode: PlaybackMode): String =
    when (mode) {
        PlaybackMode.Auto -> stringResource(R.string.playback_mode_auto)
        PlaybackMode.Direct -> stringResource(R.string.playback_mode_direct)
        PlaybackMode.Transcode -> stringResource(R.string.playback_mode_transcode)
    }

@Composable
internal fun resolutionLabel(resolution: TranscodeResolution): String =
    when (resolution) {
        TranscodeResolution.Original -> stringResource(R.string.resolution_original)
        TranscodeResolution.P1080 -> stringResource(R.string.resolution_1080p)
        TranscodeResolution.P720 -> stringResource(R.string.resolution_720p)
        TranscodeResolution.P480 -> stringResource(R.string.resolution_480p)
    }

@Composable
internal fun subtitleDisplayModeLabel(mode: SubtitleDisplayMode): String =
    when (mode) {
        SubtitleDisplayMode.Stroke -> stringResource(R.string.subtitle_display_mode_stroke)
        SubtitleDisplayMode.Background ->
            stringResource(R.string.subtitle_display_mode_background)
    }

internal fun formatSubtitleOffset(value: Float): String =
    if (value == 0f) {
        "0.0s"
    } else {
        String.format(Locale.US, "%+.1fs", value)
    }

@Composable
internal fun danmakuTextSizeLabel(size: DanmakuTextSize): String =
    when (size) {
        DanmakuTextSize.Small -> stringResource(R.string.danmaku_size_small)
        DanmakuTextSize.Medium -> stringResource(R.string.danmaku_size_medium)
        DanmakuTextSize.Large -> stringResource(R.string.danmaku_size_large)
        DanmakuTextSize.ExtraLarge -> stringResource(R.string.danmaku_size_extra_large)
    }

@Composable
internal fun danmakuSpeedLabel(speed: DanmakuSpeed): String =
    when (speed) {
        DanmakuSpeed.Slow -> stringResource(R.string.danmaku_speed_slow)
        DanmakuSpeed.Standard -> stringResource(R.string.danmaku_speed_standard)
        DanmakuSpeed.Fast -> stringResource(R.string.danmaku_speed_fast)
    }

@Composable
internal fun danmakuModeLabel(mode: DanmakuDisplayMode): String =
    when (mode) {
        DanmakuDisplayMode.Scroll -> stringResource(R.string.danmaku_mode_scroll)
        DanmakuDisplayMode.Top -> stringResource(R.string.danmaku_mode_top)
        DanmakuDisplayMode.Bottom -> stringResource(R.string.danmaku_mode_bottom)
    }

@Composable
internal fun danmakuModeDescription(mode: DanmakuDisplayMode): String =
    when (mode) {
        DanmakuDisplayMode.Scroll ->
            stringResource(R.string.danmaku_mode_scroll_description)

        DanmakuDisplayMode.Top ->
            stringResource(R.string.danmaku_mode_top_description)

        DanmakuDisplayMode.Bottom ->
            stringResource(R.string.danmaku_mode_bottom_description)
    }

@Composable
internal fun startPageLabel(page: StartPage): String =
    when (page) {
        StartPage.Home -> stringResource(R.string.home)
        StartPage.Search -> stringResource(R.string.search)
        StartPage.Library -> stringResource(R.string.library)
    }

@Composable
internal fun connectionDescription(connection: SettingsConnection): String =
    when (connection) {
        SettingsConnection.Idle -> stringResource(R.string.test_connection_description)
        SettingsConnection.Testing -> stringResource(R.string.testing)
        is SettingsConnection.Success -> if (connection.version.isBlank()) {
            stringResource(R.string.connection_success_short)
        } else {
            stringResource(R.string.connection_success, connection.version)
        }

        is SettingsConnection.Failure -> settingsErrorText(connection.error)
    }

@Composable
private fun settingsErrorText(error: AppError): String =
    when (error) {
        AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
        AppError.Forbidden -> stringResource(R.string.error_forbidden)
        AppError.NotFound -> stringResource(R.string.error_not_found)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.Offline -> stringResource(R.string.error_offline)
        is AppError.Api -> stringResource(R.string.error_api, error.code.orEmpty())
        is AppError.InvalidData -> stringResource(R.string.error_invalid_data)
    }

internal data class SettingsChoice(
    val title: String,
    val options: List<SettingsChoiceOption>,
)

internal data class SettingsChoiceOption(
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit,
)
