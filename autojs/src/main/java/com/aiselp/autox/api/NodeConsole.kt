package com.aiselp.autox.api

import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.V8ValueObject
import com.stardust.autojs.runtime.api.Console

class NodeConsole(val console: Console) : NativeApi {
    override val moduleId: String = "console"
    override val globalModule: Boolean = true
    private var v8ValueObject: V8ValueObject? = null
    override fun install(v8Runtime: V8Runtime, global: V8ValueObject): NativeApi.BindingMode {
        v8Runtime.getExecutor(SCRIPT).execute<V8ValueObject>().let {
            v8ValueObject = it
            it.bind(this)
        }
        return NativeApi.BindingMode.NOT_BIND
    }

    override fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
        v8ValueObject?.use { it.unbind(this) }
        v8ValueObject = null
    }

    @V8Function
    fun error(vararg v8Value: V8Value?) = concat(*v8Value) {
        console.error(it)
    }

    fun error(vararg obj: Any?) = concat(*obj) {
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

    fun log(vararg obj: Any?) = concat(*obj) {
        console.log(it)
    }

    companion object {
        private fun concat(vararg obj: Any?, r: (str: String) -> Unit) {
            r(obj.joinTo(StringBuilder(), " ").toString().trim())
        }

        val SCRIPT = """
            (function () {
                const stream = require('node:stream');
                const nativeConsole = {}
                const out = new stream.Writable({
                    write(chunk, encoding, callback) {
                        nativeConsole.log(chunk.toString());
                        callback();
                    }
                })
                let l = console
                const errout = new stream.Writable({
                    write(chunk, encoding, callback) {
                        nativeConsole.error(chunk.toString());
                        callback();
                    }
                })
                const myConsole = new console.Console({ stdout: out, stderr: errout });
                global.console = myConsole;
                return nativeConsole;
            }())
        """.trimIndent()
    }
}