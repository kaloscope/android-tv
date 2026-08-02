package org.kaloscope.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kaloscope.tv.core.model.NetworkDefinition

class NetworkDefinitionSelectionPolicyTest {
    @Test
    fun `software AVC devices prefer matching HEVC definition`() {
        val definitions = listOf(
            definition("480P 清晰", "avc1.640033"),
            definition("480P 清晰 HEVC", "hvc1.1.6.L120.90"),
            definition("360P 流畅 HEVC", "hvc1.1.6.L120.90"),
        )

        val selected = NetworkDefinitionSelectionPolicy.selectIndex(
            definitions = definitions,
            serverSelectedIndex = 0,
            preferHevc = true,
        )

        assertEquals(1, selected)
    }

    @Test
    fun `hardware AVC devices keep server selected definition`() {
        val definitions = listOf(
            definition("480P 清晰", "avc1.640033"),
            definition("480P 清晰 HEVC", "hvc1.1.6.L120.90"),
        )

        val selected = NetworkDefinitionSelectionPolicy.selectIndex(
            definitions = definitions,
            serverSelectedIndex = 0,
            preferHevc = false,
        )

        assertEquals(0, selected)
    }

    @Test
    fun `codec is detected from inline manifest when labels are identical`() {
        val definitions = listOf(
            definition("480P", "avc1.640033"),
            definition("480P", "hvc1.1.6.L120.90"),
        )

        val selected = NetworkDefinitionSelectionPolicy.selectIndex(
            definitions = definitions,
            serverSelectedIndex = 0,
            preferHevc = true,
        )

        assertEquals(1, selected)
    }

    @Test
    fun `HEVC preference requires a matching quality`() {
        val definitions = listOf(
            definition("480P 清晰", "avc1.640033"),
            definition("360P 流畅 HEVC", "hvc1.1.6.L120.90"),
        )

        val selected = NetworkDefinitionSelectionPolicy.selectIndex(
            definitions = definitions,
            serverSelectedIndex = 0,
            preferHevc = true,
        )

        assertEquals(0, selected)
    }

    @Test
    fun `codec capability policy requires HEVC and no hardware AVC decoder`() {
        assertTrue(
            NetworkVideoCodecPreferencePolicy.shouldPreferHevc(
                avcHardwareAcceleration = listOf(false),
                hasHevcDecoder = true,
            ),
        )
        assertFalse(
            NetworkVideoCodecPreferencePolicy.shouldPreferHevc(
                avcHardwareAcceleration = listOf(true, false),
                hasHevcDecoder = true,
            ),
        )
        assertFalse(
            NetworkVideoCodecPreferencePolicy.shouldPreferHevc(
                avcHardwareAcceleration = listOf(false),
                hasHevcDecoder = false,
            ),
        )
    }

    private fun definition(
        label: String,
        codec: String,
    ) = NetworkDefinition(
        label = label,
        url = """
            <MPD>
              <Representation codecs="$codec" />
            </MPD>
        """.trimIndent(),
    )
}
