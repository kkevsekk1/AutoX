package com.aiselp.autox.module

import com.google.gson.Gson
import java.io.File
import java.net.URI

class PackageJson(val dir: File, private val data: Map<String, Any?>) {

    val exports: Map<String, Any?>? by lazy {
        when (data["exports"]) {
            null -> null
            is String -> mapOf("." to data["exports"] as String)
            else -> {
                data["exports"] as Map<String, Any?>
            }
        }
    }

    val main: ModuleFile?
        get() {
            return if (exports == null) {
                data["main"]?.let { createModuleFile(it as String) }
            } else resolveSubPath(".", false)
        }

    private fun createModuleFile(subPath: String, import: Boolean = isEsModule()): ModuleFile {
        return ModuleFile(
            File(URI(dir.path + "/#").resolve(subPath).path),
            if (import) ModuleFile.ModelType.ES_MODULE
            else ModuleFile.ModelType.COMMONJS
        )
    }

    fun resolveSubPath(subPath: String, import: Boolean): ModuleFile? {
        val e = exports ?: return main
        val c = e[subPath]

        fun select(m: Map<*, *>): ModuleFile? {
            val es = (m["import"] as? String)?.let {
                createModuleFile(it, true)
            }
            val commonjs = (m["require"] as? String)?.let {
                createModuleFile(it, false)
            }
            return if (import) {
                es ?: commonjs
            } else commonjs ?: es
        }
        if (subPath == ".") {
            if (c == null) {
                return select(e)
            }
        }

        if (c is String) return createModuleFile(c)
        return if (c is Map<*, *>) {
            select(c)
        } else null
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