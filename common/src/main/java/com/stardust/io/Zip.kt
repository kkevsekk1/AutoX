package com.stardust.io

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object Zip {
    fun unzip(fromFile: File, newDir: File, unzipPath: String) {
        var unzipPath1 = unzipPath
        if (!unzipPath1.endsWith("/")) unzipPath1 += "/"
        if (!newDir.exists()) newDir.mkdirs()
        val regex = Regex("^$unzipPath1")
        ZipInputStream(fromFile.inputStream()).use { input ->
            generateSequence { input.nextEntry }
                .filterNotNull()
                .filter { !it.isDirectory && it.name.startsWith(unzipPath1) }
                .map {
                    val f = File(newDir, it.name.replace(regex, ""))
                    f.parentFile?.let {
                        if (!it.exists()) it.mkdirs()
                    }
                    f.outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
        }
    }
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
}