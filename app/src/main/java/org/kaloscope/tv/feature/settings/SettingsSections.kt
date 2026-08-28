package org.kaloscope.tv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeAdjustmentArrow
import org.kaloscope.tv.core.designsystem.KaloscopeAdjustmentDirection
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialogOption
import org.kaloscope.tv.core.designsystem.KaloscopeSelectionIndicatorType
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Success
import org.kaloscope.tv.core.designsystem.accentPalette
import org.kaloscope.tv.core.designsystem.danmakuBlockSummary
import org.kaloscope.tv.core.designsystem.danmakuBlockTypeLabel
import org.kaloscope.tv.core.designsystem.danmakuSpeedLabel
import org.kaloscope.tv.core.designsystem.danmakuTextSizeLabel
import org.kaloscope.tv.core.designsystem.imagePageDirectionLabel
import org.kaloscope.tv.core.designsystem.imageReadModeLabel
import org.kaloscope.tv.core.designsystem.imageZoomModeLabel
import org.kaloscope.tv.core.designsystem.readerChapterOrderLabel
import org.kaloscope.tv.core.designsystem.readerBackgroundColor
import org.kaloscope.tv.core.designsystem.subtitleDisplayModeLabel
import org.kaloscope.tv.core.designsystem.textReaderFontLabel
import org.kaloscope.tv.core.designsystem.textReaderThemeLabel
import org.kaloscope.tv.core.designsystem.toDpDimensions
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuBlockPolicy
import org.kaloscope.tv.core.model.DanmakuBlockType
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSettingsPolicy
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
import org.kaloscope.tv.core.player.TranscodeQuality
import kotlin.math.roundToInt

@Composable
internal fun PlaybackSettings(
    state: SettingsUiState.Content,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onPlaybackMode: (PlaybackMode) -> Unit,
    onTranscodeQuality: (TranscodeQuality) -> Unit,
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
                            testTag = "playback-mode-option-${mode.name.lowercase()}",
                            onSelect = { onPlaybackMode(mode) },
                        )
                },
            )
        },
        onOpenChoice = onOpenChoice,
    )
    Spacer(Modifier.height(10.dp))
    ChoiceSettingRow(
        title = stringResource(R.string.transcode_quality),
        description = stringResource(R.string.transcode_quality_description),
        value = transcodeQualityLabel(state.settings.transcodeQuality),
        interactionsEnabled = interactionsEnabled,
        createChoice = {
            SettingsChoice(
                title = stringResource(R.string.transcode_quality),
                options = listOf(
                    TranscodeQuality.High,
                    TranscodeQuality.Medium,
                    TranscodeQuality.Low,
                ).map { quality ->
                    KaloscopeChoiceDialogOption(
                        label = transcodeQualityLabel(quality),
                        selected = { quality == state.settings.transcodeQuality },
                        testTag = "transcode-quality-option-${quality.name.lowercase()}",
                        onSelect = { onTranscodeQuality(quality) },
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
    val textDimensions = settings.textReader.toDpDimensions(LocalDensity.current)
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
            swatchColor = TextReaderTheme::readerBackgroundColor,
            optionTestTag = {
                "reader-theme-option-${it.name.lowercase()}"
            },
            swatchTestTag = {
                "reader-theme-swatch-${it.name.lowercase()}"
            },
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
            value = stringResource(
                R.string.reader_dp_value,
                textDimensions.fontSize.value.roundToInt(),
            ),
            interactionsEnabled = interactionsEnabled,
            canDecrease = settings.textReader.fontSizeSp >
                ReaderSettingsPolicy.MIN_FONT_SIZE_SP,
            canIncrease = settings.textReader.fontSizeSp <
                ReaderSettingsPolicy.MAX_FONT_SIZE_SP,
            testTagPrefix = "reader-font-size",
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
            canDecrease = settings.textReader.lineHeight >
                ReaderSettingsPolicy.MIN_LINE_HEIGHT,
            canIncrease = settings.textReader.lineHeight <
                ReaderSettingsPolicy.MAX_LINE_HEIGHT,
            testTagPrefix = "reader-line-height",
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
                R.string.reader_dp_value,
                textDimensions.paragraphSpacing.value.roundToInt(),
            ),
            interactionsEnabled = interactionsEnabled,
            canDecrease = settings.textReader.paragraphSpacingDp >
                ReaderSettingsPolicy.MIN_PARAGRAPH_SPACING_DP,
            canIncrease = settings.textReader.paragraphSpacingDp <
                ReaderSettingsPolicy.MAX_PARAGRAPH_SPACING_DP,
            testTagPrefix = "reader-paragraph-spacing",
            onDecrease = {
                onTextChange(
                    settings.textReader.copy(
                        paragraphSpacingDp = settings.textReader.paragraphSpacingDp -
                            ReaderSettingsPolicy.PARAGRAPH_SPACING_STEP_DP,
                    ),
                )
            },
            onIncrease = {
                onTextChange(
                    settings.textReader.copy(
                        paragraphSpacingDp = settings.textReader.paragraphSpacingDp +
                            ReaderSettingsPolicy.PARAGRAPH_SPACING_STEP_DP,
                    ),
                )
            },
        )
        AdjustableSettingRow(
            title = stringResource(R.string.reader_horizontal_padding),
            description = stringResource(R.string.reader_horizontal_padding_description),
            value = stringResource(
                R.string.reader_dp_value,
                textDimensions.horizontalPadding.value.roundToInt(),
            ),
            interactionsEnabled = interactionsEnabled,
            canDecrease = settings.textReader.horizontalPaddingDp >
                ReaderSettingsPolicy.MIN_HORIZONTAL_PADDING_DP,
            canIncrease = settings.textReader.horizontalPaddingDp <
                ReaderSettingsPolicy.MAX_HORIZONTAL_PADDING_DP,
            testTagPrefix = "reader-horizontal-padding",
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
    Text(
        text = text,
        color = OnBackground,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun <T> ReaderChoiceRow(
    title: Int,
    description: Int,
    value: String,
    options: List<T>,
    selected: (T) -> Boolean,
    label: @Composable (T) -> String,
    swatchColor: ((T) -> Color)? = null,
    optionTestTag: ((T) -> String)? = null,
    swatchTestTag: ((T) -> String)? = null,
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
                        swatchColor = swatchColor?.invoke(option),
                        testTag = optionTestTag?.invoke(option),
                        swatchTestTag = swatchTestTag?.invoke(option),
                        onSelect = { onSelect(option) },
                    )
                },
            )
        },
        onOpenChoice = onOpenChoice,
    )
}

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
            canDecrease = SubtitleSettingsPolicy.adjustFontScale(settings, -1)
                .fontScalePercent != settings.fontScalePercent,
            canIncrease = SubtitleSettingsPolicy.adjustFontScale(settings, 1)
                .fontScalePercent != settings.fontScalePercent,
            testTagPrefix = "subtitle-font-scale",
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
            canDecrease = SubtitleSettingsPolicy.adjustVerticalPosition(settings, -1)
                .verticalPositionPercent != settings.verticalPositionPercent,
            canIncrease = SubtitleSettingsPolicy.adjustVerticalPosition(settings, 1)
                .verticalPositionPercent != settings.verticalPositionPercent,
            testTagPrefix = "subtitle-vertical-position",
            onDecrease = {
                onChange(SubtitleSettingsPolicy.adjustVerticalPosition(settings, -1))
            },
            onIncrease = {
                onChange(SubtitleSettingsPolicy.adjustVerticalPosition(settings, 1))
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
        ) {
            ChoiceSettingValue(
                value.ifBlank {
                    stringResource(R.string.subtitle_language_preference_any)
                },
            )
        }
    }
}

@Composable
private fun AdjustableSettingRow(
    title: String,
    description: String,
    unit: String? = null,
    value: String,
    interactionsEnabled: Boolean,
    canDecrease: Boolean,
    canIncrease: Boolean,
    testTagPrefix: String,
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
        preserveSelectionOnFocus = true,
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
                        if (interactionsEnabled && canDecrease) {
                            onDecrease()
                        }
                        true
                    }

                    event.type == KeyEventType.KeyDown &&
                        isAdjusting &&
                        event.key == Key.DirectionRight -> {
                        if (interactionsEnabled && canIncrease) {
                            onIncrease()
                        }
                        true
                    }

                    else -> false
                }
            },
    ) {
        SettingRowContent(title, description, unit) {
            AdjustableSettingValue(
                value = value,
                canDecrease = canDecrease,
                canIncrease = canIncrease,
                testTagPrefix = testTagPrefix,
            )
        }
    }
}

@Composable
private fun AdjustableSettingValue(
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    testTagPrefix: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KaloscopeAdjustmentArrow(
            direction = KaloscopeAdjustmentDirection.Decrease,
            enabled = canDecrease,
            testTag = "$testTagPrefix-decrease",
        )
        Text(text = value, fontSize = 15.sp)
        KaloscopeAdjustmentArrow(
            direction = KaloscopeAdjustmentDirection.Increase,
            enabled = canIncrease,
            testTag = "$testTagPrefix-increase",
        )
    }
}

@Composable
internal fun DanmakuDefaultSettings(
    settings: DanmakuSettings,
    interactionsEnabled: Boolean,
    onOpenChoice: (FocusRequester, SettingsChoice) -> Unit,
    onChange: (DanmakuSettings) -> Unit,
) {
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
            interactionsEnabled = interactionsEnabled,
            testTagPrefix = "danmaku-opacity",
            adjustedValue = { offset ->
                DanmakuSettingsPolicy.adjustOpacity(settings, offset).opacityPercent
            },
            onSelect = { onChange(settings.copy(opacityPercent = it)) },
        )
        DanmakuPercentageSetting(
            title = stringResource(R.string.danmaku_display_area),
            description = stringResource(R.string.danmaku_display_area_description),
            value = settings.displayAreaPercent,
            interactionsEnabled = interactionsEnabled,
            testTagPrefix = "danmaku-display-area",
            adjustedValue = { offset ->
                DanmakuSettingsPolicy.adjustDisplayArea(settings, offset)
                    .displayAreaPercent
            },
            onSelect = { onChange(settings.copy(displayAreaPercent = it)) },
        )
        ToggleSettingRow(
            title = stringResource(R.string.danmaku_merge_duplicates),
            description = stringResource(R.string.danmaku_merge_duplicates_description),
            checked = settings.mergeDuplicates,
            interactionsEnabled = interactionsEnabled,
            onToggle = {
                onChange(settings.copy(mergeDuplicates = !settings.mergeDuplicates))
            },
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
                    selectionIndicator = KaloscopeSelectionIndicatorType.Checkbox,
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
    interactionsEnabled: Boolean,
    testTagPrefix: String,
    adjustedValue: (Int) -> Int,
    onSelect: (Int) -> Unit,
) {
    val decreasedValue = adjustedValue(-1)
    val increasedValue = adjustedValue(1)
    AdjustableSettingRow(
        title = title,
        description = description,
        value = stringResource(R.string.percentage_value, value),
        interactionsEnabled = interactionsEnabled,
        canDecrease = decreasedValue != value,
        canIncrease = increasedValue != value,
        testTagPrefix = testTagPrefix,
        onDecrease = { onSelect(decreasedValue) },
        onIncrease = { onSelect(increasedValue) },
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
            descriptionColor = if (connection is SettingsConnection.Success) {
                Success
            } else {
                null
            },
            onClick = onTestConnection,
        )
        SettingActionRow(
            title = stringResource(R.string.manage_servers),
            description = stringResource(R.string.manage_servers_description),
            value = session.server.name,
            interactionsEnabled = interactionsEnabled,
            onClick = onManageServers,
        )
        SettingActionRow(
            title = stringResource(R.string.logout),
            description = stringResource(R.string.logout_description),
            value = session.user.username,
            interactionsEnabled = interactionsEnabled,
            titleColor = Danger,
            modifier = Modifier.focusRequester(logoutFocus),
            onClick = { onRequestLogout(logoutFocus) },
        )
    }
}
