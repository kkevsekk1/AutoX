package org.autojs.autojs.core.model.devplugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message<T>(
    @SerialName("type")
    val type: String,
    @SerialName("data")
    val data: T,
    @SerialName("md5")
    val md5: String = "",
    @SerialName("message_id")
    val messageId: String = "",
)
