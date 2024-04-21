package com.stardust.autojs.servicecomponents

import android.os.Bundle
import com.stardust.autojs.execution.ScriptExecution
import java.io.Serializable

interface TaskInfo : Serializable {
    val id: Int
    val name: String
    val desc: String
    val engineName: String
    val workerDirectory: String
    val sourcePath: String
    val isRunning: Boolean

    fun toBundle(bundle: Bundle = Bundle()): Bundle {
        return bundle.apply {
            putInt("id", id)
            putString("name", name)
            putString("desc", desc)
            putString("engineName", engineName)
            putString("sourcePath", sourcePath)
            putString("workerDirectory", workerDirectory)
            putBoolean("isRunning", isRunning)
        }
    }

    companion object {
        const val TAG = "TaskInfo"
        fun fromBundle(bundle: Bundle): TaskInfo {
            return object : TaskInfo {
                override val id: Int = bundle.getInt("id")
                override val name: String = bundle.getString("name")!!
                override val desc: String = bundle.getString("desc")!!
                override val engineName: String = bundle.getString("engineName")!!
                override val workerDirectory: String = bundle.getString("workerDirectory")!!
                override val sourcePath: String = bundle.getString("sourcePath")!!
                override val isRunning: Boolean = bundle.getBoolean("isRunning")
            }
        }
    }

    class ExecutionTaskInfo(
        exception: ScriptExecution
    ) : TaskInfo {
        override val id: Int = exception.id
        override val name: String
        override val desc: String
        override val engineName: String
        override val workerDirectory: String
        override val sourcePath: String
        override val isRunning: Boolean = true


        init {
            val source = exception.source
            name = source.name
            desc = source.toString()
            engineName = source.engineName
            workerDirectory = exception.config.workingDirectory
            sourcePath = source.toString()
        }
    }
}