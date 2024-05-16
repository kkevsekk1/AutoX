package org.autojs.autojs.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout

val axClient = HttpClient(OkHttp) {
    install(HttpTimeout){
        requestTimeoutMillis
    }
    install(HttpRequestRetry){
        retryOnServerErrors(3)
        exponentialDelay()
    }
}