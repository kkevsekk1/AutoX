package com.stardust.autojs.rhino

import com.stardust.automator.UiObject
import org.mozilla.javascript.NativeJavaMethod
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Wrapper
import java.lang.reflect.Method

class UiObjectProxy(private val uiObject: UiObject) : NativeObject(), Wrapper {
    private val methods = mutableMapOf<String, Method>().apply {
        for (method in uiObject.javaClass.methods) {
            this[method.name] = method
        }
    }
    private val jsMethods = mutableMapOf<String, NativeJavaMethod>()

    override fun get(name: String?, start: Scriptable?): Any {
        return getMethod(name!!) ?: super.get(name, start)
    }

    override fun has(name: String?, start: Scriptable?): Boolean {
        return methods.containsKey(name) || super.has(name, start)
    }

    override fun getClassName() = "UiObject"

    @Synchronized
    fun getMethod(name: String): NativeJavaMethod? {
        return jsMethods[name] ?: methods[name]?.let {
            val jsMethod = NativeJavaMethod(it, name)
            jsMethods[name] = jsMethod
            jsMethod
        }
    }

    override fun unwrap(): UiObject {
        return uiObject
    }
}