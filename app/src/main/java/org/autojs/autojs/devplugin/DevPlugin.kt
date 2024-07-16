package org.autojs.autojs.devplugin

import android.util.Log
import com.stardust.app.GlobalAppContext
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.core.network.socket.State
import org.autojs.autoxjs.R

object DevPlugin {
    const val SERVER_PORT = 9317
    private const val TAG = "DevPlugin"

    private val _connectState = MutableSharedFlow<State>()
    private val client by lazy { WebSocketClient() }
    private val server by lazy { WebSocketServer() }

    private var connection: Connection? = null
    val isActive get() = connection?.isActive ?: false

    val connectState = _connectState.asSharedFlow()
    val isUSBDebugServiceActive get() = server.isActive

    init {
        CoroutineScope(Dispatchers.IO).launch {
            collect()
        }
    }

    suspend fun connect(url: String) {
        withContext(Dispatchers.IO) {
            emitState(State(State.CONNECTING))
            try {
                client.connect(url) { newConnection(this, url) }
            } catch (e: Exception) {
                emitState(State(State.CONNECTION_FAILED, e))
                client.close()
                e.printStackTrace()
            }
        }
    }

    fun log(log: String) {
        connection?.log(log)
    }

    private suspend fun collect() {
        connectState.collect {
            when (it.state) {
                State.CONNECTING -> {
                    Log.d(TAG, "ConnectComputerSwitch: CONNECTING")
                    GlobalAppContext.toast(R.string.text_connecting)
                }

                State.RECONNECTING -> {
                    Log.d(TAG, "ConnectComputerSwitch: RECONNECTING")
                    GlobalAppContext.toast(R.string.text_reconnecting)
                }

                State.CONNECTION_FAILED -> {
                    Log.d(TAG, "ConnectComputerSwitch: CONNECTION_FAILED")
                    GlobalAppContext.toast(
                        true,
                        R.string.text_connect_failed,
                        it.e?.localizedMessage ?: ""
                    )
                    it.e?.printStackTrace()
                }

                State.HANDSHAKE_TIMEOUT -> {
                    GlobalAppContext.toast(R.string.text_handshake_failed)
                }
            }
        }
    }

    suspend fun startUSBDebug() {
        withContext(Dispatchers.IO) {
            server.listen(SERVER_PORT, "/", host = "127.0.0.1") {
                newConnection(this)
            }
        }
    }

    suspend fun stopUSBDebug() {
        withContext(Dispatchers.IO) {
            server.stop()
        }
    }

    suspend fun close() = connection?.close()

    suspend fun newConnection(session: WebSocketSession, serverUrl: String? = null) {
        val connection = Connection(session, serverUrl, client)
        this@DevPlugin.connection = connection
        connection.init()
    }

    suspend fun emitState(state: State) = _connectState.emit(state)
}