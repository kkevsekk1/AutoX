package org.autojs.autojs.network.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.autojs.autoxjs.BuildConfig

@Serializable
data class GithubReleaseInfo(
    @SerialName("assets")
    val assets: List<Asset>,
    @SerialName("body")
    val body: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("name")
    val name: String,
    @SerialName("prerelease")
    val prerelease: Boolean,
    @SerialName("target_commitish")
    val targetCommitish: String,
)

/**
 * Check latest version
 * @return Returns null if targetCommitish is prerelease or not "dev-test"
 */
fun GithubReleaseInfo.isLatestVersion(): Boolean? {
    if (targetCommitish != "dev-test" || prerelease) return null
    return this.name.getVersionByName() <= BuildConfig.VERSION_NAME.getVersionByName()
}

private fun String.getVersionByName(): Long {
    return this.replace(".", "").toLongOrNull() ?: -1
}

@Serializable
data class Asset(
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    @SerialName("name")
    val name: String,
)
