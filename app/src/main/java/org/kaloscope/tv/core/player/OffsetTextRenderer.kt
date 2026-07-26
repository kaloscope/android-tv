package org.kaloscope.tv.core.player

import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererConfiguration
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId
import androidx.media3.exoplayer.source.SampleStream

@androidx.annotation.OptIn(UnstableApi::class)
internal class OffsetTextRenderer(
    private val delegate: Renderer,
    private val clock: SubtitleClock,
) : Renderer by delegate {
    private var appliedClockVersion = clock.version

    override fun enable(
        configuration: RendererConfiguration,
        formats: Array<out Format>,
        stream: SampleStream,
        positionUs: Long,
        joining: Boolean,
        mayRenderStartOfStream: Boolean,
        startPositionUs: Long,
        offsetUs: Long,
        mediaPeriodId: MediaPeriodId,
    ) {
        appliedClockVersion = clock.version
        delegate.enable(
            configuration,
            formats,
            stream,
            clock.adjustedPositionUs(positionUs),
            joining,
            mayRenderStartOfStream,
            clock.adjustedPositionUs(startPositionUs),
            offsetUs,
            mediaPeriodId,
        )
    }

    override fun replaceStream(
        formats: Array<out Format>,
        stream: SampleStream,
        startPositionUs: Long,
        offsetUs: Long,
        mediaPeriodId: MediaPeriodId,
    ) {
        appliedClockVersion = clock.version
        delegate.replaceStream(
            formats,
            stream,
            clock.adjustedPositionUs(startPositionUs),
            offsetUs,
            mediaPeriodId,
        )
    }

    override fun resetPosition(
        positionUs: Long,
        joining: Boolean,
    ) {
        appliedClockVersion = clock.version
        delegate.resetPosition(clock.adjustedPositionUs(positionUs), joining)
    }

    override fun render(
        positionUs: Long,
        elapsedRealtimeUs: Long,
    ) {
        val adjustedPositionUs = clock.adjustedPositionUs(positionUs)
        if (appliedClockVersion != clock.version) {
            // Reset only text state so changing subtitle offset never seeks audio or video.
            delegate.resetPosition(adjustedPositionUs, false)
            appliedClockVersion = clock.version
        }
        delegate.render(adjustedPositionUs, elapsedRealtimeUs)
    }
}
