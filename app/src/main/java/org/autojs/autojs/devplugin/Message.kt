package org.autojs.autojs.devplugin

import com.google.gson.annotations.SerializedName

data class Message(
    val type: String,
    val data: Any,
)

data class Hello(
    @SerializedName("device_name")
    val deviceName: String,
    @SerializedName("app_version")
    val appVersion: String,
    @SerializedName("app_version_code")
    val appVersionCode: Int,
    @SerializedName("client_version")
    val clientVersion: Int,
)