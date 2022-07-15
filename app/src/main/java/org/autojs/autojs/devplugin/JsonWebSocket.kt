package org.autojs.autojs.devplugin

import android.util.Log
import com.google.gson.JsonElement
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.*
import okio.ByteString

@Deprecated("")
class JsonWebSocket(client: OkHttpClient, request: Request) : WebSocketListener() {


    private val mWebSocket: WebSocket
    private val mJsonElementPublishSubject = PublishSubject.create<JsonElement>()
    private val mBytesPublishSubject = PublishSubject.create<Bytes>()

    @Volatile
    var isClosed = false
        private set

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(LOG_TAG, "onMessage: text = $text")
        JsonUtil.dispatchJson(text)?.let { mJsonElementPublishSubject.onNext(it) }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d(LOG_TAG, "onMessage: ByteString = $bytes")
        mBytesPublishSubject.onNext(Bytes(bytes.md5().hex(), bytes.toByteArray()))
    }

    fun data(): Observable<JsonElement> {
        return mJsonElementPublishSubject
    }

    fun bytes(): Observable<Bytes> {
        return mBytesPublishSubject
    }

    fun write(element: JsonElement): Boolean {
        val json = element.toString()
        Log.d(LOG_TAG, "write: length = " + json.length + ", json = " + element)
        return mWebSocket.send(json)
    }

    fun close() {
        mJsonElementPublishSubject.onComplete()
        isClosed = true
        mWebSocket.close(1000, "close")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(LOG_TAG, "onFailure: code = $code, reason = $reason")
        close()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(LOG_TAG, "onFailure: response = $response", t)
        close(t)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(LOG_TAG, "onOpen: response = $response")
    }

    private fun close(e: Throwable) {
        if (isClosed) {
            return
        }
        mJsonElementPublishSubject.onError(e)
        isClosed = true
        mWebSocket.close(1011, "remote exception: " + e.message)
    }

    companion object {
        private const val LOG_TAG = "JsonWebSocket"
    }

    init {
        mWebSocket = client.newWebSocket(request, this)
    }
}