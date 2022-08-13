package org.autojs.autojs.timing.work

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import org.autojs.autojs.App
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.timing.TimedTaskScheduler
import java.util.concurrent.TimeUnit

/**
 * Created by TonyJiangWJ(https://github.com/TonyJiangWJ).
 * From [TonyJiangWJ/Auto.js](https://github.com/TonyJiangWJ/Auto.js)
 */
object AndroidJobProvider : TimedTaskScheduler() {

    private const val LOG_TAG = "AndroidJobProvider"
    private var context: Application = App.app

    init {
        JobManager.create(context).addJobCreator { tag: String ->
            if (tag == JOB_TAG_CHECK_TASKS) {
                return@addJobCreator CheckTasksJob(context)
            } else {
                return@addJobCreator TimedTaskJob(context)
            }
        }
    }

    override fun enqueueWork(timedTask: TimedTask, timeWindow: Long) {
        JobRequest.Builder(timedTask.id.toString())
            .setExact(timeWindow)
            .build()
            .schedule()
    }

    override fun enqueuePeriodicWork(delay: Int) {
        JobRequest.Builder(JOB_TAG_CHECK_TASKS)
            .setPeriodic(TimeUnit.MINUTES.toMillis(20))
            .build()
            .scheduleAsync()
    }

    override fun cancel(context: Application, timedTask: TimedTask) {
        val cancelCount = JobManager.instance().cancelAllForTag(timedTask.id.toString())
        Log.d(LOG_TAG, "cancel task: task = $timedTask, cancel = $cancelCount")
    }

    @SuppressLint("CheckResult")
    override fun cancelAllWorks() {
        JobManager.instance().cancelAll()
        TimedTaskManager
            .allTasks
            .filter(TimedTask::isScheduled)
            .forEach { timedTask: TimedTask ->
                timedTask.isScheduled = false
                timedTask.executed = false
                TimedTaskManager.updateTaskWithoutReScheduling(timedTask)
            }
    }

    override val isCheckWorkFine: Boolean
        get() {
            val jobSet = JobManager.instance().getAllJobsForTag(JOB_TAG_CHECK_TASKS)
            if (jobSet.isEmpty()) {
                return false
            }
            var workFine = false
            for (job in jobSet) {
                if (!job.isFinished) {
                    workFine = true
                    break
                }
            }
            return workFine
        }

    private class TimedTaskJob(private val context: Application) : Job() {
        override fun onRunJob(params: Params): Result {
            val id = params.tag.toLong()
            val task = TimedTaskManager.getTimedTask(id)
            Log.d(LOG_TAG, "onRunJob: id = $id, task = $task")
            runTask(context, task)
            return Result.SUCCESS
        }
    }

    private class CheckTasksJob(private val context: Application) : Job() {
        override fun onRunJob(params: Params): Result {
            getWorkProvider(context).checkTasks(context, false)
            return Result.SUCCESS
        }
    }

}