package org.autojs.autojs.core.model.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Asset(
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    @SerialName("name")
    val name: String,
)