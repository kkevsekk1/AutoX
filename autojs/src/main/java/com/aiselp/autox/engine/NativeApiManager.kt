package com.aiselp.autox.engine

import com.aiselp.autox.api.NativeApi
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueObject

class NativeApiManager {
    private val apis = mutableMapOf<String, NativeApi>()

    fun register(api: NativeApi) {
        check(!apis.contains(api.moduleId)) { "api id: ${api.moduleId} already registered" }
        apis[api.moduleId] = api
    }

    fun initialize(v8Runtime: V8Runtime, global: V8ValueObject) {
        v8Runtime.createV8ValueObject().use { autoxObject ->
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
            obj.recycle(v8Runtime, global)
        }
        apis.clear()
    }


    companion object {
        private const val INSTANCE_NAME = "Autox"
    }
}