package com.aiselp.autojs.codeeditor.web

import android.os.Build
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.webkit.WebViewClientCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import java.io.ByteArrayOutputStream
import kotlin.random.Random


@RequiresApi(Build.VERSION_CODES.M)
class JsBridge(private val webView: WebView) {
    companion object {
        const val WEBOBJECTNAME = "\$autox"
        const val JAVABRIDGE = "AutoxJavaBridge"
        const val sdkPath = "web/autox.sdk.v1.js"
        const val LOG_TAG = "JsBridge"

        fun evaluateJavascript(js: String, webView: WebView) {
            Looper.getMainLooper().queue.addIdleHandler {
                webView.evaluateJavascript(js, null)
                false
            }
        }

        fun injectionJsBridge(webView: WebView) {
            val js: String = try {
                val inputStream = webView.context.assets.open(sdkPath)
                val byteArrayOutputStream = ByteArrayOutputStream()
                inputStream.use { it ->
                    it.copyTo(byteArrayOutputStream)
                }
                String(byteArrayOutputStream.toByteArray())
            } catch (e: Exception) {
                ""
            }
            evaluateJavascript(js, webView);
        }

        val gson: Gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
    }

    private val handles = HashMap<String, Handle>()
    private val callHandlerData = HashMap<Int, Pos>()

    init {
        webView.addJavascriptInterface(this.JsObject(), JAVABRIDGE)
    }

    fun registerHandler(event: String, handle: Handle): JsBridge {
        handles[event] = handle
        return this
    }

    fun removeHandler(event: String): JsBridge {
        handles.remove(event)
        return this
    }

    fun callHandler(event: String, data: String?, callBack: Handle?) {
        val pos = Pos(getId(), event, data)
        callHandlerData[pos.id] = pos
        callBack?.let {
            it.name = "callBack"
            pos.callBackId = pos.id
            pos.callBack = it
        }
        val js = "$WEBOBJECTNAME._onCallHandler(${pos.id})"
        evaluateJavascript(js, this.webView)
    }


    private fun getId(): Int {
        val nextInt = Random.nextInt()
        if (callHandlerData.containsKey(nextInt)) {
            return getId()
        }
        return nextInt
    }

    class Handle : (String?, Handle?) -> Unit {
        private var ktFn: ((data: String?, Handle?) -> Unit)? = null
        var name: String = "handle"

        constructor(ktFn: (data: String?, Handle?) -> Unit) {
            this.ktFn = ktFn
        }

        override fun invoke(p1: String?, p2: Handle?) {
            ktFn?.invoke(p1, p2)
        }

        fun invokeToMainThread(p1: String?, p2: Handle?) {
            Looper.getMainLooper().queue.addIdleHandler {
                invoke(p1, p2)
                false
            }
        }
    }

    inner class JsObject {
        private val callBackData = HashMap<Int, Any>()

        @JavascriptInterface
        //web调用安卓
        fun callHandle(reqData: String) {
            val pos = gson.fromJson(reqData, Pos::class.java)
            Log.i(LOG_TAG, "onHandle: ${pos.event}")
            val handler = handles[pos.event]
            val callBack: Handle? = if (pos.callBackId != null) {
                Handle { data, _ ->
                    callBackData[pos.callBackId!!] = object {
                        val data = data
                        val callBackId = pos.callBackId
                    }
                    val js = "$WEBOBJECTNAME._onCallBack(${pos.callBackId})"
                    evaluateJavascript(js, webView)
                }
            } else null
            handler?.let { it.name = "callBack" }
            handler?.invokeToMainThread(pos.data, callBack)
        }

        @JavascriptInterface
        fun getCallBackData(id: Int): String {
            val data = callBackData[id]
            callBackData.remove(id)
            return Gson().toJson(data)
        }

        @JavascriptInterface
        fun callBack(callBackId: Int, data: String?) {
            val callBack = callHandlerData[callBackId]?.callBack
            callHandlerData.remove(callBackId)
            callBack?.invokeToMainThread(data, null)
        }

        @JavascriptInterface
        fun getCallHandlerData(id: Int): String {
            val pos = callHandlerData[id]
            if (pos != null && pos.callBack == null) {
                callHandlerData.remove(id)
            }
            return gson.toJson(pos)
        }
    }

    data class Pos(@Expose val id: Int, @Expose val event: String, @Expose var data: String?) {
        @Expose
        var callBackId: Int? = null

        @Expose(serialize = false, deserialize = false)
        var callBack: Handle? = null
    }

    open class SuperWebViewClient : WebViewClientCompat() {
        companion object {
            const val SDKPATH = "autox://sdk.v1.js"
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val url = request.url
            val context = view.context
            if (url.toString() == SDKPATH) {
                val webResponse: WebResourceResponse? = try {
                    val inputStream = context.assets.open(sdkPath)
                    WebResourceResponse("application/javascript", "UTF-8", inputStream)
                } catch (e: Exception) {
                    super.shouldInterceptRequest(view, request)
                }
                return webResponse
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (view != null) {
                injectionJsBridge(view)
            }
        }
    }
}