package org.kaloscope.tv.data.update.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val assets: List<GitHubReleaseAssetDto>,
)

@Serializable
internal data class GitHubReleaseAssetDto(
    val name: String,
    val size: Long,
    val state: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val digest: String? = null,
)
