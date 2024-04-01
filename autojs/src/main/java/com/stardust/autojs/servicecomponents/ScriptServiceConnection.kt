package com.stardust.autojs.servicecomponents

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.Job

class ScriptServiceConnection : ServiceConnection {
    lateinit var service: IBinder
    private val connected = Job()

    @Volatile
    var isConnected = false
        private set

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        check(service != null) { "service is null" }
        this.service = service
        isConnected = true
        connected.complete()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        isConnected = false
    }

    private suspend fun <T> sendBinder(n: suspend TanBinder.() -> T): T {
        awaitConnected()
        return ScriptBinder.connect(service, n)
    }

    suspend fun getAllScriptTasks(): MutableList<TaskInfo.BundleTaskInfo> = sendBinder {
        action = ScriptBinder.Action.GET_ALL_TASKS.id
        send()
        reply.readException()
        val bundle = reply.readBundle(ClassLoader.getSystemClassLoader())
        check(bundle != null) { "bundle is null" }
        val size = bundle.getInt("size")
        val tasks = mutableListOf<TaskInfo.BundleTaskInfo>()
        for (i in 1..size) {
            tasks.add(TaskInfo.BundleTaskInfo.formBundle(bundle.getBundle((i - 1).toString())!!))
        }
        return@sendBinder tasks
    }

    suspend fun runScript(taskInfo: TaskInfo) = sendBinder {
        action = ScriptBinder.Action.RUN_SCRIPT.id
        data.writeBundle(TaskInfo.BundleTaskInfo.fromTaskInfo(taskInfo).bundle)
        send()
    }

    suspend fun awaitConnected() {
        connected.join()
        check(isConnected) { "service is not connected" }
    }

    companion object {
        val GlobalConnection by lazy { ScriptServiceConnection() }
    }
}