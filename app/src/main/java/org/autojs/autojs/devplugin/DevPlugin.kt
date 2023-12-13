package org.autojs.autojs.devplugin

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.stardust.app.GlobalAppContext
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.autojs.autojs.devplugin.message.Hello
import org.autojs.autojs.devplugin.message.HelloResponse
import org.autojs.autojs.devplugin.message.LogData
import org.autojs.autojs.devplugin.message.Message
import org.autojs.autoxjs.BuildConfig
import org.autojs.autoxjs.R
import java.io.File
import java.net.SocketTimeoutException

object DevPlugin {

    data class State(val state: Int, val e: Throwable? = null) {
        companion object {
            const val DISCONNECTED = 0
            const val CONNECTING = 1
            const val CONNECTED = 2
            const val CONNECTION_FAILED = 3
            const val RECONNECTING = 4
            const val HANDSHAKE_TIMEOUT = 5
        }
    }

    private val gson get() = Gson()
    const val SERVER_PORT = 9317
    private const val CLIENT_VERSION = 2
    private const val TAG = "DevPlugin"
    private const val TYPE_HELLO = "hello"
    private const val TYPE_PING = "ping"
    private const val TYPE_PONG = "pong"
    private const val TYPE_CLOSE = "close"
    private const val TYPE_BYTES_COMMAND = "bytes_command"
    private const val maxRetry = 3

    private val _connectState = MutableSharedFlow<State>()
    private val client by lazy { WebSocketClient() }
    private val server by lazy { WebSocketServer() }

    private var connection: Connection? = null
    val isActive get() = connection?.isActive ?: false
    private val bytesMap = HashMap<String, Bytes>()
    private val requiredBytesCommands = HashMap<String, JsonObject>()
    private val responseHandler: DevPluginResponseHandler by lazy {
        val cache = File(GlobalAppContext.get().cacheDir, "remote_project")
        DevPluginResponseHandler(cache)
    }


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
                client.connect(
                    url
                ) {
                    newConnection(this, url)
                }
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

    suspend fun collect() {
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

    class Connection(
        private val session: WebSocketSession,
        private var serverUrl: String? = null
    ) {
        private var lastPongId = -1L
        val isActive get() = session.isActive

        suspend fun init() {
            session.shakeHandsAndHandle()
        }

        private suspend fun WebSocketSession.shakeHandsAndHandle() {
            shakeHands {
                handle()
            }
        }

        suspend fun WebSocketSession.handle() {
            emitState(State(State.CONNECTED))
            withContext(Dispatchers.IO) {
                launch {
                    mapToData(
                        onJson = {
                            onJsonData(it)
                        },
                        onBytes = {
                            onBytesData(it)
                        }
                    )
                }
            }
        }

        private suspend fun WebSocketSession.shakeHands(onSuccess: suspend () -> Unit) {
            val message = Message(
                type = TYPE_HELLO,
                data = Hello(
                    deviceName = Build.BRAND + " " + Build.MODEL,
                    clientVersion = CLIENT_VERSION,
                    appVersion = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE
                )
            )
            send(gson.toJson(message))
            val frame = incoming.receive()
            serveHello(
                frame = frame,
                ok = { resp ->
                    coroutineScope {
                        launch { onSuccess() }
                        if (resp.versionCode() >= 11090) {
                            launch { ping() }
                        }
                    }
                },
                fail = {
                    onHandshakeTimeout()
                }
            )
        }

        private suspend fun onJsonData(element: JsonElement) {
            if (!element.isJsonObject) {
                Log.w(TAG, "onSocketData: not json object: $element")
                return
            }
            try {
                val obj = element.asJsonObject
                val type = obj["type"] ?: return
                if (!type.isJsonPrimitive) return
                when (type.asString) {
                    TYPE_PONG -> {
                        lastPongId = obj["data"].asLong
                    }
                    TYPE_CLOSE -> {
                        this.close()
                    }
                    TYPE_BYTES_COMMAND -> {
                        val md5 = obj["md5"].asString
                        bytesMap.remove(md5)?.let {
                            handleBytes(obj, it)
                        } ?: kotlin.run {
                            requiredBytesCommands[md5] = obj
                        }
                    }
                    else -> responseHandler.handle(obj)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private suspend fun onBytesData(bytes: Bytes) {
            requiredBytesCommands.remove(bytes.md5)?.let { command ->
                handleBytes(command, bytes)
            } ?: kotlin.run {
                bytesMap[bytes.md5] = bytes
            }
        }


        private suspend fun ping() {
            while (true) {
                Log.d(TAG, "ping")
                val ping = Message(
                    type = TYPE_PING,
                    data = System.currentTimeMillis()
                )
                session.send(Frame.Text(gson.toJson(ping)))
                delay(10000)
                if (lastPongId != ping.data) {
                    Log.d(TAG, "ping: $lastPongId != ${ping.data}")
                    if (session.isActive) {
                        coroutineScope {
                            launch {
                                reconnect()
                            }
                        }
                    }
                    break
                }
            }
        }

        suspend fun newConnection(session: WebSocketSession, serverUrl: String? = null) =
            DevPlugin.newConnection(session, serverUrl)

        private suspend fun reconnect() {
            serverUrl?.let { url ->
                Log.i(TAG, "reconnect")
                emitDisconnect(session)
                var ok = false
                for (i in 0 until maxRetry) {
                    emitState(State(State.RECONNECTING))
                    try {
                        client.connect(url) {
                            ok = true
                            newConnection(this, url)
                        }
                    } catch (e: Exception) {
                        if (i == maxRetry - 1) {
                            emitState(State(State.CONNECTION_FAILED, e))
                        }
                        client.close()
                        e.printStackTrace()
                    }
                    if (ok) break
                }
            }
        }


        private suspend fun onHandshakeTimeout() {
            Log.i(TAG, "onHandshakeTimeout")
            emitState(State(State.HANDSHAKE_TIMEOUT))
            close(e = SocketTimeoutException("handshake timeout"))
        }


        private suspend fun handleBytes(obj: JsonObject, bytes: Bytes) {
            val projectDir = responseHandler.handleBytes1(obj, bytes)
            obj["data"].asJsonObject.add("dir", JsonPrimitive(projectDir.path))
            responseHandler.handle(obj)
        }

        private suspend fun serveHello(
            frame: Frame,
            ok: suspend (data: HelloResponse) -> Unit,
            fail: suspend () -> Unit
        ) {
            if (frame is Frame.Text) {
                val msg = frame.readText()
                val data =
                    kotlin.runCatching { gson.fromJson(msg, HelloResponse::class.java) }.getOrNull()
                Log.d(TAG, "serveHello: " + data?.toString())
                data?.let {
                    val okData = if (it.versionCode() >= 11090) "ok" else "连接成功"
                    if (it.type == TYPE_HELLO && it.data == okData) {
                        ok(data)
                        return
                    }
                }
                fail()
            }
        }

        fun log(log: String) {
            if (!session.isActive) return
            val data = Message(
                type = "log",
                data = LogData(log = log)
            )
            runBlocking {
                session.send(gson.toJson(data))
            }
        }

        suspend fun close(
            reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, ""),
            e: Throwable? = null
        ) {
            Log.i(TAG, "close: ${reason.message}")
            e?.printStackTrace()
            emitDisconnect(session, e)
        }

        suspend fun WebSocketSession.mapToData(
            onJson: suspend WebSocketSession.(JsonElement) -> Unit,
            onBytes: suspend WebSocketSession.(Bytes) -> Unit,
        ) {
            this.handleSession(
                onMessage = { frame ->
                    when (frame.frameType) {
                        FrameType.TEXT -> {
                            val text = (frame as Frame.Text).readText()
                            JsonUtil.dispatchJson(text)?.let { onJson(it) }
                        }
                        FrameType.BINARY -> {
                            val bytes = frame.readBytes()
                            val md5 = bytes.toByteString(0, bytes.size).md5().hex()
                            onBytes(Bytes(md5, bytes))
                        }
                        else -> {}
                    }
                },
                onError = { e ->
                    Log.i(TAG, "onError: ${e.message}")
                    e.printStackTrace()
                    emitDisconnect(this, e)
                },
                onClose = { e ->
                    Log.i(TAG, "onClose: ${e.message}")
                    e.printStackTrace()
                    emitDisconnect(this, e)
                },
                onFinally = {
                    Log.i(TAG, "onFinally")
                    emitDisconnect(this)
                }
            )

        }

        private suspend fun emitDisconnect(session: WebSocketSession?, e: Throwable? = null) {
            emitState(State(State.DISCONNECTED, e))
            session?.close()
            session?.cancel()
        }

        private suspend fun WebSocketSession.handleSession(
            onMessage: suspend WebSocketSession.(frame: Frame) -> Unit = { },
            onError: suspend WebSocketSession.(e: Throwable) -> Unit = { },
            onClose: suspend WebSocketSession.(e: ClosedReceiveChannelException) -> Unit = { },
            onFinally: suspend WebSocketSession.() -> Unit = { },
        ) {
            try {
                for (frame in incoming) {
                    onMessage(frame)
                }
            } catch (e: ClosedReceiveChannelException) {
                onClose(e)
            } catch (e: Throwable) {
                onError(e)
            } finally {
                onFinally()
            }
        }

        suspend fun emitState(state: State) {
            if (connection === this) _connectState.emit(state)
        }
    }

    suspend fun newConnection(session: WebSocketSession, serverUrl: String? = null) {
        val connection = Connection(session, serverUrl)
        this@DevPlugin.connection = connection
        connection.init()
    }

    suspend fun emitState(state: State) = _connectState.emit(state)
}