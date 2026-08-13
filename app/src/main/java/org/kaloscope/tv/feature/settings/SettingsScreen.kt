package org.kaloscope.tv.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceIndicator
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialog
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialogOption
import org.kaloscope.tv.core.designsystem.KaloscopeSelectionIndicatorType
import org.kaloscope.tv.core.designsystem.KaloscopeConfirmDialog
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlTone
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.designsystem.KaloscopeNavigationIcon
import org.kaloscope.tv.core.designsystem.KaloscopeSwitchIndicator
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.TvTextField
import org.kaloscope.tv.core.designsystem.appErrorText
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeQuality

@Composable
fun SettingsScreen(
    session: Session,
    state: SettingsUiState,
    requestInitialFocus: Boolean = true,
    selectedSectionFocusRequester: FocusRequester? = null,
    topNavigationFocusRequester: FocusRequester? = null,
    onRetry: () -> Unit,
    onSelectSection: (SettingsSection) -> Unit,
    onPlaybackMode: (PlaybackMode) -> Unit,
    onTranscodeQuality: (TranscodeQuality) -> Unit,
    onAutoplayNext: (Boolean) -> Unit,
    onAccentColor: (AccentColor) -> Unit = {},
    onDanmakuSettings: (DanmakuSettings) -> Unit,
    onSubtitleSettings: (SubtitleSettings) -> Unit,
    onStartPage: (StartPage) -> Unit,
    onReaderChapterOrder: (ReaderChapterOrder) -> Unit = {},
    onImageReaderSettings: (ImageReaderSettings) -> Unit = {},
    onTextReaderSettings: (TextReaderSettings) -> Unit = {},
    onTestConnection: () -> Unit,
    onManageServers: () -> Unit,
    onLogout: () -> Unit,
) {
    when (state) {
        SettingsUiState.Loading -> KaloscopeLoadingLayout("settings-loading")

        is SettingsUiState.Error -> SettingsStatus(
            title = stringResource(R.string.settings_load_failed),
            description = appErrorText(state.error),
            onRetry = onRetry,
        )

        is SettingsUiState.Content -> SettingsContent(
            session = session,
            state = state,
            requestInitialFocus = requestInitialFocus,
            selectedSectionFocusRequester = selectedSectionFocusRequester,
            topNavigationFocusRequester = topNavigationFocusRequester,
            onSelectSection = onSelectSection,
            onPlaybackMode = onPlaybackMode,
            onTranscodeQuality = onTranscodeQuality,
            onAutoplayNext = onAutoplayNext,
            onAccentColor = onAccentColor,
            onDanmakuSettings = onDanmakuSettings,
            onSubtitleSettings = onSubtitleSettings,
            onStartPage = onStartPage,
            onReaderChapterOrder = onReaderChapterOrder,
            onImageReaderSettings = onImageReaderSettings,
            onTextReaderSettings = onTextReaderSettings,
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
    requestInitialFocus: Boolean,
    selectedSectionFocusRequester: FocusRequester?,
    topNavigationFocusRequester: FocusRequester?,
    onSelectSection: (SettingsSection) -> Unit,
    onPlaybackMode: (PlaybackMode) -> Unit,
    onTranscodeQuality: (TranscodeQuality) -> Unit,
    onAutoplayNext: (Boolean) -> Unit,
    onAccentColor: (AccentColor) -> Unit,
    onDanmakuSettings: (DanmakuSettings) -> Unit,
    onSubtitleSettings: (SubtitleSettings) -> Unit,
    onStartPage: (StartPage) -> Unit,
    onReaderChapterOrder: (ReaderChapterOrder) -> Unit,
    onImageReaderSettings: (ImageReaderSettings) -> Unit,
    onTextReaderSettings: (TextReaderSettings) -> Unit,
    onTestConnection: () -> Unit,
    onManageServers: () -> Unit,
    onLogout: () -> Unit,
) {
    var choice by remember { mutableStateOf<SettingsChoice?>(null) }
    var languageEditorOpen by remember { mutableStateOf(false) }
    var logoutConfirmationOpen by remember { mutableStateOf(false) }
    var restoreFocus by remember { mutableStateOf<FocusRequester?>(null) }
    val internalSelectedSectionFocus = remember { FocusRequester() }
    val selectedSectionFocus =
        selectedSectionFocusRequester ?: internalSelectedSectionFocus
    // A transiently disabled TV control loses focus before its dialog can take over.
    val interactionsEnabled = choice == null &&
        !languageEditorOpen &&
        !logoutConfirmationOpen &&
        !state.isSaving

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            selectedSectionFocus.requestFocus()
        }
    }
    LaunchedEffect(choice, languageEditorOpen, logoutConfirmationOpen, restoreFocus) {
        if (choice == null && !languageEditorOpen && !logoutConfirmationOpen) {
            restoreFocus?.let { requester ->
                withFrameNanos { }
                requester.requestFocus()
                restoreFocus = null
            }
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportSize = DpSize(maxWidth, maxHeight)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsMenu(
                selected = state.section,
                interactionsEnabled = interactionsEnabled,
                selectedFocus = selectedSectionFocus,
                topNavigationFocusRequester = topNavigationFocusRequester,
                onSelect = onSelectSection,
            )
            SettingsPanel(
                session = session,
                state = state,
                interactionsEnabled = interactionsEnabled,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 720.dp)
                    .focusProperties {
                        onExit = {
                            when (requestedFocusDirection) {
                                FocusDirection.Left ->
                                    selectedSectionFocus.requestFocus()

                                FocusDirection.Up ->
                                    topNavigationFocusRequester?.requestFocus()

                                else -> Unit
                            }
                        }
                    }
                    .focusGroup(),
                onOpenChoice = { focus, requestedChoice ->
                    restoreFocus = focus
                    choice = requestedChoice
                },
                onAutoplayNext = onAutoplayNext,
                onAccentColor = onAccentColor,
                onDanmakuSettings = onDanmakuSettings,
                onSubtitleSettings = onSubtitleSettings,
                onOpenSubtitleLanguage = { focus ->
                    restoreFocus = focus
                    languageEditorOpen = true
                },
                onTestConnection = onTestConnection,
                onManageServers = onManageServers,
                onRequestLogout = { focus ->
                    restoreFocus = focus
                    logoutConfirmationOpen = true
                },
                onPlaybackMode = onPlaybackMode,
                onTranscodeQuality = onTranscodeQuality,
                onStartPage = onStartPage,
                onReaderChapterOrder = onReaderChapterOrder,
                onImageReaderSettings = onImageReaderSettings,
                onTextReaderSettings = onTextReaderSettings,
            )
        }
        choice?.let { current ->
            KaloscopeChoiceDialog(
                title = current.title,
                options = current.options,
                viewportSize = viewportSize,
                dismissOnSelect = current.dismissOnSelect,
                selectionIndicator = current.selectionIndicator,
                onDismiss = {
                    current.onDismiss()
                    choice = null
                },
            )
        }
        if (languageEditorOpen) {
            SubtitleLanguageDialog(
                initialValue = state.settings.subtitle.languagePreference,
                viewportSize = viewportSize,
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
        if (logoutConfirmationOpen) {
            KaloscopeConfirmDialog(
                title = stringResource(R.string.logout_confirmation_title),
                message = stringResource(
                    R.string.logout_confirmation_message,
                    session.server.name,
                    session.user.username,
                ),
                cancelLabel = stringResource(R.string.cancel),
                confirmLabel = stringResource(R.string.logout),
                confirmTone = KaloscopeControlTone.Danger,
                onDismiss = { logoutConfirmationOpen = false },
                onConfirm = {
                    logoutConfirmationOpen = false
                    restoreFocus = null
                    onLogout()
                },
            )
        }
    }
}

@Composable
private fun SettingsMenu(
    selected: SettingsSection,
    interactionsEnabled: Boolean,
    selectedFocus: FocusRequester,
    topNavigationFocusRequester: FocusRequester?,
    onSelect: (SettingsSection) -> Unit,
) {
    val firstSection = SettingsSection.entries.first()
    Column(
        modifier = Modifier
            .width(210.dp)
            .fillMaxHeight()
            .background(Panel.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (section in SettingsSection.entries) {
            KaloscopeButton(
                onClick = {
                    if (interactionsEnabled && selected != section) {
                        onSelect(section)
                    }
                },
                selected = selected == section,
                size = KaloscopeControlSize.Row,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (section == selected) {
                            Modifier.focusRequester(selectedFocus)
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (section == firstSection) {
                            topNavigationFocusRequester?.let { requester ->
                                Modifier.focusProperties { up = requester }
                            } ?: Modifier
                        } else {
                            Modifier
                        },
                    )
                    .onFocusChanged { focusState ->
                        if (
                            interactionsEnabled &&
                            focusState.isFocused &&
                            selected != section
                        ) {
                            onSelect(section)
                        }
                    },
            ) {
                KaloscopeNavigationIcon(
                    iconRes = section.iconResource(),
                    modifier = Modifier.testTag(section.iconTestTag()),
                )
                Spacer(Modifier.width(10.dp))
                Text(sectionLabel(section))
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    session: Session,
    state: SettingsUiState.Content,
    interactionsEnabled: Boolean,
    modifier: Modifier,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onAutoplayNext: (Boolean) -> Unit,
    onDanmakuSettings: (DanmakuSettings) -> Unit,
    onSubtitleSettings: (SubtitleSettings) -> Unit,
    onOpenSubtitleLanguage: (FocusRequester) -> Unit,
    onTestConnection: () -> Unit,
    onManageServers: () -> Unit,
    onRequestLogout: (FocusRequester) -> Unit,
    onPlaybackMode: (PlaybackMode) -> Unit,
    onTranscodeQuality: (TranscodeQuality) -> Unit,
    onAccentColor: (AccentColor) -> Unit,
    onStartPage: (StartPage) -> Unit,
    onReaderChapterOrder: (ReaderChapterOrder) -> Unit,
    onImageReaderSettings: (ImageReaderSettings) -> Unit,
    onTextReaderSettings: (TextReaderSettings) -> Unit,
) {
    key(state.section) {
        LazyColumn(
            modifier = modifier
                .fillMaxHeight()
                .background(Panel.copy(alpha = 0.82f), RoundedCornerShape(22.dp))
                .testTag("settings-panel")
                .padding(horizontal = 28.dp, vertical = 26.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            interactionsEnabled = interactionsEnabled,
                            onOpenChoice = onOpenChoice,
                            onPlaybackMode = onPlaybackMode,
                            onTranscodeQuality = onTranscodeQuality,
                            onAutoplayNext = onAutoplayNext,
                        )

                        SettingsSection.Danmaku -> DanmakuDefaultSettings(
                            settings = state.settings.danmaku,
                            interactionsEnabled = interactionsEnabled,
                            onOpenChoice = onOpenChoice,
                            onChange = onDanmakuSettings,
                        )

                        SettingsSection.Subtitle -> SubtitleDefaultSettings(
                            settings = state.settings.subtitle,
                            interactionsEnabled = interactionsEnabled,
                            onOpenChoice = onOpenChoice,
                            onOpenLanguage = onOpenSubtitleLanguage,
                            onChange = onSubtitleSettings,
                        )

                        SettingsSection.Reading -> ReadingSettings(
                            settings = state.settings,
                            interactionsEnabled = interactionsEnabled,
                            onOpenChoice = onOpenChoice,
                            onChapterOrder = onReaderChapterOrder,
                            onImageChange = onImageReaderSettings,
                            onTextChange = onTextReaderSettings,
                        )

                        SettingsSection.Behavior -> BehaviorSettings(
                            state = state,
                            interactionsEnabled = interactionsEnabled,
                            onOpenChoice = onOpenChoice,
                            onAccentColor = onAccentColor,
                            onStartPage = onStartPage,
                        )

                        SettingsSection.ServerAccount -> ServerAccountSettings(
                            session = session,
                            connection = state.connection,
                            interactionsEnabled = interactionsEnabled,
                            onTestConnection = onTestConnection,
                            onManageServers = onManageServers,
                            onRequestLogout = onRequestLogout,
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
        }
    }
}

@Composable
internal fun ChoiceSettingRow(
    title: String,
    description: String,
    value: String,
    interactionsEnabled: Boolean,
    onBeforeOpen: () -> Unit = {},
    createChoice: @Composable () -> SettingsChoice,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
) {
    val focus = remember { FocusRequester() }
    val choice = createChoice()
    KaloscopeButton(
        onClick = {
            if (interactionsEnabled) {
                onBeforeOpen()
                onOpenChoice(focus, choice)
            }
        },
        size = KaloscopeControlSize.Row,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
    ) {
        SettingRowContent(title, description) {
            ChoiceSettingValue(value)
        }
    }
}

@Composable
internal fun ChoiceSettingValue(value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            fontSize = 15.sp,
        )
        KaloscopeChoiceIndicator()
    }
}

@Composable
internal fun ToggleSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    interactionsEnabled: Boolean,
    onToggle: () -> Unit,
) {
    KaloscopeButton(
        onClick = {
            if (interactionsEnabled) {
                onToggle()
            }
        },
        size = KaloscopeControlSize.Row,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                toggleableState = if (checked) {
                    ToggleableState.On
                } else {
                    ToggleableState.Off
                }
            },
    ) {
        SettingRowContent(
            title = title,
            description = description,
        ) {
            KaloscopeSwitchIndicator(checked = checked)
        }
    }
}

@Composable
internal fun SettingActionRow(
    title: String,
    description: String,
    interactionsEnabled: Boolean,
    danger: Boolean,
    modifier: Modifier = Modifier,
    value: String = "",
    valueColor: Color? = null,
    onClick: () -> Unit,
) {
    KaloscopeButton(
        onClick = {
            if (interactionsEnabled) {
                onClick()
            }
        },
        size = KaloscopeControlSize.Row,
        tone = if (danger) {
            KaloscopeControlTone.Danger
        } else {
            KaloscopeControlTone.Default
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        SettingRowContent(title, description, value, valueColor)
    }
}

@Composable
internal fun SettingRowContent(
    title: String,
    description: String,
    value: String,
    valueColor: Color? = null,
) {
    SettingRowContent(title, description) {
        if (value.isNotEmpty()) {
            Text(
                text = value,
                color = valueColor ?: LocalContentColor.current,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
internal fun SettingRowContent(
    title: String,
    description: String,
    unit: String? = null,
    trailingContent: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (unit == null) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        modifier = Modifier.alignByBaseline(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = unit,
                        modifier = Modifier
                            .alignByBaseline()
                            .graphicsLayer { translationY = -1.dp.toPx() },
                        color = LocalContentColor.current.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                    )
                }
            }
            Text(
                description,
                color = LocalContentColor.current.copy(alpha = 0.72f),
                fontSize = 13.sp,
            )
        }
        trailingContent()
    }
}

@Composable
private fun SubtitleLanguageDialog(
    initialValue: String,
    viewportSize: DpSize,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val textFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        textFocus.requestFocus()
    }
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(viewportSize)
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
                TvTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = stringResource(R.string.subtitle_language_preference_any),
                    focusRequester = textFocus,
                    imeAction = ImeAction.Done,
                    selectorTestTag = "subtitle-language-selector",
                    editorTestTag = "subtitle-language-editor",
                    modifier = Modifier
                        .fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    KaloscopeButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    KaloscopeButton(onClick = { onSave(value) }) {
                        Text(stringResource(R.string.save))
                    }
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
                KaloscopeButton(
                    onClick = it,
                    modifier = Modifier.focusRequester(retryFocus),
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@DrawableRes
private fun SettingsSection.iconResource(): Int =
    when (this) {
        SettingsSection.Playback -> R.drawable.ic_settings_playback
        SettingsSection.Danmaku -> R.drawable.ic_settings_danmaku
        SettingsSection.Subtitle -> R.drawable.ic_settings_subtitle
        SettingsSection.Reading -> R.drawable.ic_settings_reading
        SettingsSection.Behavior -> R.drawable.ic_settings_behavior
        SettingsSection.ServerAccount -> R.drawable.ic_settings_server_account
    }

private fun SettingsSection.iconTestTag(): String =
    when (this) {
        SettingsSection.Playback -> "settings-section-icon-playback"
        SettingsSection.Danmaku -> "settings-section-icon-danmaku"
        SettingsSection.Subtitle -> "settings-section-icon-subtitle"
        SettingsSection.Reading -> "settings-section-icon-reading"
        SettingsSection.Behavior -> "settings-section-icon-behavior"
        SettingsSection.ServerAccount -> "settings-section-icon-server-account"
    }

@Composable
private fun sectionLabel(section: SettingsSection): String =
    when (section) {
        SettingsSection.Playback -> stringResource(R.string.playback_settings)
        SettingsSection.Danmaku -> stringResource(R.string.danmaku)
        SettingsSection.Subtitle -> stringResource(R.string.subtitle)
        SettingsSection.Reading -> stringResource(R.string.reading_settings)
        SettingsSection.Behavior -> stringResource(R.string.client_behavior)
        SettingsSection.ServerAccount -> stringResource(R.string.server_and_account)
    }

@Composable
private fun sectionTitle(section: SettingsSection): String =
    when (section) {
        SettingsSection.Playback -> stringResource(R.string.playback_settings_title)
        SettingsSection.Danmaku -> stringResource(R.string.danmaku_settings_title)
        SettingsSection.Subtitle -> stringResource(R.string.subtitle_settings_title)
        SettingsSection.Reading -> stringResource(R.string.reading_settings_title)
        SettingsSection.Behavior -> stringResource(R.string.client_behavior)
        SettingsSection.ServerAccount -> stringResource(R.string.server_and_account)
    }

@Composable
private fun sectionDescription(section: SettingsSection): String =
    when (section) {
        SettingsSection.Playback -> stringResource(R.string.playback_settings_description)
        SettingsSection.Danmaku -> stringResource(R.string.danmaku_settings_description)
        SettingsSection.Subtitle -> stringResource(R.string.subtitle_settings_description)
        SettingsSection.Reading -> stringResource(R.string.reading_settings_description)
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
internal fun transcodeQualityLabel(quality: TranscodeQuality): String =
    when (quality) {
        TranscodeQuality.High -> stringResource(R.string.transcode_quality_high)
        TranscodeQuality.Medium -> stringResource(R.string.transcode_quality_medium)
        TranscodeQuality.Low -> stringResource(R.string.transcode_quality_low)
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

        is SettingsConnection.Failure -> appErrorText(connection.error)
    }

internal data class SettingsChoice(
    val title: String,
    val options: List<KaloscopeChoiceDialogOption>,
    val dismissOnSelect: Boolean = true,
    val selectionIndicator: KaloscopeSelectionIndicatorType? = null,
    val onDismiss: () -> Unit = {},
)
