package com.aiselp.autox.api

import android.content.Context
import android.widget.Toast
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.annotations.V8Property
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class JsToast(private val context: Context, private val scope: CoroutineScope) : NativeApi {
    override val moduleId: String = "toast"

    @get:V8Property(name = "SHORT")
    val SHORT = Toast.LENGTH_SHORT

    @get:V8Property(name = "LONG")
    val LONG = Toast.LENGTH_LONG

    @OptIn(DelicateCoroutinesApi::class)
    @V8Function
    fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) =
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, msg, duration).show()
        }

    override fun install(v8Runtime: V8Runtime, global: V8ValueObject): NativeApi.BindingMode {
        return NativeApi.BindingMode.PROXY
    }

    override fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {

    }

}