package org.autojs.autojs.core.model.devplugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HelloResponse(
    @SerialName("data")
    val data: String,
    @SerialName("debug")
    val debug: Boolean,
    @SerialName("message_id")
    val messageId: String,
    @SerialName("type")
    val type: String,
    @SerialName("version")
    val version: String = "0"
) {
    fun versionCode(): Long {
        return version.replace(".", "").toLongOrNull() ?: -1L
    }
}
