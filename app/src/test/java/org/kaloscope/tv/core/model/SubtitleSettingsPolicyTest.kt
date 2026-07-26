package org.kaloscope.tv.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleSettingsPolicyTest {
    @Test
    fun `subtitle values are clamped and rounded`() {
        val actual = SubtitleSettingsPolicy.sanitize(
            SubtitleSettings(
                timeOffsetSeconds = 3_600.08f,
                fontScalePercent = 203,
                verticalPositionPercent = -2,
            ),
        )

        assertEquals(3_600f, actual.timeOffsetSeconds)
        assertEquals(200, actual.fontScalePercent)
        assertEquals(0, actual.verticalPositionPercent)
    }

    @Test
    fun `subtitle values adjust with remote friendly steps`() {
        val initial = SubtitleSettings(
            timeOffsetSeconds = -0.1f,
            fontScalePercent = 100,
            verticalPositionPercent = 2,
        )

        assertEquals(
            105,
            SubtitleSettingsPolicy.adjustFontScale(initial, offset = 1).fontScalePercent,
        )
        assertEquals(
            1,
            SubtitleSettingsPolicy.adjustVerticalPosition(initial, offset = -1)
                .verticalPositionPercent,
        )
        assertEquals(
            0f,
            SubtitleSettingsPolicy.adjustTimeOffset(initial, offsetTenths = 1)
                .timeOffsetSeconds,
        )
    }
}
