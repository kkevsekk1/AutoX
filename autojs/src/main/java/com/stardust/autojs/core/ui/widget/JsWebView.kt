package com.stardust.autojs.core.ui.widget

import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.AttributeSet
import android.webkit.*
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Scriptable
import kotlin.random.Random

open class JsWebView : WebView {
    //val events = EventEmitter()
    companion object {
        private const val sdkPath = "web/autox.sdk.v1.js"
    }

    val jsBridge = JsBridge(this)

    init {
        val settings = settings
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.loadWithOverviewMode = true
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.domStorageEnabled = true
        settings.displayZoomControls = false
        webViewClient = SuperWebViewClient()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
    @RequiresApi(Build.VERSION_CODES.M)
    fun injectionJsBridge(){
        val context = this.context
        val js: String = try {
            val inputStream = context.assets.open(sdkPath)
            val available = inputStream.available()
            val byteArray = ByteArray(available)
            inputStream.read(byteArray)
            inputStream.close()
            String(byteArray)
        } catch (e: Exception) {
            ""
        }
        jsBridge.evaluateJavascript(js);
    }

    class JsBridge(private val webView: WebView) {
        companion object {
            const val WEBOBJECTNAME = "\$autox"
            const val JAVABRIDGE = "AutoxJavaBridge"
        }

        private val handles = HashMap<String, CrFunction>()
        private val callBackFunctions = HashMap<Int, CrFunction>()
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
            val pos = Pos(getId(),event, data)
            callHandlerData[pos.id] = pos
            callBack?.let {
                pos.callBackId = pos.id
                callBackFunctions[pos.id] = it
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
            if (callHandlerData.containsKey(nextInt)||callBackFunctions.containsKey(nextInt)) {
                return getId()
            }
            return nextInt
        }
        @RequiresApi(Build.VERSION_CODES.M)
        fun evaluateJavascript(js:String){
            Looper.getMainLooper().queue.addIdleHandler {
                webView.evaluateJavascript(js, null)
                false
            }
        }
        inner class JsObject() {
            val callBackData = HashMap<Int, Any>()

            @JavascriptInterface
            //web调用安卓
            fun callHandle(reqData: String) {
                val req = Gson().fromJson(reqData, Map::class.java)
                val event = req["event"] as String
                val data = req["data"] as String?
                val callBackId = req["callBackId"] as Double?
                val handler = handles[event]
                if (callBackId != null) {
                    handler?.run(data, object : BaseFunction() {
                        @RequiresApi(Build.VERSION_CODES.M)
                        override fun call(
                            cx: org.mozilla.javascript.Context?,
                            scope: Scriptable?,
                            thisObj: Scriptable?,
                            args: Array<out Any>
                        ): Any {
                            val d : String? = try {
                                args[0] as String?
                            }catch (e: Exception){
                                null
                            }
                            callBackData[callBackId.toInt()] = object {
                                val data = d
                                val callBackId = callBackId
                            }
                            val js = "${WEBOBJECTNAME}._onCallBack(${callBackId})"
                            evaluateJavascript(js)
                            return super.call(cx, scope, thisObj, args)
                        }
                    })
                } else handler?.run(data)
            }

            @JavascriptInterface
            fun getCallBackData(id: Int): String {
                val data = callBackData[id]
                callBackData.remove(id)
                return Gson().toJson(data)
            }

            @JavascriptInterface
            fun callBack(callBackId: Int, data: String?) {
                val callBack = callBackFunctions[callBackId]
                callBackFunctions.remove(callBackId)
                callBack?.run(data)
            }

            @JavascriptInterface
            fun getCallHandlerData(id: Int): String {
                val pos = callHandlerData[id]
                callHandlerData.remove(id)
                return Gson().toJson(pos)
            }
        }

        data class Pos(val id:Int,val event: String, var data: String?) {
            var callBackId: Int? = null
        }
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
