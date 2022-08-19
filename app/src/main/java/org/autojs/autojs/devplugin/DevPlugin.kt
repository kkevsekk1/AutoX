package org.autojs.autojs.devplugin

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.stardust.app.GlobalAppContext
import io.ktor.server.plugins.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.*
import okio.ByteString.Companion.toByteString
import org.autojs.autoxjs.BuildConfig
import java.io.File
import java.net.SocketTimeoutException

object DevPlugin {

    data class State(val state: Int, val e: Throwable? = null) {
        companion object {
            const val DISCONNECTED = 0
            const val CONNECTING = 1
            const val CONNECTED = 2
            const val CONNECTION_FAILED = 3
//            const val USB_DEBUG_SERVER_STARTED = 4
        }
    }

    private val gson = Gson()
    const val SERVER_PORT = 9317
    private const val CLIENT_VERSION = 2
    private const val LOG_TAG = "ConnectComputer"
    private const val TYPE_HELLO = "hello"
    private const val TYPE_CLOSE = "close"
    private const val TYPE_BYTES_COMMAND = "bytes_command"
    private const val HANDSHAKE_TIMEOUT = (10 * 1000).toLong()

    private val _connectState = MutableSharedFlow<State>()
    private var isHandshake = false
    private val client by lazy { WebSocketClient() }
    private val server by lazy { WebSocketServer() }
    private var session: WebSocketSession? = null
    private val bytesMap = HashMap<String, Bytes>()
    private val requiredBytesCommands = HashMap<String, JsonObject>()
    private val responseHandler: DevPluginResponseHandler by lazy {
        val cache = File(GlobalAppContext.get().cacheDir, "remote_project")
        DevPluginResponseHandler(cache)
    }

    val isActive get() = session?.isActive ?: false
    val connectState = _connectState.asSharedFlow()
    val isUSBDebugServiceActive get() = server.isActive

    suspend fun connect(url: String) {
        withContext(Dispatchers.IO) {
            _connectState.emit(State(State.CONNECTING))
            try {
                client.connect(
                    url
                ) {
                    if (!DevPlugin.isActive) {
                        handle()
                    }
                }
            } catch (e: Exception) {
                _connectState.emit(State(State.CONNECTION_FAILED, e))
                client.close()
                e.printStackTrace()
            }
        }
    }

    fun startUSBDebug() {
        server.listen(SERVER_PORT, "/", host = "127.0.0.1") {
            Log.i("startUSBDebug:", this.call.request.origin.remoteHost)
            if (!DevPlugin.isActive) {
                handle()
            }
        }
    }

    suspend fun stopUSBDebug() {
        withContext(Dispatchers.IO) {
            server.stop()
        }
    }

    suspend fun WebSocketSession.handle() {
        _connectState.emit(State(State.CONNECTED))
        this@DevPlugin.session = this
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
            launch {
                sayHello()
            }
        }
    }

    private suspend fun onJsonData(element: JsonElement) {
        if (!element.isJsonObject) {
            Log.w(LOG_TAG, "onSocketData: not json object: $element")
            return
        }
        try {
            val obj = element.asJsonObject
            val typeElement = obj["type"] ?: return
            if (!typeElement.isJsonPrimitive) return
            when (typeElement.asString) {
                TYPE_HELLO -> {
                    serveHello(obj)
                    return
                }
                TYPE_CLOSE -> {
                    this.close()
                    return
                }
                TYPE_BYTES_COMMAND -> {
                    val md5 = obj["md5"].asString
                    bytesMap.remove(md5)?.let {
                        handleBytes(obj, it)
                    } ?: kotlin.run {
                        requiredBytesCommands[md5] = obj
                    }
                    return
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

    private suspend fun WebSocketSession.sayHello() {
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
        withContext(Dispatchers.IO) {
            launch {
                delay(HANDSHAKE_TIMEOUT)
                if (!isHandshake) {
                    onHandshakeTimeout()
                }
            }
        }
    }

    private suspend fun onHandshakeTimeout() {
        Log.i(LOG_TAG, "onHandshakeTimeout")
        close(e = SocketTimeoutException("handshake timeout"))
    }


    private suspend fun handleBytes(obj: JsonObject, bytes: Bytes) {
        val projectDir = responseHandler.handleBytes1(obj, bytes)
        obj["data"].asJsonObject.add("dir", JsonPrimitive(projectDir.path))
        responseHandler.handle(obj)
    }

    private suspend fun serveHello(message: JsonObject) {
        Log.i(LOG_TAG, "onServerHello: $message")
        val msg = message["data"].asString
        if ("连接成功" == msg) {
            isHandshake = true
            Log.d(LOG_TAG, "serveHello: 连接成功")
        } else {
            close()
        }
    }

    fun log(log: String) {
        if (!isActive) return
        val data = Message(
            type = "log",
            data = LogData(log = log)
        )
        session?.let {
            it.launch {
                it.send(gson.toJson(data))
            }
        }
    }

    suspend fun close(
        reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, ""),
        e: Throwable? = null
    ) {
        _connectState.emit(State(State.DISCONNECTED, e))
        session?.close(reason)
        session = null
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
                this@DevPlugin.close(
                    CloseReason(1011, "remote exception: " + e.message),
                    e
                )
            },
            onClose = { e ->
                this@DevPlugin.close(
                    CloseReason(
                        1000,
                        "close"
                    ),
                    e
                )
            }
        )

    }

    private suspend fun WebSocketSession.handleSession(
        onMessage: suspend WebSocketSession.(frame: Frame) -> Unit = { },
        onError: suspend WebSocketSession.(e: Throwable) -> Unit = { },
        onClose: suspend WebSocketSession.(e: ClosedReceiveChannelException) -> Unit = { },
    ) {
        try {
            for (frame in incoming) {
                onMessage(frame)
            }
        } catch (e: ClosedReceiveChannelException) {
            onClose(e)
        } catch (e: Throwable) {
            onError(e)
        }
    }

}