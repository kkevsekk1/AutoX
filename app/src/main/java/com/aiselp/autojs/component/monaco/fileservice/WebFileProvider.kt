package com.aiselp.autojs.component.monaco.fileservice

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.aiselp.autojs.component.monaco.web.JsBridge
import com.google.gson.Gson
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class WebFileProvider(val context: Context) : FileProvider {

    @RequiresApi(Build.VERSION_CODES.M)
    constructor(context: Context, jsBridge: JsBridge) : this(context) {
        val gson = Gson()
        jsBridge.registerHandler("readFile", JsBridge.Handle { data, handle ->
            val fileRequest = gson.fromJson(data, FileRequest::class.java)
            println("请求文件：${fileRequest.uri}")
            val uri = Uri.parse(fileRequest.uri)
            try {
                read(uri)?.let {
                    val outputStream = ByteArrayOutputStream()
                    it.use { it ->
                        it.copyTo(outputStream)
                        val text = outputStream.toString()
                        handle?.invoke(gson.toJson(Result(uri, text)))
                        return@Handle
                    }
                }
            } catch (_: Exception) {
            }
            handle?.invoke(gson.toJson(Result(Exception("not file: $uri"))))
        })
        jsBridge.registerHandler("writeFile", JsBridge.Handle { data, handle ->
            val fileRequest = gson.fromJson(data, FileRequest::class.java)
            println("请求写入文件：${fileRequest.uri}")
            val uri = Uri.parse(fileRequest.uri)
            try {
                val byteArrayInputStream = ByteArrayInputStream(fileRequest.data!!.toByteArray())
                val b = write(uri, byteArrayInputStream)
                handle?.invoke(gson.toJson(Result(uri, b.toString())))
                return@Handle
            } catch (e: Exception) {
                handle?.invoke(gson.toJson(Result(e)))
            }
        })
    }

    override fun read(uri: Uri): InputStream? {
        return if (uri.scheme == "file") {
            uri.path?.let { File(it).inputStream() }
        } else null
    }

    override fun readDir(uri: Uri): Array<File> {
        if (uri.scheme == "file") {
            uri.path?.let {
                return File(it).listFiles() ?: emptyArray()
            }
            return emptyArray()
        } else return emptyArray()
    }


    override fun write(uri: Uri, inputStream: InputStream): Boolean {
        try {
            if (uri.scheme == "file") {
                uri.path?.let {
                    val outputStream: OutputStream = File(it).outputStream()
                    inputStream.copyTo(outputStream)
                    outputStream.close()
                    return true
                }
            }
            return false
        } catch (_: Exception) {
            return false
        } finally {
            inputStream.close()
        }
    }

    override fun delete(uri: Uri): Boolean {
        return if (uri.scheme == "file") {
            uri.path?.let { File(it).delete() } ?: false
        } else false
    }

    data class FileRequest(val uri: String, val type: String) {
        var data: String? = null
    }

    data class Result(val state: Boolean) {
        var value: String? = null
        var errMessage: String? = null
        var uri: String? = null

        constructor(uri: Uri, value: String) : this(true) {
            this.value = value
            this.uri = uri.toString()
        }

        constructor(err: Exception) : this(false) {
            this.errMessage = err.message
        }
    }
}