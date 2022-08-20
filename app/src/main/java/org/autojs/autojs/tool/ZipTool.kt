package org.autojs.autojs.tool

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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