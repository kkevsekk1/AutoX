package com.aiselp.autojs.codeeditor.web

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import com.aiselp.autox.web.FilePathHandler
import java.io.File

class FileAssetWebViewClient(dir: File) : JsBridge.SuperWebViewClient() {
    private val assetLoader: WebViewAssetLoader =
        WebViewAssetLoader.Builder()
            .setHttpAllowed(true)
            .addPathHandler("/", FilePathHandler(dir))
            .build()


    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(Uri.parse(url)) ?: let {
            Log.w(TAG, "url request: $url -> $it")
            super.shouldInterceptRequest(view, url)
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(request.url) ?: let {
            Log.w(TAG, "url request: ${request.url} -> $it")
            super.shouldInterceptRequest(view, request)
        }
    }

    companion object {
        const val TAG = "FileAssetWebViewClient"
    }
}