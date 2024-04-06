package com.stardust.autojs.servicecomponents

import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import com.aiselp.autox.engine.NodeScriptEngine
import com.aiselp.autox.engine.NodeScriptSource
import com.stardust.autojs.AutoJs
import com.stardust.autojs.IndependentScriptService
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.script.JavaScriptFileSource
import com.stardust.autojs.script.ScriptSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

class ScriptBinder(service: IndependentScriptService, val scope: CoroutineScope) : Binder() {
    val wService = WeakReference(service)
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        runBlocking {
            data.enforceInterface(DESCRIPTOR)
            Log.d(TAG, "action id = $code")
            when (code) {
                Action.GET_ALL_TASKS.id -> getAllScriptTasks(data, reply!!)
                Action.RUN_SCRIPT.id -> runScript(data)
                Action.STOP_SCRIPT.id -> stopScript(data)
                Action.STOP_ALL_SCRIPT.id -> stopAllScript()
                Action.REGISTER_GLOBAL_SCRIPT_LISTENER.id -> registerGlobalScriptListener(data)
            }
            Log.d(TAG, "action id = $code, complete")
            return@runBlocking true
        }

    private fun getAllScriptTasks(data: Parcel, reply: Parcel) {
        val scriptExecutions = AutoJs.instance.scriptEngineService.scriptExecutions
        val bundle = Bundle().apply { putInt("size", scriptExecutions.size) }
        for ((i: Int, scriptExecution) in scriptExecutions.withIndex()) {
            bundle.putBundle(
                i.toString(),
                TaskInfo.ExecutionTaskInfo(scriptExecution).toBundle()
            )
        }
        reply.writeNoException()
        reply.writeBundle(bundle)
    }

    private fun runScript(data: Parcel) {
        val bundle = data.readBundle(ClassLoader.getSystemClassLoader())
        check(bundle != null) { "bundle is null" }
        val taskInfo = bundle.getBundle(TaskInfo.TAG)!!.let {
            TaskInfo.fromBundle(it)
        }
        val listener = bundle.getBinder(BinderScriptListener.TAG)?.let {
            BinderScriptListener.ServerInterface(it)
        }
        Log.d(TAG,"engineName = ${taskInfo.engineName}")
        val source: ScriptSource = when (taskInfo.engineName) {
            NodeScriptEngine.ID -> NodeScriptSource(taskInfo.sourcePath)
            else -> JavaScriptFileSource(taskInfo.sourcePath)
        }
        AutoJs.instance.scriptEngineService.execute(
            source, listener,
            ExecutionConfig(workingDirectory = taskInfo.workerDirectory)
        )
    }

    private fun stopScript(data: Parcel) {
        val id = data.readInt()
        check(id >= 0) { "invalid id" }
        val scriptExecutions = AutoJs.instance.scriptEngineService.scriptExecutions
        for (scriptExecution in scriptExecutions) {
            if (scriptExecution.id == id) {
                scriptExecution.engine.forceStop()
                break
            }
        }
    }

    private fun stopAllScript() = AutoJs.instance.scriptEngineService.stopAll()

    private fun registerGlobalScriptListener(data: Parcel) {
        val binder = data.readStrongBinder()
        val listener = BinderScriptListener.ServerInterface(binder)
        AutoJs.instance.scriptEngineService.registerGlobalScriptExecutionListener(listener)
    }

    enum class Action(val id: Int) {
        START(1),
        STOP(2),
        GET_ALL_TASKS(3),
        RUN_SCRIPT(4),
        STOP_SCRIPT(5),
        STOP_ALL_SCRIPT(6),
        REGISTER_GLOBAL_SCRIPT_LISTENER(7)
    }

    companion object {
        const val TAG = "ScriptBinder"
        const val DESCRIPTOR = "com.stardust.autojs.servicecomponents.ScriptBinder"
        suspend fun <T> connect(binder: IBinder, n: suspend TanBinder.() -> T): T {
            val d = Parcel.obtain().apply { writeInterfaceToken(DESCRIPTOR) }
            val r = Parcel.obtain()
            try {
                return TanBinder(binder, data = d, reply = r).n()
            } finally {
                d.recycle()
                r.recycle()
            }
        }
    }
}