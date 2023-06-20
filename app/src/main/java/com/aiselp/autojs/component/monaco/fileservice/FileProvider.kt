package com.aiselp.autojs.component.monaco

import android.net.Uri

interface FileProvider {
    fun read(uri: Uri)
    fun readdir(uri: Uri)
    fun write(uri: Uri, text: String)
    fun delete(uri: Uri)
}