package com.aiselp.autox.api

import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueObject

interface V8Api {
    val globalModule: Boolean
        get() = false
    val moduleId: String
    fun install(v8Runtime: V8Runtime, global: V8ValueObject): BindingMode
    fun recycle(v8Runtime: V8Runtime, global: V8ValueObject)

    enum class BindingMode {
        NOT_AUTO_BIND, AUTO_BIND
    }
}