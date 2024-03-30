package com.stardust.autojs

import android.content.Context
import com.stardust.autojs.engine.JavaScriptEngine
import com.stardust.autojs.engine.ScriptEngine
import com.stardust.autojs.engine.ScriptEngineManager
import com.stardust.autojs.engine.ScriptEngineManager.EngineLifecycleCallback
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.execution.LoopedBasedJavaScriptExecution
import com.stardust.autojs.execution.RunnableScriptExecution
import com.stardust.autojs.execution.ScriptExecuteActivity
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.execution.ScriptExecutionListener
import com.stardust.autojs.execution.ScriptExecutionObserver
import com.stardust.autojs.execution.ScriptExecutionTask
import com.stardust.autojs.execution.SimpleScriptExecutionListener
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.runtime.api.Console
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import com.stardust.autojs.script.JavaScriptSource
import com.stardust.autojs.script.ScriptSource
import com.stardust.lang.ThreadCompat
import com.stardust.util.UiHandler
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Created by Stardust on 2017/1/23.
 */
class ScriptEngineService internal constructor(builder: ScriptEngineServiceBuilder) {
    private val mUiHandler: UiHandler = builder.mUiHandler
    private val mContext: Context = mUiHandler.context
    val globalConsole: Console = builder.mGlobalConsole
    private val mScriptEngineManager: ScriptEngineManager = builder.mScriptEngineManager
    private val mEngineLifecycleObserver: EngineLifecycleObserver =
        object : EngineLifecycleObserver() {
            override fun onEngineRemove(engine: ScriptEngine<*>?) {
                mScriptExecutions.remove(engine!!.id)
                super.onEngineRemove(engine)
            }
        }
    private val mScriptExecutionObserver = ScriptExecutionObserver()
    private val mScriptExecutions = LinkedHashMap<Int, ScriptExecution>()

    init {
        mScriptEngineManager.setEngineLifecycleCallback(mEngineLifecycleObserver)
        mScriptExecutionObserver.registerScriptExecutionListener(GLOBAL_LISTENER)
        EVENT_BUS.register(this)
        mScriptEngineManager.putGlobal("context", mUiHandler.context)
        ScriptRuntime.setApplicationContext(builder.mUiHandler.context.applicationContext)
    }

    fun registerEngineLifecycleCallback(engineLifecycleCallback: EngineLifecycleCallback) {
        mEngineLifecycleObserver.registerCallback(engineLifecycleCallback)
    }

    fun unregisterEngineLifecycleCallback(engineLifecycleCallback: EngineLifecycleCallback) {
        mEngineLifecycleObserver.unregisterCallback(engineLifecycleCallback)
    }

    fun registerGlobalScriptExecutionListener(listener: ScriptExecutionListener): Boolean {
        return mScriptExecutionObserver.registerScriptExecutionListener(listener)
    }

    fun unregisterGlobalScriptExecutionListener(listener: ScriptExecutionListener): Boolean {
        return mScriptExecutionObserver.removeScriptExecutionListener(listener)
    }

    fun execute(task: ScriptExecutionTask): ScriptExecution {
        val execution = executeInternal(task)
        mScriptExecutions[execution.id] = execution
        return execution
    }

    //脚本启动入口
    private fun executeInternal(task: ScriptExecutionTask): ScriptExecution {
        if (task.listener != null) {
            task.setExecutionListener(
                ScriptExecutionObserver.Wrapper(
                    mScriptExecutionObserver,
                    task.listener
                )
            )
        } else {
            task.setExecutionListener(mScriptExecutionObserver)
        }
        val source = task.source
        if (source is JavaScriptSource) {
            val mode = source.executionMode
            if (mode and JavaScriptSource.EXECUTION_MODE_UI != 0) {
                return ScriptExecuteActivity.Companion.execute(mContext, mScriptEngineManager, task)
            }
        }
        val r: RunnableScriptExecution = if (source is JavaScriptSource) {
            LoopedBasedJavaScriptExecution(mScriptEngineManager, task)
        } else {
            RunnableScriptExecution(mScriptEngineManager, task)
        }
        ThreadCompat(r).start()
        return r
    }

    fun execute(
        source: ScriptSource?,
        listener: ScriptExecutionListener?,
        config: ExecutionConfig?
    ): ScriptExecution {
        return execute(ScriptExecutionTask(source, listener, config))
    }

    fun execute(source: ScriptSource?, config: ExecutionConfig?): ScriptExecution {
        return execute(ScriptExecutionTask(source, null, config))
    }

    @Subscribe
    fun onScriptExecution(event: ScriptExecutionEvent) {
        if (event.code == ScriptExecutionEvent.ON_START) {
            globalConsole.verbose(mContext.getString(R.string.text_start_running) + "[" + event.message + "]")
        } else if (event.code == ScriptExecutionEvent.ON_EXCEPTION) {
            mUiHandler.toast(mContext.getString(R.string.text_error) + ": " + event.message)
        }
    }

    fun stopAll(): Int {
        return mScriptEngineManager.stopAll()
    }

    fun stopAllAndToast() {
        val n = stopAll()
        if (n > 0) mUiHandler.toast(
            String.format(
                mContext.getString(R.string.text_already_stop_n_scripts),
                n
            )
        )
    }

    val engines: Set<ScriptEngine<*>>
        get() = mScriptEngineManager.engines
    val scriptExecutions: Collection<ScriptExecution>
        get() = mScriptExecutions.values

    fun getScriptExecution(id: Int): ScriptExecution? {
        return if (id == ScriptExecution.NO_ID) {
            null
        } else mScriptExecutions[id]
    }

    private open class EngineLifecycleObserver : EngineLifecycleCallback {
        private val mEngineLifecycleCallbacks: MutableSet<EngineLifecycleCallback> = LinkedHashSet()
        override fun onEngineCreate(engine: ScriptEngine<*>?) {
            synchronized(mEngineLifecycleCallbacks) {
                for (callback in mEngineLifecycleCallbacks) {
                    callback.onEngineCreate(engine)
                }
            }
        }

        override fun onEngineRemove(engine: ScriptEngine<*>?) {
            synchronized(mEngineLifecycleCallbacks) {
                for (callback in mEngineLifecycleCallbacks) {
                    callback.onEngineRemove(engine)
                }
            }
        }

        fun registerCallback(callback: EngineLifecycleCallback) {
            synchronized(mEngineLifecycleCallbacks) { mEngineLifecycleCallbacks.add(callback) }
        }

        fun unregisterCallback(callback: EngineLifecycleCallback) {
            synchronized(mEngineLifecycleCallbacks) { mEngineLifecycleCallbacks.remove(callback) }
        }
    }

    class ScriptExecutionEvent internal constructor(val code: Int, val message: String) {

        companion object {
            const val ON_START = 1001
            const val ON_SUCCESS = 1002
            const val ON_EXCEPTION = 1003
        }
    }

    companion object {
        private const val LOG_TAG = "ScriptEngineService"
        private val EVENT_BUS = EventBus()
        private val GLOBAL_LISTENER: ScriptExecutionListener =
            object : SimpleScriptExecutionListener() {
                override fun onStart(execution: ScriptExecution) {
                    if (execution.engine is JavaScriptEngine) {
                        (execution.engine as JavaScriptEngine).runtime.console.setTitle(
                            execution.source.name,
                            "#ffffffff",
                            -1
                        )
                    }
                    EVENT_BUS.post(
                        ScriptExecutionEvent(
                            ScriptExecutionEvent.ON_START,
                            execution.source.toString()
                        )
                    )
                }

                override fun onSuccess(execution: ScriptExecution?, result: Any?) {
                    onFinish(execution)
                }

                private fun onFinish(execution: ScriptExecution?) {}
                override fun onException(execution: ScriptExecution, e: Throwable) {
                    e.printStackTrace()
                    onFinish(execution)
                    var message: String? = null
                    val engine = execution.engine
                    if (!ScriptInterruptedException.causedByInterrupted(e)) {
                        message = e.message
                        if (engine is JavaScriptEngine) {
                            engine.runtime.console.error(e)
                        }
                    }
                    if (engine is JavaScriptEngine) {
                        val uncaughtException = engine.uncaughtException
                        if (uncaughtException != null) {
                            engine.runtime.console.error(uncaughtException)
                            message = uncaughtException.message
                        }
                    }
                    if (message != null) {
                        EVENT_BUS.post(
                            ScriptExecutionEvent(
                                ScriptExecutionEvent.ON_EXCEPTION,
                                message
                            )
                        )
                    }
                }
            }
        private var sInstance: ScriptEngineService? = null

        @JvmStatic
        var instance: ScriptEngineService?
            get() = sInstance
            set(service) {
                check(sInstance == null)
                sInstance = service
            }
    }
}