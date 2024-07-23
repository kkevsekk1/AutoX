package com.stardust.autojs.servicecomponents

import com.aiselp.autox.engine.NodeScriptEngine
import com.stardust.autojs.AutoJs
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.project.ProjectConfig
import com.stardust.autojs.script.JavaScriptSource
import com.stardust.autojs.script.ScriptFile
import com.stardust.autojs.script.ScriptSource
import com.stardust.autojs.servicecomponents.ScriptServiceConnection.Companion.GlobalConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

object EngineController {
    val scope = CoroutineScope(Dispatchers.Default)
    private val serviceConnection: ScriptServiceConnection
        get() = GlobalConnection

    private val globalScriptListener: CopyOnWriteArraySet<BinderScriptListener> by lazy {
        val listeners = CopyOnWriteArraySet<BinderScriptListener>()
        scope.launch {
            serviceConnection.registerGlobalScriptListener(
                object : BinderScriptListener {
                    override fun onStart(taskInfo: TaskInfo) {
                        for (l in listeners) {
                            l.onStart(taskInfo)
                        }
                    }

                    override fun onSuccess(taskInfo: TaskInfo) {
                        for (l in listeners) {
                            l.onSuccess(taskInfo)
                        }
                    }

                    override fun onException(taskInfo: TaskInfo, e: Throwable) {
                        for (l in listeners) {
                            l.onException(taskInfo, e)
                        }
                    }

                })
        }
        return@lazy listeners
    }

    fun runScript(taskInfo: TaskInfo, listener: BinderScriptListener? = null) = scope.launch {
        try {
            AutoJs.instance
            val source: ScriptSource = ScriptFile(taskInfo.sourcePath).toSource()
            AutoJs.instance.scriptEngineService.execute(
                source, listener?.toScriptExecutionListener(),
                ExecutionConfig(workingDirectory = taskInfo.workerDirectory)
            )
        } catch (e: Throwable) {
            serviceConnection.runScript(taskInfo, listener)
        }
    }

    fun launchProject(projectConfig: ProjectConfig, listener: BinderScriptListener? = null) =
        scope.launch {
            runScript(
                File(projectConfig.projectDirectory, projectConfig.mainScript ?: "main.js"),
                listener
            )
        }


    fun runScript(file: File, listener: BinderScriptListener? = null) {
        scope.launch {
            val engineName = when (file.extension) {
                "mjs" -> NodeScriptEngine.ID
                else -> JavaScriptSource.ENGINE
            }
            runScript(object : TaskInfo {
                override val id: Int = 0
                override val name: String = file.name
                override val desc: String = file.path
                override val engineName: String = engineName
                override val workerDirectory: String = file.parent ?: "/"
                override val sourcePath: String = file.path
                override val isRunning: Boolean = false
            }, listener)
        }
    }

    fun getAllScriptTasks(): Deferred<MutableList<TaskInfo>> = scope.async {
        return@async serviceConnection.getAllScriptTasks()
    }

    fun stopScript(id: Int) = scope.launch {
        serviceConnection.stopScript(id)
    }

    fun stopAllScript() = scope.launch {
        serviceConnection.stopAllScript()
    }

    fun registerGlobalScriptExecutionListener(listener: BinderScriptListener) =
        globalScriptListener.add(listener)

    fun unregisterGlobalScriptExecutionListener(listener: BinderScriptListener) =
        globalScriptListener.remove(listener)

    private val TAG = "EngineController"
}