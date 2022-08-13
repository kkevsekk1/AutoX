package org.autojs.autojs.timing

import android.annotation.SuppressLint
import android.app.Application
import android.text.TextUtils
import io.reactivex.Flowable
import io.reactivex.Observable
import org.autojs.autojs.App
import org.autojs.autojs.App.Companion.app
import org.autojs.autojs.storage.database.IntentTaskDatabase
import org.autojs.autojs.storage.database.ModelChange
import org.autojs.autojs.storage.database.TimedTaskDatabase
import org.autojs.autojs.timing.TimedTaskScheduler.Companion.getWorkProvider
import org.autojs.autojs.tool.Observers

/**
 * Created by Stardust on 2017/11/27.
 */
object TimedTaskManager {

    private var context: Application = App.app
    private val timedTaskDatabase: TimedTaskDatabase = TimedTaskDatabase(context)
    private val intentTaskDatabase: IntentTaskDatabase = IntentTaskDatabase(context)

    @SuppressLint("CheckResult")
    fun notifyTaskFinished(id: Long) {
        val task = getTimedTask(id)
        if (task.isDisposable) {
            timedTaskDatabase.delete(task)
                .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
        } else {
            task.isScheduled = false
            task.executed = true
            timedTaskDatabase.update(task)
                .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
        }
    }

    @SuppressLint("CheckResult")
    fun removeTask(timedTask: TimedTask) {
        TimedTaskScheduler.getWorkProvider(context).cancel(context, timedTask)
        timedTaskDatabase.delete(timedTask)
            .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    fun addTask(timedTask: TimedTask) {
        timedTaskDatabase.insert(timedTask)
            .subscribe({ id: Long? ->
                timedTask.id = id!!
                getWorkProvider(context).scheduleTaskIfNeeded(context, timedTask, false)
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    fun addTask(intentTask: IntentTask) {
        intentTaskDatabase.insert(intentTask)
            .subscribe({ i: Long? ->
                if (!TextUtils.isEmpty(intentTask.action)) {
                    app.dynamicBroadcastReceivers
                        .register(intentTask)
                }
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    fun removeTask(intentTask: IntentTask) {
        intentTaskDatabase.delete(intentTask)
            .subscribe({ i: Int? ->
                if (!TextUtils.isEmpty(intentTask.action)) {
                    app.dynamicBroadcastReceivers
                        .unregister(intentTask.action)
                }
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    val allTasks: Flowable<TimedTask>
        get() = timedTaskDatabase.queryAllAsFlowable()

    fun getIntentTaskOfAction(action: String?): Flowable<IntentTask> {
        return intentTaskDatabase.query("action = ?", action)
    }

    val timeTaskChanges: Observable<ModelChange<TimedTask>>
        get() = timedTaskDatabase.modelChange

    @SuppressLint("CheckResult")
    fun notifyTaskScheduled(timedTask: TimedTask) {
        timedTask.isScheduled = true
        timedTaskDatabase.update(timedTask)
            .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
    }

    val allTasksAsList: List<TimedTask>
        get() = timedTaskDatabase.queryAll()

    fun getTimedTask(taskId: Long): TimedTask {
        return timedTaskDatabase.queryById(taskId)
    }

    @SuppressLint("CheckResult")
    fun updateTask(task: TimedTask) {
        timedTaskDatabase.update(task)
            .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
        TimedTaskScheduler.getWorkProvider(context).cancel(context, task)
        getWorkProvider(context).scheduleTaskIfNeeded(context, task, false)
    }

    @SuppressLint("CheckResult")
    fun updateTaskWithoutReScheduling(task: TimedTask) {
        timedTaskDatabase.update(task)
            .subscribe(Observers.emptyConsumer()) { obj: Throwable -> obj.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    fun updateTask(task: IntentTask) {
        intentTaskDatabase.update(task)
            .subscribe({ i: Int ->
                if (i > 0 && !TextUtils.isEmpty(task.action)) {
                    app.dynamicBroadcastReceivers
                        .register(task)
                }
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    fun countTasks(): Long {
        return timedTaskDatabase.count()
    }

    val allIntentTasksAsList: List<IntentTask>
        get() = intentTaskDatabase.queryAll()
    val intentTaskChanges: Observable<ModelChange<IntentTask>>
        get() = intentTaskDatabase.modelChange

    fun getIntentTask(intentTaskId: Long): IntentTask {
        return intentTaskDatabase.queryById(intentTaskId)
    }

    val allIntentTasks: Flowable<IntentTask>
        get() = intentTaskDatabase.queryAllAsFlowable()

}