package org.autojs.autojs.core.model.devplugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Hello(
    @SerialName("device_name")
    val deviceName: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("app_version_code")
    val appVersionCode: Int,
    @SerialName("client_version")
    val clientVersion: Int,
)
