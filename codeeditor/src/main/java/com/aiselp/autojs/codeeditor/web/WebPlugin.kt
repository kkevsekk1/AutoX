package com.aiselp.autojs.codeeditor.web

import com.aiselp.autojs.codeeditor.web.annotation.WebFunction
import java.lang.reflect.Method

class WebPlugin(val id: String, val plugin: Any) {
    val methods: MutableMap<String, Method> = mutableMapOf()
    init {
        for (method in plugin.javaClass.methods) {
            val webFunction = method.getAnnotation(WebFunction::class.java)
            if (webFunction != null) {
                val name = if (webFunction.name == "null") method.name else webFunction.name
                methods[name] = method
            }
        }
    }
}