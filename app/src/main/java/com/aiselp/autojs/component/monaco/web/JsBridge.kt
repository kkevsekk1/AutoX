package com.aiselp.autojs.component.monaco.web

import android.os.Build
import android.os.Looper
import android.webkit.*
import androidx.annotation.RequiresApi
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.M)
class JsBridge(private val webView: WebView) {
    companion object {
        const val WEBOBJECTNAME = "\$autox"
        const val JAVABRIDGE = "AutoxJavaBridge"
        const val sdkPath = "web/autox.sdk.v1.js"
        private fun evaluateJavascript(js: String,webView: WebView) {
            Looper.getMainLooper().queue.addIdleHandler {
                webView.evaluateJavascript(js, null)
                false
            }
        }
        val gson: Gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
    }

    private val handles =
        HashMap<String, Handle>()
    private val callHandlerData = HashMap<Int, Pos>()

    init {
        webView.addJavascriptInterface(this.JsObject(), JAVABRIDGE)
    }

    fun registerHandler(
        event: String,
        handle: Handle
    ): JsBridge {
        handles[event] = handle
        return this
    }

    fun removeHandler(event: String): JsBridge {
        handles.remove(event)
        return this
    }

    fun callHandler(event: String, data: String?, callBack: ((data: String?) -> Unit?)?) {
        val pos = Pos(getId(), event, data)
        callHandlerData[pos.id] = pos
        callBack?.let {
            pos.callBackId = pos.id
            pos.callBack = it
        }
        val js = "${WEBOBJECTNAME}._onCallHandler(${pos.id})"
        evaluateJavascript(js,this.webView)
    }

    fun callHandler(event: String, data: String?) {
        callHandler(event, data, null)
    }


    fun callHandler(event: String) {
        callHandler(event, null, null)
    }

    fun getId(): Int {
        val nextInt = Random.nextInt()
        if (callHandlerData.containsKey(nextInt)) {
            return getId()
        }
        return nextInt
    }

    class Handle(val fn: (data: String?, Handle?) -> Unit) : (String?, Handle?) -> Unit {
        fun invoke(data: String?) {
            invoke(data, null)
        }

        override fun invoke(p1: String?, p2: Handle?) {
            fn.invoke(p1, p2)
        }
    }

    inner class JsObject {
        val callBackData = HashMap<Int, Any>()

        @JavascriptInterface
        //web调用安卓
        fun callHandle(reqData: String) {
            val pos = gson.fromJson(reqData, Pos::class.java)
            val handler = handles[pos.event]
            if (pos.callBackId != null) {
                handler?.invoke(
                    pos.data, Handle { data, handle ->
                        callBackData[pos.callBackId!!] = object {
                            val data = data
                            val callBackId = pos.callBackId
                        }
                        val js = "${WEBOBJECTNAME}._onCallBack(${pos.callBackId})"
                        evaluateJavascript(js,webView)
                    }
                )
            } else handler?.invoke(pos.data, null)
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
            callBack?.invoke(data)
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
        var callBack: ((data: String?) -> Unit?)? = null
    }

    open class SuperWebViewClient : AccompanistWebViewClient() {
        private fun injectionJsBridge(webView: WebView) {
            val js: String = try {
                val inputStream = webView.context.assets.open(sdkPath)
                val available = inputStream.available()
                val byteArray = ByteArray(available)
                inputStream.read(byteArray)
                inputStream.close()
                String(byteArray)
            } catch (e: Exception) {
                ""
            }
            evaluateJavascript(js,webView);
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val url = request.url
            val context = view.context
            if (url.toString().startsWith("autox://sdk.v1.js")) {
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