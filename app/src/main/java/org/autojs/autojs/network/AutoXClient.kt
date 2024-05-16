package org.autojs.autojs.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val axJson = Json{
    ignoreUnknownKeys = true
    prettyPrint = true
    coerceInputValues = true
}
internal val axClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(axJson)
    }
    install(HttpTimeout){
        requestTimeoutMillis
    }
    install(HttpRequestRetry){
        retryOnServerErrors(3)
        exponentialDelay()
    }
}