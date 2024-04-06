package com.aiselp.autox.api

import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.V8Value
import com.stardust.autojs.runtime.api.Console

class NodeConsole(val console: Console) {
    @V8Function
    fun error(vararg v8Value: V8Value?) = concat(*v8Value) {
        console.error(it)
    }

    @V8Function
    fun info(vararg v8Value: V8Value?) = concat(*v8Value) {
        console.info(it)
    }

    @V8Function
    fun log(vararg v8Value: V8Value?) = concat(*v8Value) {
        console.log(it)
    }

    fun register(v8Runtime: V8Runtime) = v8Runtime.globalObject.use {
        it.getObject<V8Value>("process").use { process ->

        }
    }

    companion object {
        private fun concat(vararg v8Values: V8Value?, r: (str: String) -> Unit) {
            r(v8Values.joinTo(StringBuilder(), " ").toString())
        }
    }
}