package com.aiselp.autojs.codeeditor.plugins

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.aiselp.autojs.codeeditor.web.PluginManager
import com.aiselp.autojs.codeeditor.web.annotation.WebFunction
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.AutoJs
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.execution.ScriptExecutionListener
import com.stardust.autojs.execution.ScriptExecutionTask
import com.stardust.autojs.script.ScriptSource
import com.stardust.autojs.script.StringScriptSource
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)

class AppController(val activity: Activity) {
    companion object {
        const val TAG = "AppController"
    }

    private val autojs: AutoJs? = try {
        val cz = Class.forName("org.autojs.autojs.autojs.AutoJs")
        val obj = cz.getMethod("getInstance").invoke(null)
        obj as? AutoJs
    } catch (e: Exception) {
        null
    }

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
        val file: ScriptSource = StringScriptSource(path.name, path.readText())
        try {
            autojs?.let {
                it.scriptEngineService.execute(
                    ScriptExecutionTask(file, object :
                        ScriptExecutionListener {
                        override fun onStart(execution: ScriptExecution?) {}

                        override fun onSuccess(execution: ScriptExecution?, result: Any?) {
                            call.onSuccess(null)
                        }

                        override fun onException(execution: ScriptExecution?, e: Throwable?) {
                            call.onError(Exception(e))
                        }

                    }, ExecutionConfig(workingDirectory = path.parent ?: "/"))
                )
            } ?: call.onError(Exception("没有运行接口"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(GlobalAppContext.get(), e.message, Toast.LENGTH_LONG).show()
            call.onError(e)
        }
    }
}