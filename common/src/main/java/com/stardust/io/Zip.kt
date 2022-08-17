package com.stardust.io

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object Zip {

    @JvmStatic
    fun unzip(stream: InputStream, dir: File) {
        ZipInputStream(stream).use { zis ->
            var entry: ZipEntry
            while (zis.nextEntry.also { entry = it } != null) {
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

    @JvmStatic
    fun unzip(zipFile: File, dir: File) {
        unzip(FileInputStream(zipFile), dir)
    }
}