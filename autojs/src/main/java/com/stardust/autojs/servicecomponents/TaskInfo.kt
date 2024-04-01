package com.stardust.autojs.servicecomponents

import android.os.Bundle
import com.stardust.autojs.execution.ScriptExecution
import java.io.Serializable

interface TaskInfo : Serializable {
    val name: String
    val desc: String
    val engineName: String
    val workerDirectory: String
    val sourcePath: String
    val isRunning: Boolean

    class BundleTaskInfo private constructor(
        val bundle: Bundle = Bundle()
    ) : TaskInfo {
        override val name: String
            get() = bundle.getString("name")!!
        override val desc: String
            get() = bundle.getString("desc")!!
        override val engineName: String
            get() = bundle.getString("engineName")!!
        override val workerDirectory: String
            get() = bundle.getString("workerDirectory")!!
        override val sourcePath: String
            get() = bundle.getString("sourcePath")!!
        override val isRunning: Boolean
            get() = bundle.getBoolean("isRunning")

        companion object {
            fun fromException(exception: ScriptExecution): BundleTaskInfo {
                val source = exception.source
                val bundleTaskInfo = BundleTaskInfo()
                bundleTaskInfo.bundle.putString("name", source.name)
                bundleTaskInfo.bundle.putString("desc", source.toString())
                bundleTaskInfo.bundle.putString("engineName", source.engineName)
                bundleTaskInfo.bundle.putString("sourcePath", source.toString())
                bundleTaskInfo.bundle.putString(
                    "workerDirectory",
                    exception.config.workingDirectory
                )
                return bundleTaskInfo
            }

            fun fromTaskInfo(taskInfo: TaskInfo): BundleTaskInfo {
                val binder = Bundle().apply {
                    putString("name", taskInfo.name)
                    putString("desc", taskInfo.desc)
                    putString("engineName", taskInfo.engineName)
                    putString("sourcePath", taskInfo.sourcePath)
                    putString("workerDirectory", taskInfo.workerDirectory)
                    putBoolean("isRunning", taskInfo.isRunning)
                }
                return formBundle(binder)
            }

            fun formBundle(bundle: Bundle): BundleTaskInfo {
                return BundleTaskInfo(bundle)
            }
        }
    }
}