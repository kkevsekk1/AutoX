package com.aiselp.autox.module

import android.net.Uri
import android.util.Log
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.interop.callback.IV8ModuleResolver
import com.caoccao.javet.node.modules.NodeModuleModule
import com.caoccao.javet.values.reference.IV8Module
import com.caoccao.javet.values.reference.V8Module
import com.caoccao.javet.values.reference.V8ValueFunction
import com.caoccao.javet.values.reference.V8ValueObject
import java.io.File
import java.net.URI

class NodeModuleResolver(
    val runtime: NodeRuntime,
    mainFile: File,
    private val globalModuleDirectory: File
) : IV8ModuleResolver {
    val workingDirectory: File = mainFile.parentFile ?: File("/")
    private val esModuleCache = mutableMapOf<String, IV8Module>()
    private val requireCache = mutableMapOf<String, V8ValueFunction>()
    val require: V8ValueFunction = createRequire(mainFile.absolutePath)
    private val loadFn: V8ValueFunction by lazy {
        runtime.getExecutor(
            """
        (require,filename)=>{
           const model = require(filename);
           if (model instanceof Object) {
               Object.defineProperty(model, 'default', {
                    value: model,
                    writable: false,
                    enumerable: true
                })
                return model
            } else {
                return { default:model }
            }
        }
    """.trimIndent()
        ).execute()
    }

    private fun createRequire(referrer: String): V8ValueFunction {
        return requireCache.getOrPut(referrer) {
            runtime.getNodeModule(NodeModuleModule::class.java).moduleObject
                .invoke(NodeModuleModule.FUNCTION_CREATE_REQUIRE, referrer)
        }
    }

    override fun resolve(
        v8Runtime: V8Runtime, resourceName: String, v8ModuleReferrer: IV8Module
    ): IV8Module? {
        Log.d(TAG, "resolve: $resourceName referrer: ${v8ModuleReferrer.resourceName}")
        if (resourceName.startsWith("./") || resourceName.startsWith("../")) {
            val s = URI(v8ModuleReferrer.resourceName).resolve(resourceName).toString()
            return parsingModule(v8Runtime, Uri.parse(s))
        }
        val uri = Uri.parse(resourceName)
        if (resourceName.startsWith("/")) {
            return parsingModule(v8Runtime, uri)
        }
        return when (uri.scheme) {
            "node" -> loadNodeModule(resourceName)
            null -> run {
                parsingPackageModule(v8Runtime, resourceName, v8ModuleReferrer.resourceName)
            }

            else -> parsingModule(v8Runtime, uri)
        }
    }

    private fun loadNodeModule(resourceName: String): V8Module? {
        require.call<V8ValueObject>(null, resourceName).let { module ->
            module.set("default", module)
            return runtime.createV8Module(resourceName, module)
        }
    }

    private fun checkCacheModule(resourceName: String, load: () -> IV8Module?): IV8Module? {
        esModuleCache[resourceName]?.let { return it }
        return load()?.also { addCacheModule(it) }
    }

    private fun parsingModule(v8Runtime: V8Runtime, uri: Uri): IV8Module? =
        checkCacheModule(uri.toString()) {
            if (uri.scheme == null || uri.scheme == "file") {
                val path = uri.path!!
                return@checkCacheModule parsingModule(v8Runtime, File(path))
            }
            return@checkCacheModule null
        }

    private fun parsingModule(
        v8Runtime: V8Runtime, file: File, isModule: Boolean = isEsModule(file)
    ): IV8Module? = checkCacheModule(file.path) {
        return@checkCacheModule if (isModule) {
            compileV8Module(v8Runtime, file.readText(), file.path)
        } else {
            require.call<V8ValueObject>(null, file.path).use { valueObject ->
                valueObject.set("default", valueObject)
                runtime.createV8Module(file.path, valueObject)
            }
        }
    }

    private fun loadPackageModule(
        v8Runtime: V8Runtime, directory: File?, name: String
    ): IV8Module? {
        if (directory == null) return null
        if (!directory.isDirectory) return null
        val split = name.split("/")
        val subPath = mutableListOf<String>()
        var sufPath = ""

        var packageJson: PackageJson? = null
        split.forEach {
            if (packageJson != null) {
                subPath.add(it)
                return@forEach
            }
            sufPath += "$it/"
            packageJson = PackageJson.create(File(directory, sufPath))
        }
        return packageJson?.let {
            it.resolveSubPath(
                if (subPath.isEmpty()) "."
                else subPath.joinToString("/"), true
            )?.let { modelFile ->
                parsingModule(
                    v8Runtime, modelFile.file, modelFile.modelType == ModuleFile.ModelType.ES_MODULE
                )
            }
        }
    }

    private fun loadNpmPackageModule(require: V8ValueFunction, name: String): IV8Module? =
        checkCacheModule(name) {
            try {
                runtime.createV8Module(name, loadFn.call<V8ValueObject>(null, require, name))
            } catch (e: Exception) {
                Log.e(TAG, "loadNpmPackageModule Error:\n ${e.stackTraceToString()}")
                null
            }
        }

    private fun parsingPackageModule(
        v8Runtime: V8Runtime, name: String, referrer: String
    ): IV8Module? {
        val require1 = createRequire(referrer)
        return loadNpmPackageModule(require1, name)
            ?: loadPackageModule(v8Runtime, globalModuleDirectory, name)
    }

    fun addCacheModule(module: IV8Module) {
        esModuleCache[module.resourceName] = module
    }

    fun removeCacheModule(module: IV8Module) {
        esModuleCache.remove(module.resourceName)
    }

    companion object {
        private const val TAG = "NodeModuleResolver"
        fun compileV8Module(runtime: V8Runtime, script: String, resourceName: String): IV8Module {
            val executor = runtime.getExecutor(script)
            executor.v8ScriptOrigin.resourceName = resourceName
            return executor.setModule(true).compileV8Module()
        }

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