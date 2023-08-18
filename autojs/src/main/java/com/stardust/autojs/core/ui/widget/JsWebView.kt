package com.stardust.autojs.core.ui.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.webkit.*
import androidx.annotation.RequiresApi
import com.stardust.autojs.core.web.JsBridge

open class JsWebView : WebView {
    //val events = EventEmitter()
    @RequiresApi(Build.VERSION_CODES.M)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) webViewClient = JsBridge.SuperWebViewClient()
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
        JsBridge.injectionJsBridge(this)
    }
}
