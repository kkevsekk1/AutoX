package org.autojs.autojs.ui.main.web

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.stardust.util.IntentUtil

class WebViewClient(val context: Context, assetPath: String) : WebViewClientCompat() {
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/", AssetsPathHandler(context, assetPath))
        .build();

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(request.url)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        IntentUtil.browse(context, request.url.toString())
        return true
    }
}