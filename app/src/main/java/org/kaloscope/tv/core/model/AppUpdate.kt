package org.kaloscope.tv.core.model

data class AppUpdateRelease(
    val version: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val sha256: String?,
    val checksumUrl: String?,
)
