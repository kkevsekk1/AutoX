package org.autojs.autojs.devplugin

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSockets

class WebSocketServer {

    var timeoutMillis = 10000L
    var pingInterval = 10000L

    private var engine: ApplicationEngine? = null
    var isActive: Boolean = false
        private set

    companion object {
        const val TAG = "WebSocketServer"
    }

    fun listen(
        port: Int,
        path: String,
        host: String = "0.0.0.0",
        onConnect: suspend DefaultWebSocketServerSession.() -> Unit = {},
    ) {
        engine = embeddedServer(Netty, port, host) {
            install(WebSockets) {
                pingPeriodMillis = this@WebSocketServer.pingInterval
                timeoutMillis = this@WebSocketServer.timeoutMillis
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket(path) {
                    val connectionPoint = this.call.mutableOriginConnectionPoint
                    Log.i(TAG, connectionPoint.remoteHost + ":" + connectionPoint.port)
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

    fun stop(gracePeriodMillis: Long = 0, timeoutMillis: Long = 0) {
        engine?.stop(gracePeriodMillis, timeoutMillis)
    }


}

class WebSocketClient {

    companion object {
        const val TAG = "WebSocketClient"
    }

    var socketTimeoutMillis = 10000L
    var pingInterval = 10000L
    private var client: HttpClient? = null

    suspend fun connect(
        url: String,
        connectTimeoutMillis: Long = 10000L,
        onConnect: suspend DefaultClientWebSocketSession.() -> Unit
    ) {
        client = HttpClient(OkHttp) {
            install(HttpTimeout) {
                this.connectTimeoutMillis = connectTimeoutMillis
                this.socketTimeoutMillis = this@WebSocketClient.socketTimeoutMillis
            }
            install(io.ktor.client.plugins.websocket.WebSockets) {
                this.pingInterval = this@WebSocketClient.pingInterval
                maxFrameSize = Long.MAX_VALUE
            }
        }
        client!!.webSocket(
            url
        ) {
            onConnect()
        }

    }

    fun close() {
        client?.close()
    }

}