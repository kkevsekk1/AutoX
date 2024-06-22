package com.aiselp.autox.api

import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueObject

interface NativeApi {
    val globalModule: Boolean
        get() = false
    val moduleId: String
    fun install(v8Runtime: V8Runtime, global: V8ValueObject): BindingMode
    fun recycle(v8Runtime: V8Runtime, global: V8ValueObject)

    enum class BindingMode {
        NOT_BIND, NATIVE, PROXY
    }
}