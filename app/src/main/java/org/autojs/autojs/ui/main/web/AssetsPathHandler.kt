package org.autojs.autojs.ui.main.web

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class AssetsPathHandler(val context: Context, private val assetPath: String) :
    WebViewAssetLoader.PathHandler {
    private fun parsePath(path: String): String {
        var newPath = File(assetPath, path.ifEmpty { "index.html" }).path
        if (newPath.startsWith("/")) {
            newPath = newPath.substring(1)
        }
//        println("path: $path to-------- newPath: $newPath")
        return newPath
    }

    override fun handle(path: String): WebResourceResponse? {
        return try {
            val newPath = parsePath(path)
            val `is`: InputStream = openAsset(newPath) ?: return null
            val ext = MimeTypeMap.getFileExtensionFromUrl(newPath)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            WebResourceResponse(mimeType, null, `is`)
        } catch (e: IOException) {
            Log.e(TAG, "Error opening asset path: $path", e)
            null
        }
    }

    private fun openAsset(path: String): InputStream? {
        return try {
            context.assets.open(path)
        } catch (e: FileNotFoundException) {
            val list = context.assets.list(path)
            if (!list.isNullOrEmpty()) {
                context.assets.open("$path/index.html")
            } else null
        }
    }

    companion object {
        const val TAG = "AssetsPathHandler"
    }
}