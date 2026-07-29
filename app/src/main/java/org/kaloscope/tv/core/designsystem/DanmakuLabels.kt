package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.kaloscope.tv.R
import org.kaloscope.tv.core.model.DanmakuDisplayMode
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
internal fun danmakuModeLabel(mode: DanmakuDisplayMode): String =
    when (mode) {
        DanmakuDisplayMode.Scroll -> stringResource(R.string.danmaku_mode_scroll)
        DanmakuDisplayMode.Top -> stringResource(R.string.danmaku_mode_top)
        DanmakuDisplayMode.Bottom -> stringResource(R.string.danmaku_mode_bottom)
    }
