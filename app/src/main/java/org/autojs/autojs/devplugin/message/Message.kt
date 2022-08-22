package org.autojs.autojs.devplugin.message

import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("type")
    val type: String,
    @SerializedName("data")
    val data: Any?,
)

data class HelloResponse(
    @SerializedName("data")
    val data: String,
    @SerializedName("debug")
    val debug: Boolean,
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("version")
    val version: String? = "0"
) {
    fun versionCode(): Long {
        return version?.replace(".", "")?.toLongOrNull() ?: -1L
    }
}

data class LogData(
    @SerializedName("log")
    val log: String
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