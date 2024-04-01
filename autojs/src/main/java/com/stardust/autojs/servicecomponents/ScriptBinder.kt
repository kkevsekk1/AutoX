package com.stardust.autojs.servicecomponents

import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import com.stardust.autojs.AutoJs
import com.stardust.autojs.IndependentScriptService
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.script.JavaScriptFileSource
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
                Action.RUN_SCRIPT.id -> runScript(data, reply!!)
            }
            Log.d(TAG, "action id = $code, end")
            return@runBlocking true
        }

    private fun getAllScriptTasks(data: Parcel, reply: Parcel) {
        val scriptExecutions = AutoJs.instance.scriptEngineService.scriptExecutions
        val bundle = Bundle().apply { putInt("size", scriptExecutions.size) }
        for ((i: Int, scriptExecution) in scriptExecutions.withIndex()) {
            bundle.putBundle(
                i.toString(),
                TaskInfo.BundleTaskInfo.fromException(scriptExecution).bundle
            )
        }
        reply.writeNoException()
        reply.writeBundle(bundle)
    }

    private fun runScript(data: Parcel, reply: Parcel) {
        val bundle = data.readBundle(ClassLoader.getSystemClassLoader())
        val taskInfo = TaskInfo.BundleTaskInfo.formBundle(bundle!!)
        AutoJs.instance.scriptEngineService.execute(
            JavaScriptFileSource(taskInfo.sourcePath),
            ExecutionConfig(workingDirectory = taskInfo.workerDirectory)
        )
    }

    enum class Action(val id: Int) {
        START(1),
        STOP(2),
        GET_ALL_TASKS(3),
        RUN_SCRIPT(4),
        STOP_SCRIPT(5)
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