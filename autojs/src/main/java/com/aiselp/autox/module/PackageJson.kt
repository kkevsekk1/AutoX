package com.aiselp.autox.module

import com.google.gson.Gson
import java.io.File

class PackageJson(val dir: File, private val data: Map<String, Any?>) {

    val exports: Map<String, String>? by lazy {
        when (data["exports"]) {
            null -> null
            is String -> mapOf("." to data["exports"] as String)
            else -> {
                val map = mutableMapOf<String, String>()
                for ((k, v) in data["exports"] as Map<String, Any?>) {
                    if (v is String) map[k] = v
                }
                map
            }
        }
    }

    val main: File?
        get() {
            val main = exports?.let {
                (if (isEsModule()) {
                    it["require"]
                } else it["import"]) ?: it["."]
            } ?: (data["main"] as? String) ?: (data["module"] as? String)
            return main?.let {
                File(dir, it.replace(Regex("^\\./"), ""))
            }
        }

    fun isEsModule(): Boolean {
        return data["type"] == "module"
    }

    fun isCommonjs(): Boolean = !isEsModule()

    companion object {
        private val gson = Gson()
        private const val MAX_SIZE = 1024 * 1024

        fun create(dir: File): PackageJson? {
            val file = File(dir, "package.json")
            return try {
                check(file.length() < MAX_SIZE) { "package.json too large" }
                val data = gson.fromJson<Map<String, Any?>>(file.reader(), Map::class.java)
                PackageJson(dir, data)
            } catch (e: Throwable) {
                null
            }
        }
    }
}