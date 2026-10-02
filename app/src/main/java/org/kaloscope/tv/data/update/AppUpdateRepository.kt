package org.kaloscope.tv.data.update

import java.io.File
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.AppUpdateRelease

interface AppUpdateRepository {
    suspend fun check(currentVersion: String): AppResult<AppUpdateRelease?>

    suspend fun download(
        release: AppUpdateRelease,
        onProgress: (Int) -> Unit,
    ): AppResult<File>
}
