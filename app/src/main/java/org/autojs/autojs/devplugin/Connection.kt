package org.autojs.autojs.devplugin

import android.os.Build
import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.stardust.app.GlobalAppContext
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.autojs.autojs.core.model.devplugin.Bytes
import org.autojs.autojs.core.model.devplugin.Hello
import org.autojs.autojs.core.model.devplugin.HelloResponse
import org.autojs.autojs.core.model.devplugin.LogData
import org.autojs.autojs.core.model.devplugin.Message
import org.autojs.autojs.core.network.socket.State
import org.autojs.autoxjs.BuildConfig
import java.io.File
import java.net.SocketTimeoutException

private val gson get() = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create();
private const val CLIENT_VERSION = 2
private const val TAG = "DevPlugin"
private const val TYPE_HELLO = "hello"
private const val TYPE_PING = "ping"
private const val TYPE_PONG = "pong"
private const val TYPE_CLOSE = "close"
private const val TYPE_BYTES_COMMAND = "bytes_command"
private const val MAX_RETRY = 3

private val bytesMap = HashMap<String, Bytes>()
private val requiredBytesCommands = HashMap<String, JsonObject>()
private val responseHandler: DevPluginResponseHandler by lazy {
    val cache = File(GlobalAppContext.get().cacheDir, "remote_project")
    DevPluginResponseHandler(cache)
}

class Connection(
    private val session: WebSocketSession,
    private val serverUrl: String? = null,
    private val client: WebSocketClient
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

    private suspend fun newConnection(session: DefaultClientWebSocketSession, serverUrl: String? = null) =
        DevPlugin.newConnection(session, serverUrl)

    private suspend fun reconnect() {
        serverUrl ?: return
        Log.i(TAG, "reconnect")
        emitDisconnect()
        var ok = false
        for (i in 0 until MAX_RETRY) {
            emitState(State(State.RECONNECTING))
            try {
                client.connect(serverUrl) {
                    ok = true
                    newConnection(this, serverUrl)
                }
            } catch (e: Exception) {
                if (i == MAX_RETRY - 1) {
                    emitState(State(State.CONNECTION_FAILED, e))
                }
                client.close()
                e.printStackTrace()
            }
            if (ok) break
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
        emitDisconnect(e)
    }

    private suspend fun WebSocketSession.mapToData(
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
                emitDisconnect(e)
            },
            onClose = { e ->
                Log.i(TAG, "onClose: ${e.message}")
                e.printStackTrace()
                emitDisconnect(e)
            },
            onFinally = {
                Log.i(TAG, "onFinally")
                emitDisconnect()
            }
        )

    }

    private suspend fun emitDisconnect(e: Throwable? = null) {
        emitState(State(State.DISCONNECTED, e))
        session.close()
        session.cancel()
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
        DevPlugin.emitState(state)
    }
}