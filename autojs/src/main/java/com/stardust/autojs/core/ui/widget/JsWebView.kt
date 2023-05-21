package com.stardust.autojs.core.ui.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.webkit.*
import androidx.annotation.RequiresApi
import com.stardust.autojs.core.web.JsBridge

open class JsWebView : WebView {
    //val events = EventEmitter()

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
        webViewClient = JsBridge.SuperWebViewClient()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
    @RequiresApi(Build.VERSION_CODES.M)
    fun injectionJsBridge() {
        val context = this.context
        val js: String = try {
            val inputStream = context.assets.open(JsBridge.sdkPath)
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
}
