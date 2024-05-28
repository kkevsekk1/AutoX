package org.autojs.autojs.core.model.devplugin
import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName

@Serializable
data class Command(
    @SerialName("command")
    val command: String = "",
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = ""
)
