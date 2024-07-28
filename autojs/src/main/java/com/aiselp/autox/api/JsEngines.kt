package com.aiselp.autox.api

import com.aiselp.autox.engine.NodeScriptEngine
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueFunction
import com.caoccao.javet.values.reference.V8ValueObject
import com.stardust.autojs.ScriptEngineService
import com.stardust.autojs.engine.ScriptEngine
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.execution.ScriptExecutionListener
import com.stardust.autojs.script.ScriptFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JsEngines(private val engine: NodeScriptEngine) : NativeApi {
    override val moduleId: String = ID
    private val engineService by lazy { ScriptEngineService.instance!! }
    private var emitCallback: V8ValueFunction? = null

    override fun install(v8Runtime: V8Runtime, global: V8ValueObject): NativeApi.BindingMode {
        return NativeApi.BindingMode.PROXY
    }

    override fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
    }

    @V8Function
    fun setupJs(ops: V8ValueObject) {
        emitCallback = ops.get(EMIT_FUNCTION)
    }

    @V8Function
    fun execScriptFile(
        path: String,
        config: ExecutionConfig?,
        listener: V8ValueFunction?
    ): ScriptExecution {
        if (listener != null) {
            val callback = engine.eventLoopQueue.createV8Callback(listener)
            return engineService.execute(ScriptFile(path).toSource(), object :
                ScriptExecutionListener {
                override fun onStart(execution: ScriptExecution?) {
                    callback.invoke(0, execution)
                }

                override fun onSuccess(execution: ScriptExecution?, result: Any?) {
                    callback.invoke(1, execution, result)
                    callback.close()
                }

                override fun onException(execution: ScriptExecution?, e: Throwable?) {
                    callback.invoke(2, execution, e)
                    callback.close()
                }

            }, config)
        } else
            return engineService.execute(ScriptFile(path).toSource(), config)
    }

    fun emitEngineEvent(name: String, args: Array<out Any?>) {
        emitCallback?.callVoid(null, name, *args)
    }

    @V8Function
    fun allEngine(): Set<ScriptEngine<*>> {
        return engineService.engines
    }

    @V8Function
    fun myEngine() = engine

    @V8Function
    fun createExecutionConfig() = ExecutionConfig()

    @V8Function
    fun stopAll() {
        engine.scope.launch(Dispatchers.Default) {
            engineService.stopAll()
        }
    }

    @V8Function
    fun stopAllAndToast() {
        engineService.stopAllAndToast()
    }

    companion object {
        const val ID = "engines"
        private const val EMIT_FUNCTION = "emitCallback"
    }
}