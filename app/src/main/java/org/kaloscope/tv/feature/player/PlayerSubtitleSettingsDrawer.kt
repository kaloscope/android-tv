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
import java.util.Locale
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleSettingsPolicy
import org.kaloscope.tv.core.model.SubtitleTrack

@Composable
internal fun PlayerSubtitleSettingsDrawer(
    tracks: List<SubtitleTrack>,
    selectedTrackId: String?,
    settings: SubtitleSettings,
    onSelectTrack: (String?) -> Unit,
    onChangeSettings: (SubtitleSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    val offFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }
        if (selectedTrackId == null) {
            offFocus.requestFocus()
        } else {
            initialFocus.requestFocus()
        }
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
                .width(440.dp)
                .background(Panel.copy(alpha = 0.97f))
                .padding(horizontal = 28.dp, vertical = 34.dp),
        ) {
            Text(
                text = stringResource(R.string.subtitle_settings_title),
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.player_subtitle_temporary_description),
                color = Muted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SubtitleDrawerRow(
                        title = stringResource(R.string.player_subtitles_off_option),
                        value = "",
                        active = selectedTrackId == null,
                        onClick = { onSelectTrack(null) },
                        modifier = Modifier
                            .focusRequester(offFocus)
                            .focusProperties { up = FocusRequester.Cancel },
                    )
                }
                tracks.forEach { track ->
                    item(key = track.id) {
                        SubtitleDrawerRow(
                            title = track.label,
                            value = track.language.orEmpty(),
                            active = track.id == selectedTrackId,
                            onClick = { onSelectTrack(track.id) },
                            modifier = if (track.id == selectedTrackId) {
                                Modifier.focusRequester(initialFocus)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
                item {
                    SubtitleDrawerChoiceRow(
                        title = stringResource(R.string.subtitle_display_mode),
                        values = SubtitleDisplayMode.entries,
                        selected = settings.displayMode,
                        label = {
                            when (it) {
                                SubtitleDisplayMode.Stroke ->
                                    stringResource(R.string.subtitle_display_mode_stroke)

                                SubtitleDisplayMode.Background ->
                                    stringResource(R.string.subtitle_display_mode_background)
                            }
                        },
                        onSelect = {
                            onChangeSettings(settings.copy(displayMode = it))
                        },
                    )
                }
                item {
                    SubtitleDrawerAdjustRow(
                        title = stringResource(R.string.subtitle_font_scale),
                        value = stringResource(
                            R.string.percentage_value,
                            settings.fontScalePercent,
                        ),
                        onDecrease = {
                            onChangeSettings(
                                SubtitleSettingsPolicy.adjustFontScale(settings, -1),
                            )
                        },
                        onIncrease = {
                            onChangeSettings(
                                SubtitleSettingsPolicy.adjustFontScale(settings, 1),
                            )
                        },
                    )
                }
                item {
                    SubtitleDrawerAdjustRow(
                        title = stringResource(R.string.subtitle_vertical_position),
                        value = stringResource(
                            R.string.percentage_value,
                            settings.verticalPositionPercent,
                        ),
                        onDecrease = {
                            onChangeSettings(
                                SubtitleSettingsPolicy.adjustVerticalPosition(settings, -1),
                            )
                        },
                        onIncrease = {
                            onChangeSettings(
                                SubtitleSettingsPolicy.adjustVerticalPosition(settings, 1),
                            )
                        },
                    )
                }
                item {
                    SubtitleDrawerAdjustRow(
                        title = stringResource(R.string.subtitle_time_offset),
                        value = formatSubtitleDrawerOffset(settings.timeOffsetSeconds),
                        onDecrease = {
                            onChangeSettings(
                                SubtitleSettingsPolicy.adjustTimeOffset(settings, -1),
                            )
                        },
                        onIncrease = {
                            onChangeSettings(
                                SubtitleSettingsPolicy.adjustTimeOffset(settings, 1),
                            )
                        },
                        onClick = {
                            onChangeSettings(settings.copy(timeOffsetSeconds = 0f))
                        },
                        modifier = Modifier.focusProperties {
                            down = FocusRequester.Cancel
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> SubtitleDrawerChoiceRow(
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
    SubtitleDrawerAdjustRow(
        title = title,
        value = label(selected),
        onDecrease = { selectOffset(-1) },
        onIncrease = { selectOffset(1) },
        onClick = { selectOffset(1) },
    )
}

@Composable
private fun SubtitleDrawerAdjustRow(
    title: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SubtitleDrawerRow(
        title = title,
        value = "‹  $value  ›",
        onClick = onClick,
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) {
                return@onPreviewKeyEvent false
            }
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
private fun SubtitleDrawerRow(
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
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (value.isNotBlank()) {
                Text(value, color = Muted, fontSize = 13.sp)
            }
        }
    }
}

private fun formatSubtitleDrawerOffset(value: Float): String =
    if (value == 0f) {
        "0.0s"
    } else {
        String.format(Locale.US, "%+.1fs", value)
    }
