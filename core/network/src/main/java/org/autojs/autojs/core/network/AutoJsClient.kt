package org.autojs.autojs.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

internal val client = HttpClient(
    OkHttp.create {
        preconfigured = okHttpClient
    }
) {
    install(ContentNegotiation) {
        json(json)
    }
    install(HttpTimeout)
    install(HttpRequestRetry) {
        retryOnServerErrors(3)
        exponentialDelay()
    }
}