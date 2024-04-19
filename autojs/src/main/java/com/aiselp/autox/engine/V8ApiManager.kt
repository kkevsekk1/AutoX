package com.aiselp.autox.engine

import com.aiselp.autox.api.V8Api
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueObject

class V8ApiManager {
    private val apis = mutableMapOf<String, V8Api>()
    private val autoBind = mutableMapOf<V8ValueObject, V8Api>()

    fun register(obj: V8Api) {
        check(!apis.contains(obj.moduleId)) { "api ${obj.moduleId} already registered" }
        apis[obj.moduleId] = obj
    }

    fun initialize(v8Runtime: V8Runtime, global: V8ValueObject) {
        for (obj in apis.values) {
            val bindingMode = obj.install(v8Runtime, global)
            when (bindingMode) {
                V8Api.BindingMode.AUTO_BIND -> bindGlobal(v8Runtime, global, obj)

                V8Api.BindingMode.NOT_AUTO_BIND -> {}
            }
        }
    }

    fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
        for (obj in apis.values) {
            obj.recycle(v8Runtime, global)
        }
        apis.clear()
        for ((v8ValueObject, api) in autoBind) {
            v8ValueObject.use { it.unbind(api) }
        }
        autoBind.clear()
    }

    private fun bindGlobal(v8Runtime: V8Runtime, global: V8ValueObject, api: V8Api) {
        val v8ValueObject = v8Runtime.createV8ValueObject()
        v8ValueObject.bind(api)
        global.set(api.moduleId, v8ValueObject)
        autoBind[v8ValueObject] = api
    }
}