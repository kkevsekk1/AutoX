package com.aiselp.autox.api

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.os.Build
import androidx.annotation.RequiresApi
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueObject

class JsMedia(private val context: Context) : NativeApi {
    override val moduleId: String = "media"
    private val mediaScanner = MediaScannerConnection(context, null)
    private val recycleSet = mutableSetOf<MediaPlayer>()
    override fun install(v8Runtime: V8Runtime, global: V8ValueObject): NativeApi.BindingMode {
        return NativeApi.BindingMode.PROXY
    }

    override fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
        mediaScanner.disconnect()
        recycleSet.forEach { it.release() }
        recycleSet.clear()
    }

    @V8Function
    fun scanFile(path: String) {
        mediaScanner.scanFile(path, null)
    }

    @V8Function
    fun createMediaPlayer(): MediaPlayer {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            MediaPlayerWrapper(recycleSet, context)
        } else {
            MediaPlayerWrapper(recycleSet)
        }
    }

    class MediaPlayerWrapper : MediaPlayer {
        private val recycleSet: MutableSet<MediaPlayer>

        constructor(recycleSet: MutableSet<MediaPlayer>) : super() {
            this.recycleSet = recycleSet
            recycleSet.add(this)
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        constructor(recycleSet: MutableSet<MediaPlayer>, context: Context) : super(context) {
            this.recycleSet = recycleSet
            recycleSet.add(this)
        }

        override fun release() {
            super.release()
            recycleSet.remove(this)
        }
    }
}