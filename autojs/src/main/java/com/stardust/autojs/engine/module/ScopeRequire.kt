package com.stardust.autojs.engine.module

import org.mozilla.javascript.*
import org.mozilla.javascript.commonjs.module.ModuleScope
import org.mozilla.javascript.commonjs.module.ModuleScript
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider

import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.ConcurrentHashMap


open class ScopeRequire(
    cx: Context, private val nativeScope: Scriptable,
    private val moduleScriptProvider: ModuleScriptProvider, private val preExec: Script?,
    private val postExec: Script?, private val sandboxed: Boolean = true
) : BaseFunction() {
    private var paths: Scriptable? = null
    private var mainModuleId: String? = null
    private var mainExports: Scriptable? = null

    // Modules that completed loading; visible to all threads
    private val exportedModuleInterfaces: MutableMap<String, Scriptable?> = ConcurrentHashMap()
    private val loadLock = Any()

    constructor(cx: Context, nativeScope: Scriptable, moduleScriptProvider: ModuleScriptProvider)
            : this(cx, nativeScope, moduleScriptProvider, null, null, false)

    init {
        prototype = getFunctionPrototype(nativeScope)
        if (!sandboxed) {
            paths = cx.newArray(nativeScope, 0)
            defineReadOnlyProperty(this, "paths", paths)
        } else paths = null
    }


    fun requireMain(cx: Context, mainModuleId: String): Scriptable? {
        if (this.mainModuleId != null) {
            if (this.mainModuleId != mainModuleId) {
                throw IllegalStateException("Main module already set to " + this.mainModuleId)
            }
            return mainExports
        }
        val moduleScript: ModuleScript? = try {
            moduleScriptProvider.getModuleScript(cx, mainModuleId, null, null, paths)
        } catch (x: RuntimeException) {
            throw x
        } catch (x: Exception) {
            throw RuntimeException(x)
        }
        if (moduleScript != null) {
            mainExports = getExportedModuleInterface(
                cx, mainModuleId,
                null, null, true
            )
        } else if (!sandboxed) {
            var mainUri: URI? = try {
                URI(mainModuleId)
            } catch (_: URISyntaxException) {
                null
            }
            if (mainUri == null || !mainUri.isAbsolute) {
                val file = File(mainModuleId)
                if (!file.isFile) {
                    throw ScriptRuntime.throwError(
                        cx, nativeScope,
                        "Module \"$mainModuleId\" not found."
                    )
                }
                mainUri = file.toURI()
            }
            mainExports = getExportedModuleInterface(
                cx, mainUri.toString(),
                mainUri, null, true
            )
        }
        this.mainModuleId = mainModuleId
        return mainExports
    }

    fun install(scope: Scriptable?) {
        putProperty(scope, "require", this)
    }

    override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>?): Any {
        if (args == null || args.isEmpty()) {
            throw ScriptRuntime.throwError(
                cx, scope,
                "require() needs one argument"
            )
        }
        var id = Context.jsToJava(args[0], String::class.java) as String
        var uri: URI? = null
        var base: URI? = null
        if (id.startsWith("./") || id.startsWith("../")) {
            if (thisObj !is ModuleScope) {
                throw ScriptRuntime.throwError(
                    cx, scope,
                    "Can't resolve relative module ID \"" + id +
                            "\" when require() is used outside of a module"
                )
            }
            base = thisObj.base
            val current = thisObj.uri
            uri = current.resolve(id)
            if (base == null) {
                id = uri.toString()
            } else {
                id = base.relativize(current).resolve(id).toString()
                if (id[0] == '.') {
                    if (sandboxed) {
                        throw ScriptRuntime.throwError(
                            cx, scope,
                            "Module \"$id\" is not contained in sandbox."
                        )
                    }
                    id = uri.toString()
                }
            }
        }
        return (getExportedModuleInterface(cx, id, uri, base, false))!!
    }

    override fun construct(cx: Context, scope: Scriptable, args: Array<Any>): Scriptable {
        throw ScriptRuntime.throwError(
            cx, scope,
            "require() can not be invoked as a constructor"
        )
    }

    private fun getExportedModuleInterface(
        cx: Context, id: String, uri: URI?, base: URI?, isMain: Boolean
    ): Scriptable? {
        // Check if the requested module is already completely loaded
        var exports = exportedModuleInterfaces[id]
        if (exports != null) {
            if (isMain) {
                throw IllegalStateException("Attempt to set main module after it was loaded")
            } else
                return exports
        }
        var threadLoadingModules: MutableMap<String, Scriptable>? =
            loadingModuleInterfaces.get() as? MutableMap<String, Scriptable>
        exports = threadLoadingModules?.get(id)
        if (exports != null) return exports

        synchronized(loadLock) {
            exports = exportedModuleInterfaces[id]
            if (exports != null) return exports

            val moduleScript: ModuleScript = getModule(cx, id, uri, base)
            if (sandboxed && !moduleScript.isSandboxed) {
                throw ScriptRuntime.throwError(
                    cx, nativeScope, ("Module \"$id\" is not contained in sandbox.")
                )
            }
            exports = cx.newObject(nativeScope)
            val outermostLocked: Boolean = threadLoadingModules == null
            if (outermostLocked) {
                threadLoadingModules = HashMap()
                loadingModuleInterfaces.set(threadLoadingModules)
            }

            threadLoadingModules?.set(id, exports!!)
            try {
                val newExports: Scriptable = executeModuleScript(
                    cx, id, exports,
                    moduleScript, isMain
                )
                if (exports !== newExports) {
                    threadLoadingModules?.put(id, newExports)
                    exports = newExports
                }
            } catch (e: RuntimeException) {
                threadLoadingModules?.remove(id)
                throw e
            } finally {
                if (outermostLocked) {
                    exportedModuleInterfaces.putAll((threadLoadingModules!!))
                    loadingModuleInterfaces.set(null)
                }
            }
        }
        return exports
    }

    private fun executeModuleScript(
        cx: Context, id: String,
        exports: Scriptable?, moduleScript: ModuleScript, isMain: Boolean
    ): Scriptable {
        val moduleObject = cx.newObject(nativeScope) as ScriptableObject
        val uri = moduleScript.uri
        val base = moduleScript.base
        defineReadOnlyProperty(moduleObject, "id", id)
        if (!sandboxed) {
            defineReadOnlyProperty(moduleObject, "uri", uri.toString())
        }
        val executionScope: Scriptable = ModuleScope(nativeScope, uri, base)
        executionScope.put("__filename", executionScope, File(uri.path).path)
        executionScope.put("__dirname", executionScope, File(uri.path).parent)
        executionScope.put("exports", executionScope, exports)
        executionScope.put("module", executionScope, moduleObject)
        moduleObject.put("exports", moduleObject, exports)
        install(executionScope)
        if (isMain) {
            defineReadOnlyProperty(this, "main", moduleObject)
        }
        //创建新作用域
        val funScope = cx.newObject(executionScope)
        funScope.parentScope = executionScope

        executeOptionalScript(preExec, cx, funScope)
        moduleScript.script.exec(cx, funScope)
        executeOptionalScript(postExec, cx, funScope)
        return ScriptRuntime.toObject(
            cx, nativeScope,
            getProperty(moduleObject, "exports")
        )
    }

    private fun getModule(cx: Context, id: String, uri: URI?, base: URI?): ModuleScript {
        try {
            return moduleScriptProvider.getModuleScript(cx, id, uri, base, paths)
                ?: throw ScriptRuntime.throwError(
                    cx, nativeScope, ("Module \"$id\" not found.")
                )
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw Context.throwAsScriptRuntimeEx(e)
        }
    }

    override fun getFunctionName() = "require"
    override fun getArity() = 1
    override fun getLength() = 1

    companion object {
        private const val serialVersionUID = 1L

        private val loadingModuleInterfaces = ThreadLocal<Map<String, Scriptable>>()
        private fun executeOptionalScript(
            script: Script?, cx: Context,
            executionScope: Scriptable
        ) {
            script?.exec(cx, executionScope)
        }

        private fun defineReadOnlyProperty(
            obj: ScriptableObject,
            name: String, value: Any?
        ) {
            putProperty(obj, name, value)
            obj.setAttributes(
                name, READONLY or
                        PERMANENT
            )
        }
    }
}