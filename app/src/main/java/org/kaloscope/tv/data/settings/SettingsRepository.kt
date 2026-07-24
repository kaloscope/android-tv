package org.kaloscope.tv.data.settings

import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.TvSettings

interface SettingsRepository {
    suspend fun getSettings(): AppResult<TvSettings>

    suspend fun saveSettings(settings: TvSettings): AppResult<TvSettings>
}
