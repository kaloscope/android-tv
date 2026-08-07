package org.kaloscope.tv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialogOption
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.accentPalette
import org.kaloscope.tv.core.designsystem.danmakuBlockSummary
import org.kaloscope.tv.core.designsystem.danmakuBlockTypeLabel
import org.kaloscope.tv.core.designsystem.danmakuSpeedLabel
import org.kaloscope.tv.core.designsystem.danmakuTextSizeLabel
import org.kaloscope.tv.core.designsystem.formatSubtitleOffset
import org.kaloscope.tv.core.designsystem.subtitleDisplayModeLabel
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuBlockPolicy
import org.kaloscope.tv.core.model.DanmakuBlockType
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize
import org.kaloscope.tv.core.model.ImagePageDirection
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ImageZoomMode
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderSettingsPolicy
import org.kaloscope.tv.core.model.Session
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleSettingsPolicy
import org.kaloscope.tv.core.model.TextReaderFont
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.TextReaderTheme
import org.kaloscope.tv.core.model.TvSettings
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
                    KaloscopeChoiceDialogOption(
                        label = playbackModeLabel(mode),
                        selected = { mode == state.settings.playbackMode },
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
                    KaloscopeChoiceDialogOption(
                        label = resolutionLabel(resolution),
                        selected = { resolution == state.settings.transcodeResolution },
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
internal fun ReadingSettings(
    settings: TvSettings,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onChapterOrder: (ReaderChapterOrder) -> Unit,
    onImageChange: (ImageReaderSettings) -> Unit,
    onTextChange: (TextReaderSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reading-default-settings"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ReaderSettingsGroupTitle(stringResource(R.string.reader_common_settings))
        ChoiceSettingRow(
            title = stringResource(R.string.reader_chapter_order),
            description = stringResource(R.string.reader_chapter_order_description),
            value = readerChapterOrderLabel(settings.readerChapterOrder),
            interactionsEnabled = interactionsEnabled,
            createChoice = {
                SettingsChoice(
                    title = stringResource(R.string.reader_chapter_order),
                    options = ReaderChapterOrder.entries.map { order ->
                        KaloscopeChoiceDialogOption(
                            label = readerChapterOrderLabel(order),
                            selected = { order == settings.readerChapterOrder },
                            onSelect = { onChapterOrder(order) },
                        )
                    },
                )
            },
            onOpenChoice = onOpenChoice,
        )

        Spacer(Modifier.height(6.dp))
        ReaderSettingsGroupTitle(stringResource(R.string.reader_image_settings))
        ReaderChoiceRow(
            title = R.string.reader_image_read_mode,
            description = R.string.reader_image_read_mode_description,
            value = imageReadModeLabel(settings.imageReader.readMode),
            options = ImageReadMode.entries,
            selected = { it == settings.imageReader.readMode },
            label = { imageReadModeLabel(it) },
            interactionsEnabled = interactionsEnabled,
            onOpenChoice = onOpenChoice,
            onSelect = { onImageChange(settings.imageReader.copy(readMode = it)) },
        )
        ReaderChoiceRow(
            title = R.string.reader_image_zoom,
            description = R.string.reader_image_zoom_description,
            value = imageZoomModeLabel(settings.imageReader.zoomMode),
            options = ImageZoomMode.entries,
            selected = { it == settings.imageReader.zoomMode },
            label = { imageZoomModeLabel(it) },
            interactionsEnabled = interactionsEnabled,
            onOpenChoice = onOpenChoice,
            onSelect = { onImageChange(settings.imageReader.copy(zoomMode = it)) },
        )
        ReaderChoiceRow(
            title = R.string.reader_page_direction,
            description = R.string.reader_page_direction_description,
            value = imagePageDirectionLabel(settings.imageReader.pageDirection),
            options = ImagePageDirection.entries,
            selected = { it == settings.imageReader.pageDirection },
            label = { imagePageDirectionLabel(it) },
            interactionsEnabled = interactionsEnabled,
            onOpenChoice = onOpenChoice,
            onSelect = { onImageChange(settings.imageReader.copy(pageDirection = it)) },
        )

        Spacer(Modifier.height(6.dp))
        ReaderSettingsGroupTitle(stringResource(R.string.reader_text_settings))
        ReaderChoiceRow(
            title = R.string.reader_text_theme,
            description = R.string.reader_text_theme_description,
            value = textReaderThemeLabel(settings.textReader.theme),
            options = TextReaderTheme.entries,
            selected = { it == settings.textReader.theme },
            label = { textReaderThemeLabel(it) },
            interactionsEnabled = interactionsEnabled,
            onOpenChoice = onOpenChoice,
            onSelect = { onTextChange(settings.textReader.copy(theme = it)) },
        )
        ReaderChoiceRow(
            title = R.string.reader_text_font,
            description = R.string.reader_text_font_description,
            value = textReaderFontLabel(settings.textReader.font),
            options = TextReaderFont.entries,
            selected = { it == settings.textReader.font },
            label = { textReaderFontLabel(it) },
            interactionsEnabled = interactionsEnabled,
            onOpenChoice = onOpenChoice,
            onSelect = { onTextChange(settings.textReader.copy(font = it)) },
        )
        AdjustableSettingRow(
            title = stringResource(R.string.reader_font_size),
            description = stringResource(R.string.reader_font_size_description),
            value = stringResource(R.string.reader_sp_value, settings.textReader.fontSizeSp),
            interactionsEnabled = interactionsEnabled,
            onDecrease = {
                onTextChange(
                    settings.textReader.copy(
                        fontSizeSp = settings.textReader.fontSizeSp -
                            ReaderSettingsPolicy.FONT_SIZE_STEP_SP,
                    ),
                )
            },
            onIncrease = {
                onTextChange(
                    settings.textReader.copy(
                        fontSizeSp = settings.textReader.fontSizeSp +
                            ReaderSettingsPolicy.FONT_SIZE_STEP_SP,
                    ),
                )
            },
        )
        AdjustableSettingRow(
            title = stringResource(R.string.reader_line_height),
            description = stringResource(R.string.reader_line_height_description),
            value = stringResource(
                R.string.reader_multiplier_value,
                settings.textReader.lineHeight,
            ),
            interactionsEnabled = interactionsEnabled,
            onDecrease = {
                onTextChange(
                    settings.textReader.copy(
                        lineHeight = settings.textReader.lineHeight -
                            ReaderSettingsPolicy.LINE_HEIGHT_STEP,
                    ),
                )
            },
            onIncrease = {
                onTextChange(
                    settings.textReader.copy(
                        lineHeight = settings.textReader.lineHeight +
                            ReaderSettingsPolicy.LINE_HEIGHT_STEP,
                    ),
                )
            },
        )
        AdjustableSettingRow(
            title = stringResource(R.string.reader_paragraph_spacing),
            description = stringResource(R.string.reader_paragraph_spacing_description),
            value = stringResource(
                R.string.reader_em_value,
                settings.textReader.paragraphSpacingEm,
            ),
            interactionsEnabled = interactionsEnabled,
            onDecrease = {
                onTextChange(
                    settings.textReader.copy(
                        paragraphSpacingEm = settings.textReader.paragraphSpacingEm -
                            ReaderSettingsPolicy.PARAGRAPH_SPACING_STEP_EM,
                    ),
                )
            },
            onIncrease = {
                onTextChange(
                    settings.textReader.copy(
                        paragraphSpacingEm = settings.textReader.paragraphSpacingEm +
                            ReaderSettingsPolicy.PARAGRAPH_SPACING_STEP_EM,
                    ),
                )
            },
        )
        AdjustableSettingRow(
            title = stringResource(R.string.reader_horizontal_padding),
            description = stringResource(R.string.reader_horizontal_padding_description),
            value = stringResource(
                R.string.reader_dp_value,
                settings.textReader.horizontalPaddingDp,
            ),
            interactionsEnabled = interactionsEnabled,
            onDecrease = {
                onTextChange(
                    settings.textReader.copy(
                        horizontalPaddingDp = settings.textReader.horizontalPaddingDp -
                            ReaderSettingsPolicy.HORIZONTAL_PADDING_STEP_DP,
                    ),
                )
            },
            onIncrease = {
                onTextChange(
                    settings.textReader.copy(
                        horizontalPaddingDp = settings.textReader.horizontalPaddingDp +
                            ReaderSettingsPolicy.HORIZONTAL_PADDING_STEP_DP,
                    ),
                )
            },
        )
    }
}

@Composable
private fun ReaderSettingsGroupTitle(text: String) {
    Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun <T> ReaderChoiceRow(
    title: Int,
    description: Int,
    value: String,
    options: List<T>,
    selected: (T) -> Boolean,
    label: @Composable (T) -> String,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onSelect: (T) -> Unit,
) {
    val titleText = stringResource(title)
    ChoiceSettingRow(
        title = titleText,
        description = stringResource(description),
        value = value,
        interactionsEnabled = interactionsEnabled,
        createChoice = {
            SettingsChoice(
                title = titleText,
                options = options.map { option ->
                    KaloscopeChoiceDialogOption(
                        label = label(option),
                        selected = { selected(option) },
                        onSelect = { onSelect(option) },
                    )
                },
            )
        },
        onOpenChoice = onOpenChoice,
    )
}

@Composable
internal fun readerChapterOrderLabel(value: ReaderChapterOrder): String =
    stringResource(
        when (value) {
            ReaderChapterOrder.Ascending -> R.string.reader_order_ascending
            ReaderChapterOrder.Descending -> R.string.reader_order_descending
        },
    )

@Composable
internal fun imageReadModeLabel(value: ImageReadMode): String =
    stringResource(
        when (value) {
            ImageReadMode.Scroll -> R.string.reader_mode_scroll
            ImageReadMode.Paged -> R.string.reader_mode_paged
        },
    )

@Composable
internal fun imageZoomModeLabel(value: ImageZoomMode): String =
    stringResource(
        when (value) {
            ImageZoomMode.Auto -> R.string.reader_zoom_auto
            ImageZoomMode.FitWidth -> R.string.reader_zoom_fit_width
            ImageZoomMode.FitHeight -> R.string.reader_zoom_fit_height
        },
    )

@Composable
internal fun imagePageDirectionLabel(value: ImagePageDirection): String =
    stringResource(
        when (value) {
            ImagePageDirection.Right -> R.string.reader_direction_right
            ImagePageDirection.Left -> R.string.reader_direction_left
            ImagePageDirection.Down -> R.string.reader_direction_down
        },
    )

@Composable
internal fun textReaderThemeLabel(value: TextReaderTheme): String =
    stringResource(
        when (value) {
            TextReaderTheme.White -> R.string.reader_theme_white
            TextReaderTheme.Cream -> R.string.reader_theme_cream
            TextReaderTheme.Sepia -> R.string.reader_theme_sepia
            TextReaderTheme.LightGray -> R.string.reader_theme_light_gray
            TextReaderTheme.Green -> R.string.reader_theme_green
            TextReaderTheme.Dark -> R.string.reader_theme_dark
            TextReaderTheme.Slate -> R.string.reader_theme_slate
            TextReaderTheme.Black -> R.string.reader_theme_black
        },
    )

@Composable
internal fun textReaderFontLabel(value: TextReaderFont): String =
    stringResource(
        when (value) {
            TextReaderFont.System -> R.string.reader_font_system
            TextReaderFont.Sans -> R.string.reader_font_sans
            TextReaderFont.Serif -> R.string.reader_font_serif
            TextReaderFont.Kai -> R.string.reader_font_kai
            TextReaderFont.Monospace -> R.string.reader_font_monospace
        },
    )

@Composable
internal fun SubtitleDefaultSettings(
    settings: SubtitleSettings,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onOpenLanguage: (FocusRequester) -> Unit,
    onChange: (SubtitleSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subtitle-default-settings"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToggleSettingRow(
            title = stringResource(R.string.default_subtitle),
            description = stringResource(R.string.default_subtitle_description),
            checked = settings.enabled,
            interactionsEnabled = interactionsEnabled,
            onToggle = { onChange(settings.copy(enabled = !settings.enabled)) },
        )
        SubtitleLanguageSettingRow(
            value = settings.languagePreference,
            interactionsEnabled = interactionsEnabled,
            onOpen = onOpenLanguage,
        )
        ChoiceSettingRow(
            title = stringResource(R.string.subtitle_display_mode),
            description = stringResource(R.string.subtitle_display_mode_description),
            value = subtitleDisplayModeLabel(settings.displayMode),
            interactionsEnabled = interactionsEnabled,
            createChoice = {
                SettingsChoice(
                    title = stringResource(R.string.subtitle_display_mode),
                    options = SubtitleDisplayMode.entries.map { mode ->
                        KaloscopeChoiceDialogOption(
                            label = subtitleDisplayModeLabel(mode),
                            selected = { mode == settings.displayMode },
                            onSelect = { onChange(settings.copy(displayMode = mode)) },
                        )
                    },
                )
            },
            onOpenChoice = onOpenChoice,
        )
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
        )
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
) {
    var isAdjusting by remember { mutableStateOf(false) }
    var consumeBackKeyUp by remember { mutableStateOf(false) }
    KaloscopeButton(
        onClick = {
            if (interactionsEnabled) {
                isAdjusting = !isAdjusting
            }
        },
        selected = isAdjusting,
        size = KaloscopeControlSize.Row,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) {
                    isAdjusting = false
                    consumeBackKeyUp = false
                }
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.key == Key.Back &&
                        event.type == KeyEventType.KeyDown &&
                        isAdjusting -> {
                        isAdjusting = false
                        consumeBackKeyUp = true
                        true
                    }

                    event.key == Key.Back &&
                        event.type == KeyEventType.KeyUp &&
                        consumeBackKeyUp -> {
                        consumeBackKeyUp = false
                        true
                    }

                    event.type == KeyEventType.KeyDown &&
                        isAdjusting &&
                        event.key == Key.DirectionLeft -> {
                        if (interactionsEnabled) {
                            onDecrease()
                        }
                        true
                    }

                    event.type == KeyEventType.KeyDown &&
                        isAdjusting &&
                        event.key == Key.DirectionRight -> {
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
) {
    val percentages = listOf(25, 50, 75, 100)
    var blockDraft by remember { mutableStateOf(settings) }
    LaunchedEffect(settings) {
        blockDraft = settings
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToggleSettingRow(
            title = stringResource(R.string.default_danmaku),
            description = stringResource(R.string.default_danmaku_description),
            checked = settings.enabled,
            interactionsEnabled = interactionsEnabled,
            onToggle = { onChange(settings.copy(enabled = !settings.enabled)) },
        )
        ChoiceSettingRow(
            title = stringResource(R.string.danmaku_text_size),
            description = stringResource(R.string.danmaku_text_size_description),
            value = danmakuTextSizeLabel(settings.textSize),
            interactionsEnabled = interactionsEnabled,
            createChoice = {
                SettingsChoice(
                    title = stringResource(R.string.danmaku_text_size),
                    options = DanmakuTextSize.entries.map { size ->
                        KaloscopeChoiceDialogOption(
                            label = danmakuTextSizeLabel(size),
                            selected = { size == settings.textSize },
                            onSelect = { onChange(settings.copy(textSize = size)) },
                        )
                    },
                )
            },
            onOpenChoice = onOpenChoice,
        )
        ChoiceSettingRow(
            title = stringResource(R.string.danmaku_speed),
            description = stringResource(R.string.danmaku_speed_description),
            value = danmakuSpeedLabel(settings.speed),
            interactionsEnabled = interactionsEnabled,
            createChoice = {
                SettingsChoice(
                    title = stringResource(R.string.danmaku_speed),
                    options = DanmakuSpeed.entries.map { speed ->
                        KaloscopeChoiceDialogOption(
                            label = danmakuSpeedLabel(speed),
                            selected = { speed == settings.speed },
                            onSelect = { onChange(settings.copy(speed = speed)) },
                        )
                    },
                )
            },
            onOpenChoice = onOpenChoice,
        )
        DanmakuPercentageSetting(
            title = stringResource(R.string.danmaku_opacity),
            description = stringResource(R.string.danmaku_opacity_description),
            value = settings.opacityPercent,
            percentages = percentages,
            interactionsEnabled = interactionsEnabled,
            onOpenChoice = onOpenChoice,
            onSelect = { onChange(settings.copy(opacityPercent = it)) },
        )
        DanmakuPercentageSetting(
            title = stringResource(R.string.danmaku_display_area),
            description = stringResource(R.string.danmaku_display_area_description),
            value = settings.displayAreaPercent,
            percentages = percentages,
            interactionsEnabled = interactionsEnabled,
            onOpenChoice = onOpenChoice,
            onSelect = { onChange(settings.copy(displayAreaPercent = it)) },
        )
        ChoiceSettingRow(
            title = stringResource(R.string.danmaku_block_types),
            description = stringResource(R.string.danmaku_block_types_description),
            value = danmakuBlockSummary(settings),
            interactionsEnabled = interactionsEnabled,
            onBeforeOpen = { blockDraft = settings },
            createChoice = {
                SettingsChoice(
                    title = stringResource(R.string.danmaku_block_types),
                    dismissOnSelect = false,
                    onDismiss = {
                        if (blockDraft != settings) {
                            onChange(blockDraft)
                        }
                    },
                    options = DanmakuBlockType.entries.map { type ->
                        KaloscopeChoiceDialogOption(
                            label = danmakuBlockTypeLabel(type),
                            selected = {
                                DanmakuBlockPolicy.isSelected(blockDraft, type)
                            },
                            testTag =
                                "settings-danmaku-block-${type.name.lowercase()}",
                            onSelect = {
                                blockDraft = DanmakuBlockPolicy.toggle(blockDraft, type)
                            },
                        )
                    },
                )
            },
            onOpenChoice = onOpenChoice,
        )
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
                    KaloscopeChoiceDialogOption(
                        label = stringResource(R.string.percentage_value, percentage),
                        selected = { percentage == value },
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
    onAccentColor: (AccentColor) -> Unit,
    onStartPage: (StartPage) -> Unit,
) {
    ChoiceSettingRow(
        title = stringResource(R.string.accent_color),
        description = stringResource(R.string.accent_color_description),
        value = accentColorLabel(state.settings.accentColor),
        interactionsEnabled = interactionsEnabled,
        createChoice = {
            SettingsChoice(
                title = stringResource(R.string.accent_color),
                options = AccentColor.entries.map { accentColor ->
                    KaloscopeChoiceDialogOption(
                        label = accentColorLabel(accentColor),
                        selected = { accentColor == state.settings.accentColor },
                        swatchColor = accentColor.accentPalette().primary,
                        testTag = "accent-option-${accentColor.name.lowercase()}",
                        swatchTestTag = "accent-swatch-${accentColor.name.lowercase()}",
                        onSelect = { onAccentColor(accentColor) },
                    )
                },
            )
        },
        onOpenChoice = onOpenChoice,
    )
    Spacer(Modifier.height(10.dp))
    ChoiceSettingRow(
        title = stringResource(R.string.default_start_page),
        description = stringResource(R.string.default_start_page_description),
        value = startPageLabel(state.settings.startPage),
        interactionsEnabled = interactionsEnabled,
        createChoice = {
            SettingsChoice(
                title = stringResource(R.string.default_start_page),
                options = StartPage.entries.map { page ->
                    KaloscopeChoiceDialogOption(
                        label = startPageLabel(page),
                        selected = { page == state.settings.startPage },
                        onSelect = { onStartPage(page) },
                    )
                },
            )
        },
        onOpenChoice = onOpenChoice,
    )
}

@Composable
internal fun accentColorLabel(accentColor: AccentColor): String =
    when (accentColor) {
        AccentColor.Blue -> stringResource(R.string.accent_color_blue)
        AccentColor.Purple -> stringResource(R.string.accent_color_purple)
        AccentColor.Orange -> stringResource(R.string.accent_color_orange)
        AccentColor.Yellow -> stringResource(R.string.accent_color_yellow)
        AccentColor.Green -> stringResource(R.string.accent_color_green)
    }

@Composable
internal fun ServerAccountSettings(
    session: Session,
    connection: SettingsConnection,
    interactionsEnabled: Boolean,
    onTestConnection: () -> Unit,
    onManageServers: () -> Unit,
    onRequestLogout: (FocusRequester) -> Unit,
) {
    val logoutFocus = remember { FocusRequester() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingActionRow(
            title = stringResource(R.string.test_connection),
            description = connectionDescription(connection),
            value = session.server.origin,
            interactionsEnabled = interactionsEnabled &&
                connection != SettingsConnection.Testing,
            danger = false,
            onClick = onTestConnection,
        )
        SettingActionRow(
            title = stringResource(R.string.manage_servers),
            description = stringResource(R.string.manage_servers_description),
            value = session.server.name,
            interactionsEnabled = interactionsEnabled,
            danger = false,
            onClick = onManageServers,
        )
        SettingActionRow(
            title = stringResource(R.string.logout),
            description = stringResource(R.string.logout_description),
            value = session.user.username,
            interactionsEnabled = interactionsEnabled,
            danger = true,
            modifier = Modifier.focusRequester(logoutFocus),
            onClick = { onRequestLogout(logoutFocus) },
        )
    }
}
