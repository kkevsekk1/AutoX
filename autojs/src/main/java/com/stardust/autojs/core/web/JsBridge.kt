package com.stardust.autojs.core.web

import android.os.Build
import android.os.Looper
import android.webkit.*
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.stardust.autojs.core.ui.widget.JsWebView
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Scriptable
import kotlin.random.Random

class JsBridge(private val webView: WebView) {
    companion object {
        const val WEBOBJECTNAME = "\$autox"
        const val JAVABRIDGE = "AutoxJavaBridge"
        const val sdkPath = "web/autox.sdk.v1.js"
        val gson: Gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
    }

    private val handles = HashMap<String, CrFunction>()
    private val callHandlerData = HashMap<Int, Pos>()

    init {
        webView.addJavascriptInterface(this.JsObject(), JAVABRIDGE)
    }

    fun registerHandler(event: String, handle: CrFunction): JsBridge {
        handles[event] = handle
        return this
    }

    fun removeHandler(event: String): JsBridge {
        handles.remove(event)
        return this
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun callHandler(event: String, data: String?, callBack: CrFunction?) {
        val pos = Pos(getId(), event, data)
        callHandlerData[pos.id] = pos
        callBack?.let {
            pos.callBackId = pos.id
            pos.callBack = it
        }
        val js = "${WEBOBJECTNAME}._onCallHandler(${pos.id})"
        evaluateJavascript(js)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun callHandler(event: String, data: String?) {
        callHandler(event, data, null)
    }

    @RequiresApi(Build.VERSION_CODES.M)
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

    @RequiresApi(Build.VERSION_CODES.M)
    fun evaluateJavascript(js: String) {
        Looper.getMainLooper().queue.addIdleHandler {
            webView.evaluateJavascript(js, null)
            false
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
                handler?.run(pos.data, object : BaseFunction() {
                    @RequiresApi(Build.VERSION_CODES.M)
                    override fun call(
                        cx: org.mozilla.javascript.Context?,
                        scope: Scriptable?,
                        thisObj: Scriptable?,
                        args: Array<out Any>
                    ): Any {
                        val d: String? = try {
                            args[0] as String?
                        } catch (e: Exception) {
                            null
                        }
                        callBackData[pos.callBackId!!] = object {
                            val data = d
                            val callBackId = pos.callBackId
                        }
                        val js = "${WEBOBJECTNAME}._onCallBack(${pos.callBackId})"
                        evaluateJavascript(js)
                        return super.call(cx, scope, thisObj, args)
                    }
                })
            } else handler?.run(pos.data)
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
            callBack?.run(data)
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
        var callBack: CrFunction? = null
    }

    interface CrFunction {
        fun run(args: Any?)
        fun run(arg1: Any?, arg2: Any?)
        fun run(arg1: Any?, arg2: Any?, arg3: Any?)
    }

    open class SuperWebViewClient : WebViewClient() {
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

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            (view as JsWebView).injectionJsBridge()
        }
    }
}