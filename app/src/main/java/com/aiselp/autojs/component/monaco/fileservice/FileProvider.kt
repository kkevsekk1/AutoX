package com.aiselp.autojs.component.monaco.fileservice

import android.net.Uri
import java.io.File
import java.io.InputStream

interface FileProvider {
    fun read(uri: Uri):InputStream?
    fun readDir(uri: Uri):Array<File>
    fun write(uri: Uri, inputStream: InputStream):Boolean
    fun delete(uri: Uri):Boolean
}