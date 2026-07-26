package org.kaloscope.tv.core.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer

@androidx.annotation.OptIn(UnstableApi::class)
internal class PlaybackRenderersFactory(
    context: Context,
    private val subtitleClock: SubtitleClock,
) : DefaultRenderersFactory(context) {
    override fun buildTextRenderers(
        context: Context,
        output: TextOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>,
    ) {
        out += OffsetTextRenderer(
            delegate = TextRenderer(output, outputLooper),
            clock = subtitleClock,
        )
    }
}
