package com.stardust.autojs.engine.module

import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Script
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.commonjs.module.ModuleScope
import org.mozilla.javascript.commonjs.module.ModuleScript
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider
import java.io.File
import java.net.URI


class ScopeRequire(
    cx: Context, private val nativeScope: Scriptable,
    private val moduleScriptProvider: ModuleScriptProvider,
    private val preExec: Script?,
    private val postExec: Script?, private val sandboxed: Boolean = true
) : BaseFunction() {
    private var paths: Scriptable? = null

    private val moduleCache = mutableMapOf<String, Any?>()
    private val loadingModules: MutableMap<String, Any?> = mutableMapOf()

    private var mainExports: Any? = null
    var mainModuleId: String? = null

    constructor(cx: Context, nativeScope: Scriptable, moduleScriptProvider: ModuleScriptProvider)
            : this(cx, nativeScope, moduleScriptProvider, null, null, false)

    init {
        prototype = getFunctionPrototype(nativeScope)
        if (!sandboxed) {
            paths = cx.newArray(nativeScope, 0)
            defineReadOnlyProperty(this, "paths", paths)
        } else paths = null
    }

    @Synchronized
    private fun loadModule(
        cx: Context,
        require: String,
        currentModule: ModuleScope?
    ): ModuleScript? {
        val uri: URI? = currentModule?.uri
        val base: URI? = currentModule?.base
        if (require.startsWith("./") || require.startsWith("../")) {
            val resDir = uri ?: (mainModuleId?.let { URI.create(it) }) ?: return null
            val newUri = resDir.resolve(require)
            return getModule(cx, newUri.toString(), newUri, base)
        } else {
            return getModule(cx, require, null, null)
        }
    }

    fun requireMain(cx: Context, mainModuleId: String): Any? {
        if (mainExports != null) {
            throw IllegalStateException("Main module already set to " + this.mainModuleId)
        }

        val moduleScript: ModuleScript? = try {
            moduleScriptProvider.getModuleScript(cx, mainModuleId, null, null, paths)
        } catch (x: Exception) {
            throw if (x is RuntimeException) x else RuntimeException(x)
        }
        moduleScript ?: throw ScriptRuntime.throwError(
            cx, nativeScope,
            "Module \"$mainModuleId\" not found."
        )

        mainExports = getExportedModuleInterface(
            cx, mainModuleId,
            moduleScript, true
        )
        this.mainModuleId = uriToId(moduleScript.uri)
        return mainExports
    }

    private fun uriToId(uri: URI): String {
        return if (uri.scheme == "file") {
            uri.path
        } else uri.toString()
    }

    fun install(scope: Scriptable?) {
        putProperty(scope, "require", this)
    }

    override fun call(
        cx: Context,
        scope: Scriptable,
        thisObj: Scriptable,
        args: Array<Any>?
    ): Any? {
        if (args.isNullOrEmpty()) {
            throw ScriptRuntime.throwError(
                cx, scope, "require() needs one argument"
            )
        }
        val require = Context.toString(args[0])
        val moduleRef = thisObj as? ModuleScope
        val moduleScript = loadModule(cx, require, moduleRef) ?: throw ScriptRuntime.throwError(
            cx, scope, "Can't resolve relative module ID \"" + require +
                    "\" when require() is used outside of a module"
        )
        val id = uriToId(moduleScript.uri)
//        println("加载模块：$require id: $id")
        val e = getExportedModuleInterface(cx, id, moduleScript, false)
//        println("加载结束：$require, exports = ${e?.javaClass}")
        return e
    }

    override fun construct(cx: Context, scope: Scriptable, args: Array<Any>): Scriptable {
        throw ScriptRuntime.throwError(
            cx, scope,
            "require() can not be invoked as a constructor"
        )
    }

    @Synchronized
    private fun getExportedModuleInterface(
        cx: Context, id: String, moduleScript: ModuleScript, isMain: Boolean
    ): Any? {
        // Check if the requested module is already completely loaded
        val exports = moduleCache[id] ?: loadingModules[id]
        if (exports != null) {
//            println("导出缓存：$id")
            return if (isMain) {
                throw IllegalStateException("Attempt to set main module after it was loaded")
            } else exports
        }
        if (sandboxed && !moduleScript.isSandboxed) {
            throw ScriptRuntime.throwError(
                cx, nativeScope, ("Module \"$id\" is not contained in sandbox.")
            )
        }
        val pExports = cx.newObject(nativeScope)
        loadingModules[id] = pExports
        try {
            val newExports: Any? = executeModuleScript(
                cx, id, pExports, moduleScript, isMain
            )
            moduleCache[id] = newExports
            return newExports
        } catch (e: RuntimeException) {
            throw e
        } finally {
            loadingModules.remove(id)
        }
    }

    private fun executeModuleScript(
        cx: Context, id: String,
        exports: Scriptable?, moduleScript: ModuleScript, isMain: Boolean
    ): Any? {
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
        return getProperty(moduleObject, "exports")
    }

    @Synchronized
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
//        private const val serialVersionUID = 1L

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