package com.aiselp.autox.engine

import android.content.Context
import com.aiselp.autox.api.NativeApi
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.annotations.V8Property
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.V8ValueError
import com.caoccao.javet.values.reference.V8ValueObject
import com.stardust.autojs.runtime.exception.ScriptException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

class NativeApiManager(engine: NodeScriptEngine) {
    private val apis = mutableMapOf<String, NativeApi>()
    private val rootObject = RootObject(engine)
    fun register(api: NativeApi) {
        check(!apis.contains(api.moduleId)) { "api id: ${api.moduleId} already registered" }
        apis[api.moduleId] = api
    }

    fun initialize(v8Runtime: V8Runtime, global: V8ValueObject) {
        v8Runtime.createV8ValueObject().use { autoxObject ->
            autoxObject.bind(rootObject)
            global.set(INSTANCE_NAME, autoxObject)
            for (api in apis.values) {
                val bindingMode = api.install(v8Runtime, global)
                when (bindingMode) {
                    NativeApi.BindingMode.NOT_BIND -> {}
                    NativeApi.BindingMode.NATIVE -> {
                        autoxObject.set(api.moduleId, api)
                        if (api.globalModule) {
                            global.set(api.moduleId, api)
                        }
                    }

                    NativeApi.BindingMode.PROXY -> v8Runtime.createV8ValueObject()
                        .use { apiObject ->
                            apiObject.bind(api)
                            autoxObject.set(api.moduleId, apiObject)
                            if (api.globalModule) {
                                global.set(api.moduleId, apiObject)
                            }
                        }
                }
            }
        }
    }

    fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
        for (obj in apis.values) {
            try {
                obj.recycle(v8Runtime, global)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        apis.clear()
    }

    private class RootObject(private val engine: NodeScriptEngine) {
        @V8Property
        val context: Context = engine.context.applicationContext

        @V8Function
        fun exit(e: V8Value?) {
            if (e is V8ValueError) {
                val stack = e.stack
                val message = e.getString("message")
                Thread {
                    engine.forceStop(message ?: "unknown error", stack ?: "")
                }.start()
            } else {
                val err = ScriptException(e.toString())
                Thread {
                    engine.forceStop(err)
                }.start()
            }
        }

        @V8Function
        fun safeExit() {
            if (engine.scope.isActive) engine.scope.cancel()
        }
    }

    companion object {
        private const val INSTANCE_NAME = "Autox"
    }
}