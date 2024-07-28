package com.aiselp.autox.api

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.aiselp.autox.engine.EventLoopQueue
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueFunction
import com.caoccao.javet.values.reference.V8ValueObject

class JsClipManager(context: Context, private val eventLoopQueue: EventLoopQueue) : NativeApi {
    override val moduleId: String = "clipManager"
    private val clipboard: ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private lateinit var jsListener: V8ValueFunction
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        eventLoopQueue.addTask {
            jsListener.callVoid(null, null)
        }
    }

    override fun install(v8Runtime: V8Runtime, global: V8ValueObject): NativeApi.BindingMode {
        return NativeApi.BindingMode.PROXY
    }

    override fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
        clipboard.removePrimaryClipChangedListener(listener)
        if (::jsListener.isInitialized) jsListener.close()
    }

    @V8Function
    fun registerListener(listener: V8ValueObject) {
        jsListener = listener.get("onClipChanged")
        clipboard.addPrimaryClipChangedListener(this.listener)
    }

    @V8Function
    fun getClip(): String {
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
    }

    @V8Function
    fun hasClip(): Boolean {
        return clipboard.hasPrimaryClip()
    }

    @V8Function
    fun clearClip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else setClip("")
    }

    @V8Function
    fun setClip(text: String) {
        val clip = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(clip)
    }

}