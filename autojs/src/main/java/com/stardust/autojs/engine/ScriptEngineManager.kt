package com.stardust.autojs.engine

import android.content.Context
import com.stardust.autojs.engine.ScriptEngine.OnDestroyListener
import com.stardust.autojs.engine.ScriptEngineFactory.EngineNotFoundException
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.script.ScriptSource
import com.stardust.util.Supplier

/**
 * Created by Stardust on 2017/1/27.
 */
class ScriptEngineManager(val androidContext: Context) {
    interface EngineLifecycleCallback {
        fun onEngineCreate(engine: ScriptEngine<*>?)
        fun onEngineRemove(engine: ScriptEngine<*>?)
    }

    private val mEngines: MutableSet<ScriptEngine<*>> = HashSet()
    private var mEngineLifecycleCallback: EngineLifecycleCallback? = null
    private val mEngineSuppliers: MutableMap<String, Supplier<ScriptEngine<*>>> = HashMap()
    private val mGlobalVariableMap: MutableMap<String, Any> = HashMap()
    private val mOnEngineDestroyListener = OnDestroyListener { engine -> removeEngine(engine) }
    private fun addEngine(engine: ScriptEngine<*>) {
        engine.setOnDestroyListener(mOnEngineDestroyListener)
        synchronized(mEngines) {
            mEngines.add(engine)
            if (mEngineLifecycleCallback != null) {
                mEngineLifecycleCallback!!.onEngineCreate(engine)
            }
        }
    }

    fun setEngineLifecycleCallback(engineLifecycleCallback: EngineLifecycleCallback?) {
        mEngineLifecycleCallback = engineLifecycleCallback
    }

    val engines: Set<ScriptEngine<*>>
        get() = mEngines

    fun removeEngine(engine: ScriptEngine<*>) {
        synchronized(mEngines) {
            if (mEngines.remove(engine) && mEngineLifecycleCallback != null) {
                mEngineLifecycleCallback!!.onEngineRemove(engine)
            }
        }
    }

    fun stopAll(): Int {
        synchronized(mEngines) {
            val n = mEngines.size
            for (engine in mEngines) {
                engine.forceStop()
            }
            return n
        }
    }

    fun putGlobal(varName: String, value: Any) {
        mGlobalVariableMap[varName] = value
    }

     private fun putProperties(engine: ScriptEngine<*>) {
        for ((key, value) in mGlobalVariableMap) {
            engine.put(key, value)
        }
    }

    fun createEngine(name: String, id: Int): ScriptEngine<*>? {
        val s = mEngineSuppliers[name] ?: return null
        val engine = s.get()
        engine.setId(id)
        putProperties(engine)
        addEngine(engine)
        return engine
    }

    fun createEngineOfSource(source: ScriptSource, id: Int): ScriptEngine<*>? {
        return createEngine(source.engineName, id)
    }

    @JvmOverloads
    fun createEngineOfSourceOrThrow(
        source: ScriptSource,
        id: Int = ScriptExecution.NO_ID
    ): ScriptEngine<*> {
        return createEngineOfSource(source, id) ?: throw EngineNotFoundException("source: $source")
    }

    fun registerEngine(name: String, supplier: Supplier<ScriptEngine<*>>) {
        mEngineSuppliers[name] = supplier
    }

    fun unregisterEngine(name: String) {
        mEngineSuppliers.remove(name)
    }

    companion object {
        private const val TAG = "ScriptEngineManager"
    }
}
