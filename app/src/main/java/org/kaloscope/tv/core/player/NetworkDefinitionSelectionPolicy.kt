package org.kaloscope.tv.core.player

import org.kaloscope.tv.core.model.NetworkDefinition

internal object NetworkDefinitionSelectionPolicy {
    fun selectIndex(
        definitions: List<NetworkDefinition>,
        serverSelectedIndex: Int?,
        preferHevc: Boolean,
    ): Int? {
        val selectedIndex = serverSelectedIndex
            ?.takeIf(definitions.indices::contains)
            ?: definitions.indices.firstOrNull()
            ?: return null
        if (!preferHevc || definitions[selectedIndex].codec() == VideoCodec.Hevc) {
            return selectedIndex
        }
        val selectedQuality = definitions[selectedIndex].label.compatibilityKey()
        return definitions.indices.firstOrNull { index ->
            definitions[index].codec() == VideoCodec.Hevc &&
                definitions[index].label.compatibilityKey() == selectedQuality
        } ?: selectedIndex
    }

    private fun NetworkDefinition.codec(): VideoCodec {
        val normalizedLabel = label.lowercase().filter(Char::isLetterOrDigit)
        return when {
            normalizedLabel.contains("hevc") || normalizedLabel.contains("h265") ->
                VideoCodec.Hevc

            normalizedLabel.contains("avc") || normalizedLabel.contains("h264") ->
                VideoCodec.Avc

            HEVC_MANIFEST_CODEC.containsMatchIn(url) -> VideoCodec.Hevc
            AVC_MANIFEST_CODEC.containsMatchIn(url) -> VideoCodec.Avc
            else -> VideoCodec.Unknown
        }
    }

    private fun String.compatibilityKey(): String =
        CODEC_MARKERS.fold(lowercase().filter(Char::isLetterOrDigit)) { value, marker ->
            value.replace(marker, "")
        }

    private enum class VideoCodec {
        Avc,
        Hevc,
        Unknown,
    }

    private val CODEC_MARKERS = listOf("hevc", "h265", "avc", "h264")
    private val HEVC_MANIFEST_CODEC = Regex(
        pattern = """codecs\s*=\s*[\"'][^\"']*(?:hev1|hvc1)""",
        option = RegexOption.IGNORE_CASE,
    )
    private val AVC_MANIFEST_CODEC = Regex(
        pattern = """codecs\s*=\s*[\"'][^\"']*avc1""",
        option = RegexOption.IGNORE_CASE,
    )
}

internal object NetworkVideoCodecPreferencePolicy {
    // Some DASH AVC streams are accepted by software codecs but then stop producing output.
    // Prefer an equivalent HEVC stream only when no hardware AVC path is available.
    fun shouldPreferHevc(
        avcHardwareAcceleration: List<Boolean>,
        hasHevcDecoder: Boolean,
    ): Boolean = hasHevcDecoder && avcHardwareAcceleration.none { it }
}
