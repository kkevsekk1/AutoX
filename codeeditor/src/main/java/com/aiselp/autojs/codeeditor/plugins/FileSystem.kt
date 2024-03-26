package com.aiselp.autojs.codeeditor.plugins

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.aiselp.autojs.codeeditor.web.PluginManager
import com.aiselp.autojs.codeeditor.web.annotation.WebFunction
import com.google.gson.Gson
import java.io.File

@RequiresApi(Build.VERSION_CODES.M)
class FileSystem(context: Context) {
    companion object {
        const val TAG = "FileSystem"
        const val MaxFileSize = 1024 * 1024 * 5
        private val basePath: File = Environment.getExternalStorageDirectory()
        private var sampleDir: File? = null
        private val uriFiles = mutableSetOf<String>()
        private const val sampleBase = "/@Autox-sample"
        fun parsePath(path: String): File {
            if (path.startsWith(sampleBase)) {
                return File(sampleDir, path.replace(sampleBase, ""))
            }
            return File(basePath, path)
        }

        fun toWebPath(path: File): String {
            if (path.path.startsWith(basePath.path)) {
                return path.path.replace(basePath.path, "")
            }
            val samplePath = sampleDir?.path
            if (samplePath != null && path.path.startsWith(samplePath)) {
                return sampleBase + path.path.replace(samplePath, "")
            }
            return path.path
        }
    }

    private val gson = Gson()

    init {
        sampleDir = File(context.filesDir, "sample")
    }

    @WebFunction
    fun stat(call: PluginManager.WebCall) {
        Log.i(TAG, "stat: ${call.data}")
        val fileRequest = gson.fromJson(call.data, FileRequest::class.java)
        val file = parsePath(fileRequest.path)
        if (!file.isFile && !file.isDirectory) {
            return call.onError(Exception("${fileRequest.path} not file or directory"))
        }
        val stat = StatResult()
        stat.type = if (file.isFile) StatResult.FileType else StatResult.DirectoryType
        stat.size = file.length()
        stat.name = file.name
        stat.ctime = file.lastModified()
        stat.mtine = file.lastModified()
        call.onSuccess(gson.toJson(stat))
    }

    @WebFunction
    fun readDirectory(call: PluginManager.WebCall) {
        Log.i(TAG, "readDirectory: ${call.data}")
        val fileRequest = gson.fromJson(call.data, FileRequest::class.java)
        val file = parsePath(fileRequest.path)
        val list: List<StatResult> = file.listFiles()?.map {
            val stat = StatResult()
            stat.type = if (it.isFile) StatResult.FileType else StatResult.DirectoryType
            stat.size = it.length()
            stat.name = it.name
            stat.ctime = it.lastModified()
            stat.mtine = it.lastModified()
            return@map stat
        } ?: throw Error("${fileRequest.path} not directory")
        call.onSuccess(gson.toJson(list))
    }

    @WebFunction
    fun createDirectory(call: PluginManager.WebCall) {
        Log.i(TAG, "createDirectory: ${call.data}")
        val req = gson.fromJson(call.data, FileRequest::class.java)
        val path = parsePath(req.path)
        path.mkdirs()
        call.onSuccess(null)
    }

    @WebFunction
    fun readFile(call: PluginManager.WebCall) {
        Log.i(TAG, "readFile: ${call.data}")
        val req = gson.fromJson(call.data, FileRequest::class.java)
        val file = parsePath(req.path)
        if (file.isFile) {
            if (file.length() > MaxFileSize) throw Exception("${req.path} too large")
            val b64 = Base64.encodeToString(file.readBytes(), Base64.DEFAULT)
            call.onSuccess(b64)
        } else call.onError(Exception("${req.path} not file"))
    }

    @WebFunction
    fun writeFile(call: PluginManager.WebCall) {
        Log.i(TAG, "writeFile: ${call.data}")
        val req = gson.fromJson(call.data, FileRequest::class.java)
        val file = parsePath(req.path)
        val data = Base64.decode(req.data!!, Base64.DEFAULT)
        file.writeBytes(data)
        call.onSuccess(null)
    }

    @WebFunction
    fun delete(call: PluginManager.WebCall) {
        Log.i(TAG, "delete: ${call.data}")
        val req = gson.fromJson(call.data, FileRequest::class.java)
        val path = parsePath(req.path)
        path.delete()
        call.onSuccess(null)
    }

    @WebFunction
    fun rename(call: PluginManager.WebCall) {
        Log.i(TAG, "rename: ${call.data}")
        val req = gson.fromJson(call.data, RenameFileRequest::class.java)
        val form = parsePath(req.form)
        val to = parsePath(req.to)
        form.renameTo(to)
        call.onSuccess(null)
    }

    @WebFunction
    fun copy(call: PluginManager.WebCall) {
        Log.i(TAG, "copy: ${call.data}")
        val req = gson.fromJson(call.data, CopyFileRequest::class.java)
        val form = parsePath(req.form)
        val to = parsePath(req.to)
        form.copyTo(to)
        call.onSuccess(null)
    }

    class FileRequest {
        var path: String = "null"
        var data: String? = null
        var encoding: String? = null
    }

    class RenameFileRequest {
        var form: String = "null"
        var to: String = "null"
    }

    class CopyFileRequest {
        var form: String = "null"
        var to: String = "null"
    }

    class StatResult {
        companion object {
            const val FileType = "file"
            const val DirectoryType = "directory"
        }

        var type: String = FileType
        var name: String? = null
        var size: Long = 0
        var ctime: Long = 0
        var mtine: Long = 0
    }
}