package org.autojs.autojs.core.model.devplugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogData(
    @SerialName("log")
    val log: String
)
