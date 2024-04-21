package com.aiselp.autox.module

import android.net.Uri
import android.util.Log
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.interop.callback.JavetBuiltInModuleResolver
import com.caoccao.javet.values.reference.IV8Module
import java.io.File
import java.net.URI

class NodeModuleResolver(val workingDirectory: File) : JavetBuiltInModuleResolver() {
    private val esModuleCache = mutableMapOf<String, IV8Module>()

    override fun resolve(
        v8Runtime: V8Runtime, resourceName: String, v8ModuleReferrer: IV8Module
    ): IV8Module? {
        Log.d(TAG, "resolve: $resourceName referrer: ${v8ModuleReferrer.resourceName}")
        if (resourceName.startsWith("./") || resourceName.startsWith("../")) {
            val s = URI(v8ModuleReferrer.resourceName).resolve(resourceName).toString()
            return parsingModule(v8Runtime, Uri.parse(s))
        }
        val uri = Uri.parse(resourceName)
        return if (uri.scheme == null) {
            super.resolve(v8Runtime, "node:$resourceName", v8ModuleReferrer)
                ?: parsingPackageModule(v8Runtime, workingDirectory, resourceName)
        } else parsingModule(v8Runtime, uri)
    }

    private fun checkCacheModule(resourceName: String, load: () -> IV8Module?): IV8Module? {
        esModuleCache[resourceName]?.let { return it }
        return load()?.also { addCacheModule(it) }
    }

    private fun parsingModule(v8Runtime: V8Runtime, uri: Uri): IV8Module? =
        checkCacheModule(uri.toString()) {
            if (uri.scheme == null || uri.scheme == "file") {
                return@checkCacheModule parsingModule(v8Runtime, File(uri.path!!))
            }
            return@checkCacheModule null
        }

    private fun parsingModule(
        v8Runtime: V8Runtime, file: File, isModule: Boolean = isEsModule(file)
    ): IV8Module? = checkCacheModule(file.path) {
        return@checkCacheModule if (isModule) {
            v8Runtime.getExecutor(file).setResourceName(file.path)
                .compileV8Module()
        } else {
            v8Runtime.getExecutor(
                "export default Promise.resolve().then(() => require(`${file.path}`));"
            ).setResourceName(file.path)
                .compileV8Module()
        }
    }

    private fun parsingPackageModule(
        v8Runtime: V8Runtime, workingDirectory: File?, name: String
    ): IV8Module? {
        if (workingDirectory == null) return null
        val modulesDirection = File(workingDirectory, "node_modules")
        if (!modulesDirection.isDirectory) return null
        val packageDirectory = File(modulesDirection, name)
        return PackageJson.create(packageDirectory)?.let {
            it.main?.let { file -> parsingModule(v8Runtime, file, it.isEsModule()) }
        }
    }

    fun addCacheModule(module: IV8Module) {
        esModuleCache[module.resourceName] = module
    }

    fun removeCacheModule(module: IV8Module) {
        esModuleCache.remove(module.resourceName)
    }

    companion object {
        private const val TAG = "NodeModuleResolver"
        fun isEsModule(file: File): Boolean {
            return if (file.isFile) {
                if (file.path.endsWith(".mjs")) return true
                if (file.path.endsWith(".cjs")) return false
                if (file.path.endsWith(".js")) {
                    file.parentFile?.let { PackageJson.create(it)?.isEsModule() } ?: false
                } else false
            } else {
                PackageJson.create(file)?.isEsModule() ?: false
            }
        }
    }
}