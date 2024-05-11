package org.autojs.autojs.tool

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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