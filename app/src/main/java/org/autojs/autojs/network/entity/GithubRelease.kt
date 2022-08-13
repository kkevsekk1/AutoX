package org.autojs.autojs.network.entity

import com.google.gson.annotations.SerializedName
import org.autojs.autoxjs.BuildConfig

class GithubReleaseInfoList : ArrayList<GithubReleaseInfo>()

data class GithubReleaseInfo(
    @SerializedName("assets")
    val assets: List<Asset>,
    @SerializedName("assets_url")
    val assetsUrl: String,
    @SerializedName("author")
    val author: Author,
    @SerializedName("body")
    val body: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("draft")
    val draft: Boolean,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("mentions_count")
    val mentionsCount: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("node_id")
    val nodeId: String,
    @SerializedName("prerelease")
    val prerelease: Boolean,
    @SerializedName("published_at")
    val publishedAt: String,
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("tarball_url")
    val tarballUrl: String,
    @SerializedName("target_commitish")
    val targetCommitish: String,
    @SerializedName("upload_url")
    val uploadUrl: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("zipball_url")
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

data class Asset(
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("download_count")
    val downloadCount: Int,
    @SerializedName("id")
    val id: Int,
    @SerializedName("label")
    val label: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("node_id")
    val nodeId: String,
    @SerializedName("size")
    val size: Int,
    @SerializedName("state")
    val state: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("uploader")
    val uploader: Uploader,
    @SerializedName("url")
    val url: String
)

data class Author(
    @SerializedName("avatar_url")
    val avatarUrl: String,
    @SerializedName("events_url")
    val eventsUrl: String,
    @SerializedName("followers_url")
    val followersUrl: String,
    @SerializedName("following_url")
    val followingUrl: String,
    @SerializedName("gists_url")
    val gistsUrl: String,
    @SerializedName("gravatar_id")
    val gravatarId: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("login")
    val login: String,
    @SerializedName("node_id")
    val nodeId: String,
    @SerializedName("organizations_url")
    val organizationsUrl: String,
    @SerializedName("received_events_url")
    val receivedEventsUrl: String,
    @SerializedName("repos_url")
    val reposUrl: String,
    @SerializedName("site_admin")
    val siteAdmin: Boolean,
    @SerializedName("starred_url")
    val starredUrl: String,
    @SerializedName("subscriptions_url")
    val subscriptionsUrl: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("url")
    val url: String
)

data class Uploader(
    @SerializedName("avatar_url")
    val avatarUrl: String,
    @SerializedName("events_url")
    val eventsUrl: String,
    @SerializedName("followers_url")
    val followersUrl: String,
    @SerializedName("following_url")
    val followingUrl: String,
    @SerializedName("gists_url")
    val gistsUrl: String,
    @SerializedName("gravatar_id")
    val gravatarId: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("login")
    val login: String,
    @SerializedName("node_id")
    val nodeId: String,
    @SerializedName("organizations_url")
    val organizationsUrl: String,
    @SerializedName("received_events_url")
    val receivedEventsUrl: String,
    @SerializedName("repos_url")
    val reposUrl: String,
    @SerializedName("site_admin")
    val siteAdmin: Boolean,
    @SerializedName("starred_url")
    val starredUrl: String,
    @SerializedName("subscriptions_url")
    val subscriptionsUrl: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("url")
    val url: String
)