package com.stardust.pio

import android.content.Context
import android.content.res.AssetManager
import android.os.Environment
import android.util.Log
import com.stardust.util.Func1
import java.io.*
import java.nio.charset.Charset
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

/**
 * Created by Stardust on 2017/4/1.
 */
object PFiles {
    private const val TAG = "PFiles"
    const val DEFAULT_BUFFER_SIZE = 8192

    @JvmField
    val DEFAULT_ENCODING: String = Charset.defaultCharset().name()

    @JvmStatic
    fun open(path: String, mode: String, encoding: String, bufferSize: Int): PFileInterface? {
        when (mode) {
            "r" -> return PReadableTextFile(path, encoding, bufferSize)
            "w" -> return PWritableTextFile(path, encoding, bufferSize, false)
            "a" -> return PWritableTextFile(path, encoding, bufferSize, true)
        }
        return null
    }

    @JvmStatic
    fun open(path: String, mode: String, encoding: String): PFileInterface? {
        return open(path, mode, encoding, DEFAULT_BUFFER_SIZE)
    }

    @JvmStatic
    fun open(path: String, mode: String): PFileInterface? {
        return open(path, mode, DEFAULT_ENCODING, DEFAULT_BUFFER_SIZE)
    }

    @JvmStatic
    fun open(path: String): PFileInterface? {
        return open(path, "r", DEFAULT_ENCODING, DEFAULT_BUFFER_SIZE)
    }

    @JvmStatic
    fun create(path: String): Boolean {
        val f = File(path)
        if (f.exists()) return true
        return if (path.endsWith(File.separator)) {
            f.mkdir()
        } else {
            try {
                f.createNewFile()
            } catch (e: IOException) {
                false
            }
        }
    }

    @JvmStatic
    fun createIfNotExists(path: String): Boolean {
        ensureDir(path)
        val file = File(path)
        if (!file.exists()) {
            try {
                return file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return false
    }

    @JvmStatic
    fun createWithDirs(path: String): Boolean {
        return createIfNotExists(path)
    }

    @JvmStatic
    fun exists(path: String): Boolean {
        return File(path).exists()
    }

    @JvmStatic
    fun ensureDir(path: String): Boolean {
        var i = path.lastIndexOf("\\")
        if (i < 0) i = path.lastIndexOf("/")
        return if (i >= 0) {
            val folder = path.substring(0, i)
            val file = File(folder)
            if (file.exists()) true else file.mkdirs()
        } else {
            false
        }
    }

    @JvmStatic
    fun read(path: String, encoding: String): String {
        return read(File(path), encoding)
    }

    @JvmStatic
    fun read(path: String): String {
        return read(File(path))
    }

    @JvmStatic
    fun read(file: File): String {
        return file.readText()
    }

    @JvmStatic
    fun read(file: File, encoding: String = "utf-8"): String {
        return file.readText(charset(encoding))
    }

    @JvmStatic
    fun read(input: InputStream, encoding: String): String {
        return input.reader(charset(encoding)).use { it.readText() }
    }

    @JvmStatic
    fun read(inputStream: InputStream): String {
        return read(inputStream, "utf-8")
    }

    @JvmStatic
    fun readBytes(input: InputStream): ByteArray {
        return input.use { it.readBytes() }
    }

    @JvmStatic
    fun copyRaw(context: Context, rawId: Int, path: String): Boolean {
        val input = context.resources.openRawResource(rawId)
        return copyStream(input, path)
    }

    @JvmStatic
    fun copyStream(input: InputStream, path: String): Boolean {
        if (!ensureDir(path)) return false
        val file = File(path)
        if (!file.exists()) if (!file.createNewFile()) return false
        input.copyToAndClose(file.outputStream())
        return true
    }

    @JvmStatic
    fun write(input: InputStream, out: OutputStream, close: Boolean = true) {
        if (close) input.copyToAndClose(out)
        else input.copyTo(out)
    }

    @JvmStatic
    fun write(path: String, text: String) {
        File(path).writeText(text)
    }

    @JvmStatic
    fun write(path: String, text: String, encoding: String) {
        File(path).writeText(text, charset(encoding))
    }

    @JvmStatic
    fun write(file: File, text: String) {
        file.writeText(text)
    }

    @JvmStatic
    fun write(fileOutputStream: OutputStream, text: String) {
        write(fileOutputStream, text, "utf-8")
    }

    @JvmStatic
    fun write(outputStream: OutputStream, text: String, encoding: String) {
        outputStream.use { it.write(text.toByteArray(charset(encoding))) }
    }

    @JvmStatic
    fun append(path: String, text: String) {
        create(path)
        try {
            write(FileOutputStream(path, true), text)
        } catch (e: FileNotFoundException) {
            throw UncheckedIOException(e)
        }
    }

    @JvmStatic
    fun append(path: String, text: String, encoding: String) {
        create(path)
        File(path).appendText(text, charset(encoding))
    }

    @JvmStatic
    fun writeBytes(outputStream: OutputStream, bytes: ByteArray) {
        outputStream.use { it.write(bytes) }
    }

    @JvmStatic
    fun appendBytes(path: String, bytes: ByteArray) {
        create(path)
        File(path).writeBytes(bytes)
    }

    @JvmStatic
    fun writeBytes(path: String, bytes: ByteArray) {
        File(path).writeBytes(bytes)
    }

    @JvmStatic
    fun copy(pathFrom: String, pathTo: String): Boolean {
        return copyStream(FileInputStream(pathFrom), pathTo)
    }

    @JvmStatic
    fun copyAsset(context: Context, assetFile: String, path: String): Boolean {
        return try {
            copyStream(context.assets.open(assetFile), path)
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyAssetDir(
        manager: AssetManager,
        assetsDir: String,
        toDir: String,
        list: Array<String>?
    ) {
        var list1 = list
        File(toDir).mkdirs()
        if (list1 == null) {
            list1 = manager.list(assetsDir)
        }
        if (list1 == null) throw IOException("not a directory: $assetsDir")
        for (file in list1) {
            if (file.isEmpty()) continue
            val fullAssetsPath = join(assetsDir, file)
            val children = manager.list(fullAssetsPath)
            if (children.isNullOrEmpty()) {
                //空目录会报错，所以加 try catch
                try {
                    manager.open(fullAssetsPath).use {
                        copyStream(it, join(toDir, file))
                    }
                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "${e.message} is a directory", e)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                copyAssetDir(manager, fullAssetsPath, join(toDir, file), children)
            }
        }
    }

    @JvmStatic
    fun renameWithoutExtensionAndReturnNewPath(path: String, newName: String): String {
        val file = File(path)
        val newFile = File(file.parent, newName + "." + getExtension(file.name))
        file.renameTo(newFile)
        return newFile.absolutePath
    }

    @JvmStatic
    fun renameWithoutExtension(path: String, newName: String): Boolean {
        val file = File(path)
        val newFile = File(file.parent, newName + "." + getExtension(file.name))
        return file.renameTo(newFile)
    }

    @JvmStatic
    fun rename(path: String, newName: String): Boolean {
        val f = File(path)
        return f.renameTo(File(f.parent, newName))
    }

    @JvmStatic
    fun move(path: String, newPath: String): Boolean {
        val f = File(path)
        return f.renameTo(File(newPath))
    }

    @JvmStatic
    fun getExtension(fileName: String): String {
        val i = fileName.lastIndexOf('.')
        return if (i < 0 || i + 1 >= fileName.length - 1) "" else fileName.substring(i + 1)
    }

    @JvmStatic
    fun generateNotExistingPath(path: String, extension: String): String {
        if (!File(path + extension).exists()) return path + extension
        var i = 0
        while (true) {
            val pathI = "$path($i)$extension"
            if (!File(pathI).exists()) return pathI
            i++
        }
    }

    @JvmStatic
    fun getName(filePath: String): String {
        var filePath1 = filePath
        filePath1 = filePath1.replace('\\', '/')
        return File(filePath1).name
    }

    @JvmStatic
    fun getNameWithoutExtension(filePath: String): String {
        var fileName = getName(filePath)
        var b = fileName.lastIndexOf('.')
        if (b < 0) b = fileName.length
        fileName = fileName.substring(0, b)
        return fileName
    }

    @JvmStatic
    fun copyAssetToTmpFile(context: Context, path: String): File {
        val extension = "." + getExtension(path)
        var name = getNameWithoutExtension(path)
        if (name.length < 5) {
            name += name.hashCode()
        }
        val tmpFile = File.createTempFile(name, extension, context.cacheDir)
        copyAsset(context, path, tmpFile.path)
        return tmpFile
    }

    @JvmStatic
    fun deleteRecursively(file: File): Boolean {
        if (file.isFile) return file.delete()
        val children = file.listFiles()
        if (children != null) {
            for (child in children) {
                if (!deleteRecursively(child)) return false
            }
        }
        return file.delete()
    }

    @JvmStatic
    fun deleteFilesOfDir(dir: File): Boolean {
        require(dir.isDirectory) { "not a directory: $dir" }
        val children = dir.listFiles()
        if (children != null) {
            for (child in children) {
                if (!deleteRecursively(child)) return false
            }
        }
        return true
    }

    @JvmStatic
    fun remove(path: String): Boolean {
        return File(path).delete()
    }

    @JvmStatic
    fun removeDir(path: String): Boolean {
        return deleteRecursively(File(path))
    }

    @JvmStatic
    val sdcardPath: String
        get() = Environment.getExternalStorageDirectory().path

    @JvmStatic
    fun readAsset(assets: AssetManager, path: String): String {
        return read(assets.open(path))
    }

    @JvmStatic
    fun listDir(path: String): Array<String> {
        val file = File(path)
        return file.list() ?: arrayOf()
    }

    @JvmStatic
    fun listDir(path: String, filter: Func1<String, Boolean>): Array<String> {
        val file = File(path)
        return file.list { _, name -> filter.call(name) }?: arrayOf()
    }

    @JvmStatic
    fun isFile(path: String): Boolean {
        return File(path).isFile
    }

    @JvmStatic
    fun isDir(path: String): Boolean {
        return File(path).isDirectory
    }

    @JvmStatic
    fun isEmptyDir(path: String): Boolean {
        val file = File(path)
        return file.isDirectory && file.list()?.isEmpty() ?: true
    }

    @JvmStatic
    fun join(base: String, vararg paths: String): String {
        var file = File(base)
        for (path in paths) {
            file = File(file, path)
        }
        return file.path
    }

    @JvmStatic
    fun getHumanReadableSize(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = "KMGTPE".substring(exp - 1, exp)
        return String.format(
            Locale.getDefault(),
            "%.1f %sB",
            bytes / unit.toDouble().pow(exp.toDouble()),
            pre
        )
    }

    @JvmStatic
    fun getSimplifiedPath(path: String): String {
        return if (path.startsWith(Environment.getExternalStorageDirectory().path)) {
            path.substring(Environment.getExternalStorageDirectory().path.length)
        } else path
    }

    @JvmStatic
    fun readBytes(path: String): ByteArray {
        return try {
            readBytes(FileInputStream(path))
        } catch (e: FileNotFoundException) {
            throw UncheckedIOException(e)
        }
    }

    @JvmStatic
    fun closeSilently(closeable: Closeable) {
        closeable.close()
    }
}