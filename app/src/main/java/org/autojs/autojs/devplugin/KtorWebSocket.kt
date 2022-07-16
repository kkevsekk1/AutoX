package org.autojs.autojs.devplugin

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSockets

class WebSocketServer {

    private var engine: ApplicationEngine? = null
    var isActive: Boolean = false
        private set

    fun listen(
        port: Int,
        path: String,
        host: String = "0.0.0.0",
        onConnect: suspend DefaultWebSocketServerSession.() -> Unit = {},
    ) {
        engine = embeddedServer(Netty, port, host) {
            install(WebSockets) {
                pingPeriodMillis = 10 * 1000
                timeoutMillis = 10 * 1000
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket(path) {
                    Log.d("TAG", "listenOnce: onConnect")
                    onConnect()
                }
            }
        }
        engine!!.environment.monitor.apply {
            subscribe(ApplicationStarted) {
                // Handle the event using the application as subject
                isActive = true
            }
            subscribe(ApplicationStopped) {
                // Handle the event using the application as subject
                isActive = false
            }
        }
        engine!!.start(wait = false)
    }

    fun stop() {
        engine?.stop()
    }


}

class WebSocketClient {

    private var client: HttpClient? = null

    suspend fun connect(
        url: String,
        onConnect: suspend DefaultClientWebSocketSession.() -> Unit
    ) {
        client = HttpClient(OkHttp) {
            install(io.ktor.client.plugins.websocket.WebSockets) {
                maxFrameSize = Long.MAX_VALUE
            }
        }
        client!!.webSocket(
            url,
        ) {
            onConnect()
        }

    }

    fun close() {
        client?.close()
    }

}