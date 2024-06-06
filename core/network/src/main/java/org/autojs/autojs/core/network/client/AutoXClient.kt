package org.autojs.autojs.core.network.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import org.autojs.autojs.core.network.json
import org.autojs.autojs.core.network.okHttpClient

internal val client = HttpClient(
    OkHttp.create {
        preconfigured = okHttpClient
    }
) {
    Logging()
    WebSockets {
        contentConverter = KotlinxWebsocketSerializationConverter(json)
        pingInterval = 10000
        maxFrameSize = Long.MAX_VALUE
    }
    install(ContentNegotiation) {
        json(json)
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(3)
        exponentialDelay()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 10000
        connectTimeoutMillis = 10000
        socketTimeoutMillis = 10000
    }
}