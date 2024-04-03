package org.autojs.autojs.ui.main.task

import com.stardust.app.GlobalAppContext.getString
import com.stardust.autojs.script.AutoFileSource
import com.stardust.autojs.script.JavaScriptSource
import com.stardust.autojs.servicecomponents.TaskInfo
import com.stardust.pio.PFiles.getSimplifiedPath
import org.autojs.autojs.timing.IntentTask
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTaskManager.removeTask
import org.autojs.autojs.ui.timing.TimedTaskSettingActivity
import org.autojs.autoxjs.R
import org.joda.time.format.DateTimeFormat
import java.io.File

/**
 * Created by Stardust on 2017/11/28.
 */
interface Task : TaskInfo {
    fun cancel()
    class PendingTask : Task {
        var timedTask: TimedTask?
        private var mIntentTask: IntentTask?

        constructor(timedTask: TimedTask?) {
            this.timedTask = timedTask
            mIntentTask = null
        }

        constructor(intentTask: IntentTask?) {
            mIntentTask = intentTask
            timedTask = null
        }

        fun taskEquals(task: Any): Boolean {
            return if (timedTask != null) {
                timedTask == task
            } else mIntentTask == task
        }

        override val name: String = getSimplifiedPath(scriptPath!!)

        override val desc: String
            get() {
                return if (timedTask != null) {
                    val nextTime = timedTask!!.nextTime
                    getString(R.string.text_next_run_time) + ": " + DateTimeFormat.forPattern("yyyy/MM/dd HH:mm")
                        .print(nextTime)
                } else {
                    assert(mIntentTask != null)
                    val desc = TimedTaskSettingActivity.ACTION_DESC_MAP[mIntentTask!!.action]
                    if (desc != null) {
                        getString(desc)
                    } else mIntentTask!!.action!!
                }
            }

        override fun cancel() {
            if (timedTask != null) {
                removeTask(timedTask!!)
            } else {
                removeTask(mIntentTask!!)
            }
        }

        private val scriptPath: String?
            get() = if (timedTask != null) {
                timedTask!!.scriptPath
            } else {
                assert(mIntentTask != null)
                mIntentTask!!.scriptPath
            }

        override val engineName: String
            get() {
                return if (scriptPath!!.endsWith(".js")) {
                    JavaScriptSource.ENGINE
                } else {
                    AutoFileSource.ENGINE
                }
            }

        fun setIntentTask(intentTask: IntentTask?) {
            mIntentTask = intentTask
        }

        override val id: Int
            get() {
                return if (timedTask != null) timedTask!!.id.toInt() else mIntentTask!!.id.toInt()
            }

        override val workerDirectory: String = File(scriptPath).getParent()

        override val sourcePath: String = scriptPath!!

        override val isRunning: Boolean = false
    }
}
