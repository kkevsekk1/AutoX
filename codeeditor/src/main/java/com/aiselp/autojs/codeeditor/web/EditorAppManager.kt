package com.aiselp.autojs.codeeditor.web

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.aiselp.autojs.codeeditor.plugins.FileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.M)
class EditorAppManager(context: Context) {
    companion object {
        const val TAG = "EditorAppManager"
        const val WEB_DIST_PATH = "codeeditor/dist/"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val executors: ExecutorService = Executors.newSingleThreadExecutor()
    val webView = createWebView(context)
    private val jsBridge = JsBridge(webView)
    private val fileHttpServer = FileHttpServer(context)
    private val pluginManager = PluginManager(jsBridge,coroutineScope)

    init {
        jsBridge.registerHandler("test", JsBridge.Handle { data, handle ->
            println("web调用：Data: $data")
            if (handle != null) {
                handle.invoke("ax水", null)
            }
        })
        webView.webViewClient = JsBridge.SuperWebViewClient()
        installPlugin()
        coroutineScope.launch {
            launch(executors.asCoroutineDispatcher()) { fileHttpServer.start() }
            delay(300)
            withContext(Dispatchers.Main) {
//                webView.loadUrl("http://${fileHttpServer.hostname}:${fileHttpServer.port}")
                webView.loadUrl("http://192.168.10.10:8010")
            }
        }
//        webView.loadUrl("http://appassets.androidplatform.net/index.html")
    }

    private fun installPlugin() {
        pluginManager.registerPlugin("FileSystem", FileSystem())
    }

    fun destroy() {
        webView.destroy()
        fileHttpServer.stop()
        coroutineScope.cancel()
        executors.shutdownNow()
    }

    private fun createWebView(context: Context): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                useWideViewPort = true
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
}