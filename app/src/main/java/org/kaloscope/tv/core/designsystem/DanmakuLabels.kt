package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.kaloscope.tv.R
import org.kaloscope.tv.core.model.DanmakuBlockPolicy
import org.kaloscope.tv.core.model.DanmakuBlockType
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize

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
internal fun danmakuBlockTypeLabel(type: DanmakuBlockType): String =
    when (type) {
        DanmakuBlockType.Scroll -> stringResource(R.string.danmaku_block_scroll)
        DanmakuBlockType.Top -> stringResource(R.string.danmaku_block_top)
        DanmakuBlockType.Bottom -> stringResource(R.string.danmaku_block_bottom)
        DanmakuBlockType.Colored -> stringResource(R.string.danmaku_block_colored)
    }

@Composable
internal fun danmakuBlockSummary(settings: DanmakuSettings): String {
    val selected = DanmakuBlockPolicy.selected(settings)
    return if (selected.isEmpty()) {
        stringResource(R.string.danmaku_block_none)
    } else {
        selected.map { danmakuBlockTypeLabel(it) }.joinToString("、")
    }
}
