package com.stardust.io

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object Zip {

    @JvmStatic
    fun unzip(stream: InputStream, dir: File) {
        stream.use {
            ZipInputStream(stream).use { zis ->
                var z: ZipEntry?
                while (zis.nextEntry.also { z = it } != null) {
                    val entry = z ?: continue
                    val file = File(dir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                        file.outputStream().use { fos ->
                            zis.copyTo(fos)
                            zis.closeEntry()
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun unzip(zipFile: File, dir: File) {
        unzip(FileInputStream(zipFile), dir)
    }

    fun unzip(fromFile: File, newDir: File, unzipPath: String) {
        var unzipPath1 = unzipPath
        if (!unzipPath1.endsWith("/")) unzipPath1 += "/"
        if (!newDir.exists()) newDir.mkdirs()
        var z: ZipEntry?
        ZipInputStream(fromFile.inputStream()).use { input ->
            while (input.nextEntry.also { z = it } != null) {
                val zipEntry = z ?: continue
                if (!zipEntry.isDirectory && zipEntry.name.startsWith(unzipPath1)) {
                    val f = File(newDir, zipEntry.name.replace(Regex("^$unzipPath1"), ""))
                    f.parentFile?.let {
                        if (!it.exists()) it.mkdirs()
                    }
                    f.outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
            }
        }
    }
}