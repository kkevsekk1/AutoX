package com.aiselp.autox.engine

import android.content.Context
import android.util.Log
import com.aiselp.autox.api.NodeConsole
import com.caoccao.javet.enums.JSRuntimeType
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.engine.JavetEnginePool
import com.caoccao.javet.node.modules.NodeModuleModule
import com.caoccao.javet.values.reference.V8ValuePromise
import com.stardust.autojs.AutoJs
import com.stardust.autojs.engine.ScriptEngine
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.runtime.exception.ScriptException
import com.stardust.autojs.script.ScriptSource
import com.stardust.util.UiHandler
import kotlinx.coroutines.runBlocking
import java.io.File

class NodeScriptEngine(context: Context, val uiHandler: UiHandler) :
    ScriptEngine.AbstractScriptEngine<ScriptSource>() {
    private val javetEngine = javetEnginePool.engine
    private val runtime = javetEngine.v8Runtime

    private val tags = mutableMapOf<String, Any?>()
    private val config: ExecutionConfig by lazy {
        tags[ExecutionConfig.tag] as ExecutionConfig
    }
    private val moduleDirectory = File(context.filesDir, "node_module")
    private val resultListener = PromiseListener()
    private val console = NodeConsole(AutoJs.instance.globalConsole)

    init {
        Log.i(TAG, "node version: ${runtime.version}")
    }

    override fun put(name: String, value: Any?) {
        tags[name] = value
    }

    override fun forceStop() {
        resultListener.cancel()
        javetEngine.close()
    }

    override fun init() {
        initializeApi()
    }

    private fun initializeApi() = runtime.globalObject.use { global ->
        runtime.createV8ValueObject().use {
            it.bind(console)
            global.set("console", it)
        }
    }

    override fun execute(scriptSource: ScriptSource): Any? = runBlocking {
        check(scriptSource is NodeScriptSource) { "scriptSource must be NodeScriptSource" }
        Log.i(TAG, "execute: ${scriptSource.file.path}")
        val scriptFile = scriptSource.file
        runtime.getNodeModule(NodeModuleModule::class.java)
            .setRequireRootDirectory(scriptFile.parent)
        val executor =
            runtime.getExecutor(scriptFile).setModule(true).setResourceName(scriptFile.path)
        executor.execute<V8ValuePromise>().use {
            it.register(resultListener)
            runtime.await()
        }
        return@runBlocking try {
            resultListener.await()
        } catch (e: Throwable) {
            throw ScriptException(e).apply {
                AutoJs.instance.globalConsole.error(this.toString())
            }
        }
    }

    override fun destroy() {
        super.destroy()
        runtime.lowMemoryNotification()
        javetEngine.close()
    }

    companion object {
        private val javetEnginePool by lazy {
            JavetEnginePool<NodeRuntime>().apply {
                config.setJSRuntimeType(JSRuntimeType.Node)
            }
        }
        const val ID = "com.aiselp.autox.engine.NodeScriptEngine"
        private const val TAG = "NodeScriptEngine"
    }
}