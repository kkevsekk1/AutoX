package org.autojs.autojs.timing.work

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.*
import org.autojs.autojs.App
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.timing.TimedTaskScheduler
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by TonyJiangWJ(https://github.com/TonyJiangWJ).
 * From [TonyJiangWJ/Auto.js](https://github.com/TonyJiangWJ/Auto.js)
 */
object WorkManagerProvider : TimedTaskScheduler() {

    private var context: Application = App.app

    override fun enqueueWork(timedTask: TimedTask, timeWindow: Long) {
        autoJsLog("enqueue task:$timedTask")
        WorkManager.getInstance(context).enqueueUniqueWork(
            timedTask.id.toString(),
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequest.Builder(TimedTaskWorker::class.java)
                .addTag(timedTask.id.toString())
                .setInputData(Data.Builder().putLong("taskId", timedTask.id).build())
                .setInitialDelay(timeWindow, TimeUnit.MILLISECONDS)
                .build()
        )
    }

    override fun enqueuePeriodicWork(delay: Int) {
        autoJsLog("enqueueUniquePeriodicWork")
        val builder = PeriodicWorkRequest.Builder(CheckTaskWorker::class.java, 20, TimeUnit.MINUTES)
        if (delay > 0) {
            builder.setInitialDelay(delay.toLong(), TimeUnit.MINUTES)
        }
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            JOB_TAG_CHECK_TASKS,
            ExistingPeriodicWorkPolicy.REPLACE,
            builder.build()
        )
    }

    override fun cancel(context: Application,timedTask: TimedTask) {
        WorkManager.getInstance(context).cancelAllWorkByTag(timedTask.id.toString()).result
        autoJsLog("cancel task: task = $timedTask")
    }

    @SuppressLint("CheckResult")
    override fun cancelAllWorks() {
        autoJsLog("cancel all tasks")
        WorkManager.getInstance(context).cancelAllWork().result
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
            var workFine = false
            var workInfoList: List<WorkInfo>? = null
            try {
                workInfoList = WorkManager.getInstance(context).getWorkInfosForUniqueWork(
                    JOB_TAG_CHECK_TASKS
                ).get()
            } catch (e: Exception) {
                Log.d(LOG_TAG, "获取校验线程失败")
            }
            if (workInfoList != null && workInfoList.isNotEmpty()) {
                for (workInfo in workInfoList) {
                    if (workInfo.state == WorkInfo.State.ENQUEUED) {
                        workFine = true
                        break
                    }
                }
            }
            return workFine
        }

    override fun autoJsLog(content: String) {
        Log.d(LOG_TAG, content)
        super.autoJsLog(content)
    }

    class TimedTaskWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
        override fun doWork(): Result {
            if (isStopped) {
                return Result.success()
            }
            val id = this.inputData.getLong("taskId", -1)
            if (id > -1) {
                val task = TimedTaskManager.getTimedTask(id)
                autoJsLog("onRunJob: id = " + id + ", task = " + task + ", currentMillis=" + System.currentTimeMillis())
                runTask(context, task)
                return Result.success(
                    Data.Builder()
                        .putString(
                            DONE_TIME,
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(
                                Date()
                            )
                        )
                        .build()
                )
            }
            return Result.failure()
        }

        companion object {
            private const val DONE_TIME = "DONE_TIME"
        }
    }

    class CheckTaskWorker(context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {
        override fun doWork(): Result {
            if (isStopped) {
                return Result.success()
            }
            Log.d(LOG_TAG, "定期检测任务运行中")
            AutoJs.getInstance().debugInfo("定期检测任务运行中")
            getWorkProvider(context).checkTasks(context, false)
            return Result.success()
        }
    }

}