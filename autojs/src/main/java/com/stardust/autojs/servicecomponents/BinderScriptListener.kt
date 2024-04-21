package com.stardust.autojs.servicecomponents

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import com.stardust.autojs.ScriptEngineService.ScriptExecutionEvent
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.execution.ScriptExecutionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface BinderScriptListener {
    fun onStart(taskInfo: TaskInfo)
    fun onSuccess(taskInfo: TaskInfo)
    fun onException(taskInfo: TaskInfo, e: Throwable)

    fun toBinder(scope: CoroutineScope = EngineController.scope): IBinder {
        return ClientInterface(this, scope)
    }

    class ServerInterface(val binder: IBinder) : BinderScriptListener, ScriptExecutionListener {

        fun connect(code: Int, taskInfo: TaskInfo, binderAction: TanBinder.() -> Unit) {
            val data = Parcel.obtain()
            try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeBundle(taskInfo.toBundle())
                TanBinder(binder, code, data, null, 1).binderAction()
            } finally {
                data.recycle()
            }
        }

        override fun onStart(taskInfo: TaskInfo) =
            connect(ScriptExecutionEvent.ON_START, taskInfo) {
                send()
            }

        override fun onSuccess(taskInfo: TaskInfo) =
            connect(ScriptExecutionEvent.ON_SUCCESS, taskInfo) {
                send()
            }

        override fun onException(taskInfo: TaskInfo, e: Throwable) =
            connect(ScriptExecutionEvent.ON_EXCEPTION, taskInfo) {
                data.writeException(IllegalStateException(e))
                send()
            }

        override fun onStart(execution: ScriptExecution) {
            onStart(TaskInfo.ExecutionTaskInfo(execution))
        }

        override fun onSuccess(execution: ScriptExecution, result: Any?) {
            onSuccess(TaskInfo.ExecutionTaskInfo(execution))
        }

        override fun onException(execution: ScriptExecution, e: Throwable) {
            onException(TaskInfo.ExecutionTaskInfo(execution), e)
        }

    }

    class ClientInterface(val listener: BinderScriptListener, val scope: CoroutineScope) :
        Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            data.enforceInterface(DESCRIPTOR)
            val bundle = data.readBundle(ClassLoader.getSystemClassLoader())
            val taskInfo = TaskInfo.fromBundle(bundle!!)
            when (code) {
                ScriptExecutionEvent.ON_START -> {
                    scope.launch { listener.onStart(taskInfo) }
                }

                ScriptExecutionEvent.ON_SUCCESS -> {
                    scope.launch { listener.onSuccess(taskInfo) }
                }

                ScriptExecutionEvent.ON_EXCEPTION -> {
                    val ex = try {
                        data.readException()
                        Exception("unknown exception")
                    } catch (e: Throwable) {
                        e
                    }
                    scope.launch {
                        listener.onException(taskInfo, ex)
                    }
                }
            }
            return super.onTransact(code, data, reply, flags)
        }
    }

    companion object {
        private const val DESCRIPTOR =
            "com.stardust.autojs.servicecomponents.BinderScriptExecutionListener"
        const val TAG = "BinderScriptExecutionListener"
    }
}