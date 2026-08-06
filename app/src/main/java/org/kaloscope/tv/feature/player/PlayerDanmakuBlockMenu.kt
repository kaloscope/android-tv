package org.kaloscope.tv.feature.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialog
import org.kaloscope.tv.core.designsystem.KaloscopeChoiceDialogOption
import org.kaloscope.tv.core.designsystem.danmakuBlockTypeLabel
import org.kaloscope.tv.core.model.DanmakuBlockPolicy
import org.kaloscope.tv.core.model.DanmakuBlockType
import org.kaloscope.tv.core.model.DanmakuSettings

@Composable
internal fun PlayerDanmakuBlockMenu(
    settings: DanmakuSettings,
    onChange: (DanmakuSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        KaloscopeChoiceDialog(
            title = stringResource(R.string.danmaku_block_types),
            options = DanmakuBlockType.entries.map { type ->
                KaloscopeChoiceDialogOption(
                    label = danmakuBlockTypeLabel(type),
                    selected = { DanmakuBlockPolicy.isSelected(settings, type) },
                    testTag = "player-settings-block-${type.name.lowercase()}",
                    onSelect = {
                        onChange(DanmakuBlockPolicy.toggle(settings, type))
                    },
                )
            },
            viewportSize = DpSize(maxWidth, maxHeight),
            onDismiss = onDismiss,
            dismissOnSelect = false,
        )
    }
}
