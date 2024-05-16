package org.autojs.autojs.network.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.autojs.autoxjs.BuildConfig

@Serializable
data class GithubReleaseInfo(
    @SerialName("assets")
    val assets: List<Asset>,
    @SerialName("assets_url")
    val assetsUrl: String,
    @SerialName("author")
    val author: Author,
    @SerialName("body")
    val body: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("draft")
    val draft: Boolean,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("id")
    val id: Int,
    @SerialName("mentions_count")
    val mentionsCount: Int,
    @SerialName("name")
    val name: String,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("prerelease")
    val prerelease: Boolean,
    @SerialName("published_at")
    val publishedAt: String,
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("tarball_url")
    val tarballUrl: String,
    @SerialName("target_commitish")
    val targetCommitish: String,
    @SerialName("upload_url")
    val uploadUrl: String,
    @SerialName("url")
    val url: String,
    @SerialName("zipball_url")
    val zipballUrl: String
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
    @SerialName("content_type")
    val contentType: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("download_count")
    val downloadCount: Int,
    @SerialName("id")
    val id: Int,
    @SerialName("label")
    val label: String,
    @SerialName("name")
    val name: String,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("size")
    val size: Int,
    @SerialName("state")
    val state: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("uploader")
    val uploader: Uploader,
    @SerialName("url")
    val url: String
)

@Serializable
data class Author(
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("events_url")
    val eventsUrl: String,
    @SerialName("followers_url")
    val followersUrl: String,
    @SerialName("following_url")
    val followingUrl: String,
    @SerialName("gists_url")
    val gistsUrl: String,
    @SerialName("gravatar_id")
    val gravatarId: String,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("id")
    val id: Int,
    @SerialName("login")
    val login: String,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("organizations_url")
    val organizationsUrl: String,
    @SerialName("received_events_url")
    val receivedEventsUrl: String,
    @SerialName("repos_url")
    val reposUrl: String,
    @SerialName("site_admin")
    val siteAdmin: Boolean,
    @SerialName("starred_url")
    val starredUrl: String,
    @SerialName("subscriptions_url")
    val subscriptionsUrl: String,
    @SerialName("type")
    val type: String,
    @SerialName("url")
    val url: String
)

@Serializable
data class Uploader(
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("events_url")
    val eventsUrl: String,
    @SerialName("followers_url")
    val followersUrl: String,
    @SerialName("following_url")
    val followingUrl: String,
    @SerialName("gists_url")
    val gistsUrl: String,
    @SerialName("gravatar_id")
    val gravatarId: String,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("id")
    val id: Int,
    @SerialName("login")
    val login: String,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("organizations_url")
    val organizationsUrl: String,
    @SerialName("received_events_url")
    val receivedEventsUrl: String,
    @SerialName("repos_url")
    val reposUrl: String,
    @SerialName("site_admin")
    val siteAdmin: Boolean,
    @SerialName("starred_url")
    val starredUrl: String,
    @SerialName("subscriptions_url")
    val subscriptionsUrl: String,
    @SerialName("type")
    val type: String,
    @SerialName("url")
    val url: String
)