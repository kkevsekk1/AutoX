package org.autojs.autojs.devplugin

import android.annotation.SuppressLint
import android.os.Build
import android.os.Looper
import android.util.Log
import android.util.Pair
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.stardust.app.GlobalAppContext.get
import com.stardust.util.MapBuilder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.autojs.autojs.BuildConfig
import java.io.File
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Created by Stardust on 2017/5/11.
 */
@Deprecated("")
class DevPluginService {
    class State @JvmOverloads constructor(val state: Int, val exception: Throwable? = null) {

        companion object {
            const val DISCONNECTED = 0
            const val CONNECTING = 1
            const val CONNECTED = 2
        }
    }

    private val mConnectionState = PublishSubject.create<State>()
    private val mResponseHandler: DevPluginResponseHandler
    private val mBytes = HashMap<String, Bytes>()
    private val mRequiredBytesCommands = HashMap<String, JsonObject>()
    private val mHandler = android.os.Handler(Looper.getMainLooper())

    @Volatile
    private var mSocket: JsonWebSocket? = null

    @get:AnyThread
    val isConnected: Boolean
        get() = mSocket != null && !mSocket!!.isClosed

    @get:AnyThread
    val isDisconnected: Boolean
        get() = mSocket == null || mSocket!!.isClosed

    @AnyThread
    fun disconnectIfNeeded() {
        if (isDisconnected) return
        disconnect()
    }

    @AnyThread
    fun disconnect() {
        mSocket!!.close()
        mSocket = null
    }

    fun connectionState(): Observable<State> {
        return mConnectionState
    }

    @AnyThread
    fun connectToServer(host: String, params: String): Flow<JsonWebSocket> {
        var port = PORT
        var ip = host
        val i = host.lastIndexOf(':')
        if (i > 0 && i < host.length - 1) {
            port = host.substring(i + 1).toInt()
            ip = host.substring(0, i)
        }
        mConnectionState.onNext(State(State.CONNECTING))
        return socket(ip, port, params)
            .flowOn(Dispatchers.Main)
            .catch { e: Throwable -> onSocketError(e) }
    }

    @AnyThread
    private fun socket(ip: String, port: Int, params: String): Flow<JsonWebSocket> {
        val client: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        var url = "$ip:$port"
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            url = "ws://$url"
        }
        url = "$url?$params"
        Log.d(LOG_TAG, "socket: $url")
        return flowOf(
            JsonWebSocket(
                client, Request.Builder()
                    .url(url)
                    .build()
            )
        ).onEach { socket: JsonWebSocket ->
            mSocket = socket
            subscribeMessage(socket)
            sayHelloToServer(socket)
        }
    }

    @SuppressLint("CheckResult")
    private fun subscribeMessage(socket: JsonWebSocket) {
        socket.data()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { mConnectionState.onNext(State(State.DISCONNECTED)) }
            .subscribe({ data: JsonElement ->
                onSocketData(
                    socket,
                    data
                )
            }) { e: Throwable -> onSocketError(e) }
        socket.bytes()
            .doOnComplete { mConnectionState.onNext(State(State.DISCONNECTED)) }
            .subscribe({ data: Bytes ->
                onSocketData(
                    data
                )
            }) { e: Throwable -> onSocketError(e) }
    }

    @MainThread
    private fun onSocketError(e: Throwable) {
        e.printStackTrace()
        if (mSocket != null) {
            mConnectionState.onNext(State(State.DISCONNECTED, e))
            mSocket!!.close()
            mSocket = null
        }
    }

    @MainThread
    private fun onSocketData(jsonWebSocket: JsonWebSocket, element: JsonElement) {
        if (!element.isJsonObject) {
            Log.w(LOG_TAG, "onSocketData: not json object: $element")
            return
        }
        try {
            val obj = element.asJsonObject
            val typeElement = obj["type"]
            if (typeElement == null || !typeElement.isJsonPrimitive) {
                return
            }
            val type = typeElement.asString
            if (type == TYPE_HELLO) {
                onServerHello(jsonWebSocket, obj)
                return
            }
            if (TYPE_BYTES_COMMAND == type) {
                val md5 = obj["md5"].asString
                val bytes = mBytes.remove(md5)
                if (bytes != null) {
                    handleBytes(obj, bytes)
                } else {
                    mRequiredBytesCommands[md5] = obj
                }
                return
            }
            mResponseHandler.handle(obj)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("CheckResult")
    private fun handleBytes(obj: JsonObject, bytes: Bytes) {
        mResponseHandler.handleBytes(obj, bytes)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { dir: File ->
                obj["data"].asJsonObject.add("dir", JsonPrimitive(dir.path))
                mResponseHandler.handle(obj)
            }
    }

    @WorkerThread
    private fun onSocketData(bytes: Bytes) {
        val command = mRequiredBytesCommands.remove(bytes.md5)
        if (command != null) {
            handleBytes(command, bytes)
        } else {
            mBytes[bytes.md5] = bytes
        }
    }

    @WorkerThread
    private fun sayHelloToServer(socket: JsonWebSocket) {
        writeMap(
            socket, TYPE_HELLO, MapBuilder<String, Any>()
                .put("device_name", Build.BRAND + " " + Build.MODEL)
                .put("client_version", CLIENT_VERSION)
                .put("app_version", BuildConfig.VERSION_NAME)
                .put("app_version_code", BuildConfig.VERSION_CODE)
                .build()
        )
        mHandler.postDelayed({
            if (mSocket !== socket && !socket.isClosed) {
                onHandshakeTimeout(socket)
            }
        }, HANDSHAKE_TIMEOUT)
    }


    @MainThread
    private fun onHandshakeTimeout(socket: JsonWebSocket) {
        Log.i(LOG_TAG, "onHandshakeTimeout")
        mConnectionState.onNext(
            State(
                State.DISCONNECTED,
                SocketTimeoutException("handshake timeout")
            )
        )
        socket.close()
    }

    @MainThread
    private fun onServerHello(jsonWebSocket: JsonWebSocket, message: JsonObject) {
        Log.i(LOG_TAG, "onServerHello: $message")
        var msg = "请在服务器端查看消息"
        try {
            msg = message["data"].asString
            if ("连接成功" != msg) {
                disconnectIfNeeded()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mSocket = jsonWebSocket
        mConnectionState.onNext(State(State.CONNECTED, SocketTimeoutException(msg)))
    }

    @SuppressLint("CheckResult")
    @AnyThread
    fun log(log: String) {
        if (!isConnected) return
        writePair(mSocket, "log", Pair("log", log))
    }

    companion object {
        private const val CLIENT_VERSION = 2
        private const val LOG_TAG = "DevPluginService"
        private const val TYPE_HELLO = "hello"
        private const val TYPE_BYTES_COMMAND = "bytes_command"
        private const val HANDSHAKE_TIMEOUT = (10 * 1000).toLong()
        private const val PORT = 9317

        @JvmField
        val instance = DevPluginService()

        @AnyThread
        private fun write(socket: JsonWebSocket?, type: String, data: JsonObject): Boolean {
            val json = JsonObject()
            json.addProperty("type", type)
            json.add("data", data)
            return socket!!.write(json)
        }

        @AnyThread
        private fun writePair(
            socket: JsonWebSocket?,
            type: String,
            pair: Pair<String, String>
        ): Boolean {
            val data = JsonObject()
            data.addProperty(pair.first, pair.second)
            return write(socket, type, data)
        }

        @AnyThread
        private fun writeMap(socket: JsonWebSocket, type: String, map: Map<String, *>): Boolean {
            val data = JsonObject()
            for ((key, value1) in map) {
                val value = value1!!
                if (value is String) {
                    data.addProperty(key, value)
                } else if (value is Char) {
                    data.addProperty(key, value)
                } else if (value is Number) {
                    data.addProperty(key, value)
                } else if (value is Boolean) {
                    data.addProperty(key, value)
                } else if (value is JsonElement) {
                    data.add(key, value)
                } else {
                    throw IllegalArgumentException("cannot put value $value into json")
                }
            }
            return write(socket, type, data)
        }
    }

    init {
        val cache = File(get().cacheDir, "remote_project")
        mResponseHandler = DevPluginResponseHandler(cache)
    }
}