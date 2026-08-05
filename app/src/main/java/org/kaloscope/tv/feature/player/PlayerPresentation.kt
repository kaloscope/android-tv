package org.kaloscope.tv.feature.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.player.PlaybackRequest

internal fun PlaybackRequest.playbackIdentity(): String =
    when (this) {
        is PlaybackRequest.LocalMedia -> "$requestId:local:$mediaId"
        is PlaybackRequest.NetworkVideo ->
            "$requestId:network:${source.resourceId}:${source.selectedChapterIndex}:${source.url}"
    }

internal fun playerQualityControlLabel(
    playbackModeLabel: String,
    selectedDefinitionLabel: String?,
): String =
    selectedDefinitionLabel
        ?.takeIf(String::isNotBlank)
        ?: playbackModeLabel

@androidx.annotation.OptIn(UnstableApi::class)
internal fun SubtitleView.applySubtitleStyle(settings: SubtitleSettings) {
    setApplyEmbeddedStyles(false)
    setApplyEmbeddedFontSizes(false)
    setFractionalTextSize(
        SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * settings.fontScalePercent / 100f,
    )
    setBottomPaddingFraction(settings.verticalPositionPercent / 100f)
    setStyle(
        when (settings.displayMode) {
            SubtitleDisplayMode.Stroke -> CaptionStyleCompat(
                android.graphics.Color.WHITE,
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                android.graphics.Color.BLACK,
                null,
            )

            SubtitleDisplayMode.Background -> CaptionStyleCompat(
                android.graphics.Color.WHITE,
                0xB3000000.toInt(),
                android.graphics.Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_NONE,
                android.graphics.Color.TRANSPARENT,
                null,
            )
        },
    )
}
