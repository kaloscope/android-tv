package org.kaloscope.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.model.DanmakuSettings

@Composable
internal fun PlayerDanmakuBlockMenu(
    settings: DanmakuSettings,
    onChange: (DanmakuSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedOptions = PlayerDanmakuBlockPolicy.selected(settings).toSet()
    val firstOptionFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        firstOptionFocus.requestFocus()
    }
    BackHandler(onBack = onDismiss)
    Popup(
        alignment = Alignment.CenterEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(Modifier.testTag("player-settings-block-menu")) {
            PlayerDanmakuBlockOption.entries.forEachIndexed { index, option ->
                KaloscopeButton(
                    onClick = {
                        onChange(PlayerDanmakuBlockPolicy.toggle(settings, option))
                    },
                    selected = option in selectedOptions,
                    size = KaloscopeControlSize.Row,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player-settings-block-${option.name.lowercase()}")
                        .then(
                            if (index == 0) {
                                Modifier.focusRequester(firstOptionFocus)
                            } else {
                                Modifier
                            },
                        )
                        .focusProperties {
                            left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                            if (index == 0) up = FocusRequester.Cancel
                            if (index == PlayerDanmakuBlockOption.entries.lastIndex) {
                                down = FocusRequester.Cancel
                            }
                        },
                ) {
                    Text(playerDanmakuBlockLabel(option))
                }
            }
        }
    }
}

@Composable
internal fun playerDanmakuBlockLabel(option: PlayerDanmakuBlockOption): String =
    when (option) {
        PlayerDanmakuBlockOption.Scroll ->
            stringResource(R.string.danmaku_block_scroll)
        PlayerDanmakuBlockOption.Top ->
            stringResource(R.string.danmaku_block_top)
        PlayerDanmakuBlockOption.Bottom ->
            stringResource(R.string.danmaku_block_bottom)
        PlayerDanmakuBlockOption.Colored ->
            stringResource(R.string.danmaku_block_colored)
    }
