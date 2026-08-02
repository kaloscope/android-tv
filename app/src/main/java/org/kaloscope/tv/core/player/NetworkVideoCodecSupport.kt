package org.kaloscope.tv.core.player

import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import javax.inject.Inject
import javax.inject.Singleton

fun interface NetworkVideoCodecSupport {
    fun shouldPreferHevcForDash(): Boolean

    companion object {
        val KeepServerOrder = NetworkVideoCodecSupport { false }
    }
}

@Singleton
@androidx.annotation.OptIn(UnstableApi::class)
class AndroidNetworkVideoCodecSupport @Inject constructor() : NetworkVideoCodecSupport {
    private val preferHevc by lazy {
        try {
            val avcDecoders = MediaCodecUtil.getDecoderInfos(
                MimeTypes.VIDEO_H264,
                /* requiresSecureDecoder = */ false,
                /* requiresTunnelingDecoder = */ false,
            )
            val hevcDecoders = MediaCodecUtil.getDecoderInfos(
                MimeTypes.VIDEO_H265,
                /* requiresSecureDecoder = */ false,
                /* requiresTunnelingDecoder = */ false,
            )
            NetworkVideoCodecPreferencePolicy.shouldPreferHevc(
                avcHardwareAcceleration = avcDecoders.map { it.hardwareAccelerated },
                hasHevcDecoder = hevcDecoders.isNotEmpty(),
            )
        } catch (_: MediaCodecUtil.DecoderQueryException) {
            false
        }
    }

    override fun shouldPreferHevcForDash(): Boolean = preferHevc
}
