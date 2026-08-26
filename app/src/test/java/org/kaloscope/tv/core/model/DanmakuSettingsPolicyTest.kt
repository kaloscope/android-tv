package org.kaloscope.tv.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuSettingsPolicyTest {
    @Test
    fun `opacity adjusts by five percent and stops at twenty and one hundred`() {
        val minimum = DanmakuSettings(opacityPercent = 20)
        val middle = DanmakuSettings(opacityPercent = 50)
        val maximum = DanmakuSettings(opacityPercent = 100)

        assertEquals(
            20,
            DanmakuSettingsPolicy.adjustOpacity(minimum, offset = -1).opacityPercent,
        )
        assertEquals(
            45,
            DanmakuSettingsPolicy.adjustOpacity(middle, offset = -1).opacityPercent,
        )
        assertEquals(
            55,
            DanmakuSettingsPolicy.adjustOpacity(middle, offset = 1).opacityPercent,
        )
        assertEquals(
            100,
            DanmakuSettingsPolicy.adjustOpacity(maximum, offset = 1).opacityPercent,
        )
    }

    @Test
    fun `display area adjusts by five percent and stops at twenty and one hundred`() {
        val minimum = DanmakuSettings(displayAreaPercent = 20)
        val middle = DanmakuSettings(displayAreaPercent = 75)
        val maximum = DanmakuSettings(displayAreaPercent = 100)

        assertEquals(
            20,
            DanmakuSettingsPolicy.adjustDisplayArea(minimum, offset = -1)
                .displayAreaPercent,
        )
        assertEquals(
            70,
            DanmakuSettingsPolicy.adjustDisplayArea(middle, offset = -1)
                .displayAreaPercent,
        )
        assertEquals(
            80,
            DanmakuSettingsPolicy.adjustDisplayArea(middle, offset = 1)
                .displayAreaPercent,
        )
        assertEquals(
            100,
            DanmakuSettingsPolicy.adjustDisplayArea(maximum, offset = 1)
                .displayAreaPercent,
        )
    }

    @Test
    fun `invalid persisted percentage falls back to the supplied default`() {
        assertEquals(20, DanmakuSettingsPolicy.validPercentage(20, fallback = 75))
        assertEquals(55, DanmakuSettingsPolicy.validPercentage(55, fallback = 100))
        assertEquals(100, DanmakuSettingsPolicy.validPercentage(41, fallback = 100))
        assertEquals(75, DanmakuSettingsPolicy.validPercentage(null, fallback = 75))
    }
}
