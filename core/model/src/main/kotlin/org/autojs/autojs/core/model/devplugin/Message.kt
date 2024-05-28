package org.autojs.autojs.core.model.devplugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message<T>(
    @SerialName("type")
    val type: String,
    @SerialName("data")
    val data: T,
)
