package com.aiselp.autox.engine

import android.content.Context
import android.util.Log
import com.aiselp.autox.api.JavaInteractor
import com.aiselp.autox.api.NodeConsole
import com.aiselp.autox.module.NodeModuleResolver
import com.caoccao.javet.enums.V8AwaitMode
import com.caoccao.javet.exceptions.JavetExecutionException
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.converters.JavetProxyConverter
import com.caoccao.javet.node.modules.NodeModuleModule
import com.caoccao.javet.node.modules.NodeModuleProcess
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.V8ValuePromise
import com.stardust.autojs.AutoJs
import com.stardust.autojs.engine.ScriptEngine
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.runtime.exception.ScriptException
import com.stardust.autojs.script.ScriptSource
import com.stardust.util.UiHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import java.io.File

class NodeScriptEngine(context: Context, val uiHandler: UiHandler) :
    ScriptEngine.AbstractScriptEngine<ScriptSource>() {
    private val runtime: NodeRuntime = V8Host.getNodeInstance().createV8Runtime()

    private val tags = mutableMapOf<String, Any?>()
    private val config: ExecutionConfig by lazy {
        tags[ExecutionConfig.tag] as ExecutionConfig
    }
    private val moduleDirectory = File(context.filesDir, "node_module")
    private val resultListener = PromiseListener()
    private val console = NodeConsole(AutoJs.instance.globalConsole)
    private val v8ApiManager = V8ApiManager()
    private val converter = JavetProxyConverter()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        Log.i(TAG, "node version: ${runtime.version}")
    }

    override fun put(name: String, value: Any?) {
        tags[name] = value
    }

    override fun forceStop() {
        Log.i(TAG, "force stop")
        resultListener.cancel()
        if (runtime.isInUse) {
            runtime.terminateExecution()
        }
        runtime.getNodeModule(NodeModuleProcess::class.java)
            .moduleObject.invokeVoid("exit", runtime.createV8ValueInteger(1))
    }

    override fun init() {
        runtime.converter = converter
        runtime.isPurgeEventLoopBeforeClose = true
        initializeApi()
    }

    private fun initializeApi() = runtime.globalObject.use { global ->
        v8ApiManager.register(console)
        v8ApiManager.register(JavaInteractor(scope, converter))
        v8ApiManager.initialize(runtime, global)
    }

    override fun execute(scriptSource: ScriptSource): Any? = runBlocking {
        check(scriptSource is NodeScriptSource) { "scriptSource must be NodeScriptSource" }
        Log.i(TAG, "execute: ${scriptSource.file.path}")
        val scriptFile = scriptSource.file
        try {
            initializeModule(scriptFile).use {
                if (it is V8ValuePromise)
                    it.register(resultListener)
                else resultListener.onFulfilled(it)
                while (scope.isActive and runtime.await(V8AwaitMode.RunOnce)) {
                    Thread.sleep(1)
                }
            }
            return@runBlocking resultListener.await().let {
                if (resultListener.stack != null) console.error(resultListener.stack)
                if (resultListener.isRejectedCalled) throw ScriptException(it)
            }
        } catch (e: JavetExecutionException) {
            throw e.apply { console.error(scriptingError.stack) }
        } catch (e: Throwable) {
            throw e.apply { console.error(toString()) }
        }
    }

    private fun initializeModule(file: File): V8Value {
        val parentFile = file.parentFile ?: File("/")
        runtime.getNodeModule(NodeModuleProcess::class.java).workingDirectory = parentFile
        runtime.getNodeModule(NodeModuleModule::class.java).setRequireRootDirectory(parentFile)
        val nodeModuleResolver = NodeModuleResolver(parentFile)
        runtime.v8ModuleResolver = nodeModuleResolver
        return if (NodeModuleResolver.isEsModule(file)) {
            //es module
            runtime.getExecutor(file).setResourceName(file.path).compileV8Module(true).run {
                nodeModuleResolver.addCacheModule(this)
                execute()
            }
        } else {
            //commonjs
            runtime.globalObject.invoke(
                NodeModuleModule.PROPERTY_REQUIRE, runtime.createV8ValueString(file.path)
            )
        }
    }

    override fun destroy() {
        v8ApiManager.recycle(runtime, runtime.globalObject)
        scope.cancel()
        if (runtime.isClosed) return
        runtime.lowMemoryNotification()
        runtime.close()
        super.destroy()
    }

    companion object {
        const val ID = "com.aiselp.autox.engine.NodeScriptEngine"
        private const val TAG = "NodeScriptEngine"
    }
}