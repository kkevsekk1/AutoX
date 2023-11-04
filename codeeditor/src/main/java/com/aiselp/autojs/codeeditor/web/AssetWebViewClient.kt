package com.aiselp.autojs.codeeditor.web

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.PathHandler
import java.io.File
import java.io.IOException
import java.io.InputStream

@RequiresApi(Build.VERSION_CODES.M)
class AssetWebViewClient(assetPath: String, context: Context) : JsBridge.SuperWebViewClient() {
    private val assetLoader: WebViewAssetLoader =
        WebViewAssetLoader.Builder()
            .setHttpAllowed(true)
//            .addPathHandler("/", AssetsPathHandler(context, assetPath))
            .addPathHandler("/", WebViewAssetLoader.InternalStoragePathHandler(context,File(context.filesDir,"public")))
            .build()


    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(Uri.parse(url)) ?: super.shouldInterceptRequest(
            view, url
        )
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(request.url) ?: super.shouldInterceptRequest(
            view, request
        )
    }

    class AssetsPathHandler(val context: Context, private val assetPath: String) : PathHandler {

        private fun parsePath(path: String): String {
            var newPath = File(assetPath, path).path
            if (newPath.startsWith("/")) {
                newPath = newPath.substring(1)
            }
            println("path: $path to-------- newPath: $newPath")
            return newPath
        }

        override fun handle(path: String): WebResourceResponse? {
            return try {
                val newPath = parsePath(path)
                val `is`: InputStream = context.assets.open(newPath)
                val ext = MimeTypeMap.getFileExtensionFromUrl(newPath)
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                WebResourceResponse(mimeType, null, `is`)
            } catch (e: IOException) {
                Log.e(TAG, "Error opening asset path: $path", e)
                WebResourceResponse(null, null, null)
            }
        }

        companion object {
            const val TAG = "AssetsPathHandler"
        }
    }
}