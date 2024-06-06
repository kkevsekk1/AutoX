package org.autojs.autojs.core.network

import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    coerceInputValues = true
}
