package com.aiselp.autojs.codeeditor.plugins

import android.app.Activity
import com.aiselp.autojs.codeeditor.web.PluginManager
import com.aiselp.autojs.codeeditor.web.annotation.WebFunction
import com.stardust.autojs.AutoJs
import com.stardust.autojs.servicecomponents.BinderScriptListener
import com.stardust.autojs.servicecomponents.EngineController
import com.stardust.autojs.servicecomponents.TaskInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AppController(val activity: Activity) {
    companion object {
        const val TAG = "AppController"
    }

    private val autojs: AutoJs?
        get() = AutoJs.instance


    @WebFunction
    fun exit(call: PluginManager.WebCall) {
        call.onSuccess(null)
        activity.finish()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @WebFunction
    fun back(call: PluginManager.WebCall) {
        call.onSuccess(null)
        GlobalScope.launch(Dispatchers.Main) {
            activity.moveTaskToBack(false)
        }
    }

    @WebFunction
    fun runScript(call: PluginManager.WebCall) {
        val path = FileSystem.parsePath(call.data!!)
        EngineController.runScript(path, listener = object : BinderScriptListener {
            override fun onStart(taskInfo: TaskInfo) {}

            override fun onSuccess(taskInfo: TaskInfo) {
                call.onSuccess(null)
            }

            override fun onException(taskInfo: TaskInfo, e: Throwable) {
                call.onError(Exception(e))
            }

        })
    }
}