package com.aiselp.autojs.codeeditor.web

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.M)
class EditorAppManager(context: Context) {
    companion object {
        val TAG = "EditorAppManager"
        const val WEB_DIST_PATH = "codeeditor/dist/"
    }
    val executors = Executors.newSingleThreadExecutor()
    val webView = createWebView(context)
    //    val jsBridge = JsBridge(webView)
    private val fileHttpServer = FileHttpServer(context)

    init {
        println("laoding WebView")
        webView.webViewClient = AssetWebViewClient(WEB_DIST_PATH, context)
        executors.submit {
            fileHttpServer.start()
        }
        while (true){
            if (fileHttpServer.isAlive) {
                webView.loadUrl("http://${fileHttpServer.hostname}:${fileHttpServer.port}")
                break
            }
        }
//        webView.loadUrl("http://appassets.androidplatform.net/index.html")
    }

    fun destroy() {
        webView.destroy()
        fileHttpServer.stop()
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