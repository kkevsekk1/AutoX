package org.autojs.autojs.core.model.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
