package org.autojs.autojs.ui.main.task

import android.content.Context
import com.bignerdranch.expandablerecyclerview.model.Parent
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.servicecomponents.EngineController
import com.stardust.autojs.servicecomponents.TaskInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.timing.IntentTask
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTaskManager.allIntentTasksAsList
import org.autojs.autojs.timing.TimedTaskManager.allTasksAsList
import org.autojs.autojs.ui.main.task.Task.PendingTask
import org.autojs.autoxjs.R

/**
 * Created by Stardust on 2017/11/28.
 */
abstract class TaskGroup protected constructor(val title: String) : Parent<TaskInfo> {
    protected var mTasks: MutableList<TaskInfo> = ArrayList()

    override fun getChildList(): List<TaskInfo> {
        return mTasks
    }

    override fun isInitiallyExpanded(): Boolean {
        return true
    }

    abstract suspend fun refresh()

    @OptIn(DelicateCoroutinesApi::class)
    class PendingTaskGroup(context: Context) :
        TaskGroup(context.getString(R.string.text_timed_task)) {
        init {
            GlobalScope.launch { refresh() }
        }

        override suspend fun refresh() {
            mTasks.clear()
            for (timedTask in allTasksAsList) {
                mTasks.add(PendingTask(timedTask))
            }
            for (intentTask in allIntentTasksAsList) {
                mTasks.add(PendingTask(intentTask))
            }
        }

        fun addTask(task: Any): Int {
            val pos = mTasks.size
            if (task is TimedTask) {
                mTasks.add(PendingTask(task))
            } else if (task is IntentTask) {
                mTasks.add(PendingTask(task))
            } else {
                throw IllegalArgumentException("task = $task")
            }
            return pos
        }

        fun removeTask(data: Any): Int {
            val i = indexOf(data)
            if (i >= 0) mTasks.removeAt(i)
            return i
        }

        private fun indexOf(data: Any): Int {
            for (i in mTasks.indices) {
                val task = mTasks[i] as PendingTask
                if (task.taskEquals(data)) {
                    return i
                }
            }
            return -1
        }

        fun updateTask(task: Any): Int {
            val i = indexOf(task)
            if (i >= 0) {
                if (task is TimedTask) {
                    (mTasks[i] as PendingTask).timedTask = task
                } else if (task is IntentTask) {
                    (mTasks[i] as PendingTask).setIntentTask(task)
                } else {
                    throw IllegalArgumentException("task = $task")
                }
            }
            return i
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    class RunningTaskGroup(context: Context) :
        TaskGroup(context.getString(R.string.text_running_task)) {
        init {
            GlobalScope.launch { refresh() }
        }

        override suspend fun refresh() {
            mTasks.clear()
            val executions = withContext(Dispatchers.Default) {
                EngineController.getAllScriptTasks().await()
            }
            mTasks.addAll(executions)
        }

        fun addTask(engine: ScriptExecution): Int {
            val pos = mTasks.size
            mTasks.add(TaskInfo.BundleTaskInfo.fromException(engine))
            return pos
        }

        fun removeTask(engine: ScriptExecution): Int {
            val i = indexOf(engine)
            if (i >= 0) {
                mTasks.removeAt(i)
            }
            return i
        }

        fun indexOf(engine: ScriptExecution): Int {
            for (i in mTasks.indices) {
                if (engine.source.toString() == mTasks[i].sourcePath) {
                    return i
                }
            }
            return -1
        }
    }
}
