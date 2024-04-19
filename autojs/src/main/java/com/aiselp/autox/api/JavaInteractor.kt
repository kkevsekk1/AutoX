package com.aiselp.autox.api

import android.util.Log
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.interop.converters.JavetObjectConverter
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.V8ValueObject
import com.caoccao.javet.values.reference.V8ValuePromise
import com.stardust.autojs.runtime.exception.ScriptException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class JavaInteractor(val scope: CoroutineScope, private val converter: JavetObjectConverter) :
    V8Api {
    override val globalModule: Boolean = true
    override val moduleId: String = "java"
    private lateinit var v8Runtime: V8Runtime
    override fun install(v8Runtime: V8Runtime, global: V8ValueObject): V8Api.BindingMode {
        this.v8Runtime = v8Runtime
        return V8Api.BindingMode.AUTO_BIND
    }

    override fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {

    }

    private fun asyncInvoke(
        con: CoroutineDispatcher,
        func: () -> Any?
    ): V8ValuePromise {
        val v8ValuePromise = v8Runtime.createV8ValuePromise()
        scope.launch(con) {
            try {
                delay(1000)
                v8ValuePromise.resolve(func())
            } catch (e: Throwable) {
                v8ValuePromise.reject(e)
            }
        }
        return v8ValuePromise.promise
    }

    @V8Function
    fun loadClass(className: String): Class<*> {
        return Class.forName(className)
    }

    @V8Function
    fun invoke(vararg args: V8Value?): Any? {
        val exec = findMethod(args.toList())
        return exec()
    }

    @V8Function
    fun invokeIo(vararg args: V8Value?): Any? =
        asyncInvoke(Dispatchers.IO, findMethod(args.toList()))

    @V8Function
    fun invokeUi(vararg args: V8Value?): Any? =
        asyncInvoke(Dispatchers.Main, findMethod(args.toList()))

    @V8Function
    fun invokeDefault(vararg args: V8Value?): Any? =
        asyncInvoke(Dispatchers.Default, findMethod(args.toList()))

    private fun findMethod(args: List<V8Value?>): () -> Any? {
        val argList = args.map { converter.toObject<Any?>(it) }
        Log.i(TAG, "invoke  $args")
        check(argList.size >= 2) { ScriptException("invoke args size must >= 2") }
        val javaObj = argList[0] as Any
        val method = argList[1] as String
        val javaArgs = argList.drop(2).toTypedArray()
        val argTpyes = javaArgs.map { it.javaClass }
        val method1 = javaObj.javaClass.methods.find {
            if (it.name == method) {
                if (it.parameterCount != argTpyes.size) return@find false
                if (it.parameterCount == 0) return@find true
                val parameterTypes = it.parameterTypes
                for (i: Int in parameterTypes.indices) {
                    if (parameterTypes[i] != argTpyes[i]) return@find false
                }
                true
            } else false
        }
        checkNotNull(method1) { ScriptException("method not found args: $argTpyes") }
        return { method1.invoke(javaObj, *javaArgs) }
    }

    companion object {
        private const val TAG = "JavaInteractor"
    }
}