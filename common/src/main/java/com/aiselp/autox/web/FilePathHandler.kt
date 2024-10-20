package com.aiselp.autox.web

import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.File

class FilePathHandler(private val baseFile: File) : WebViewAssetLoader.PathHandler {

    private fun parsePath(path: String): File {
        val file = File(baseFile, path.ifEmpty { "index.html" }).let {
            if (it.isDirectory) {
                File(it, "index.html")
            } else it
        }

        return file
    }

    override fun handle(path: String): WebResourceResponse? {
        val file = parsePath(path)
        if (!file.isFile) return null
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            MimeTypeMap.getFileExtensionFromUrl(path)
        )
        return WebResourceResponse(mimeType, null, file.inputStream())
    }
}