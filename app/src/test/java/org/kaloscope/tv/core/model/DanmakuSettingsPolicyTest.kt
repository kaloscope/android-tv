package org.kaloscope.tv.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuSettingsPolicyTest {
    @Test
    fun `percentages use the canonical ordered values`() {
        assertEquals(listOf(25, 50, 75, 100), DanmakuSettingsPolicy.PERCENTAGE_VALUES)
    }

    @Test
    fun `opacity adjusts one value at a time and stops at boundaries`() {
        val minimum = DanmakuSettings(opacityPercent = 25)
        val middle = DanmakuSettings(opacityPercent = 50)
        val maximum = DanmakuSettings(opacityPercent = 100)

        assertEquals(
            25,
            DanmakuSettingsPolicy.adjustOpacity(minimum, offset = -1).opacityPercent,
        )
        assertEquals(
            75,
            DanmakuSettingsPolicy.adjustOpacity(middle, offset = 1).opacityPercent,
        )
        assertEquals(
            100,
            DanmakuSettingsPolicy.adjustOpacity(maximum, offset = 1).opacityPercent,
        )
    }

    @Test
    fun `display area adjusts one value at a time and stops at boundaries`() {
        val minimum = DanmakuSettings(displayAreaPercent = 25)
        val middle = DanmakuSettings(displayAreaPercent = 75)
        val maximum = DanmakuSettings(displayAreaPercent = 100)

        assertEquals(
            25,
            DanmakuSettingsPolicy.adjustDisplayArea(minimum, offset = -1)
                .displayAreaPercent,
        )
        assertEquals(
            50,
            DanmakuSettingsPolicy.adjustDisplayArea(middle, offset = -1)
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
        assertEquals(100, DanmakuSettingsPolicy.validPercentage(41, fallback = 100))
        assertEquals(75, DanmakuSettingsPolicy.validPercentage(null, fallback = 75))
    }
}
