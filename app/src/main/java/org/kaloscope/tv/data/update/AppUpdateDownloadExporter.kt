package org.kaloscope.tv.data.update

import java.io.File
import org.kaloscope.tv.core.common.AppResult

interface AppUpdateDownloadExporter {
    suspend fun save(apk: File, version: String): AppResult<String>
}
