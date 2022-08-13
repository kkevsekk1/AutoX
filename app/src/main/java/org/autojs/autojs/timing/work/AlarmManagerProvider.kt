package org.autojs.autojs.timing.work

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import org.autojs.autojs.App
import org.autojs.autoxjs.BuildConfig
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.timing.TimedTaskScheduler
import java.util.concurrent.TimeUnit

/**
 * Created by TonyJiangWJ(https://github.com/TonyJiangWJ).
 * From [TonyJiangWJ/Auto.js](https://github.com/TonyJiangWJ/Auto.js)
 */
object AlarmManagerProvider : TimedTaskScheduler() {

    private const val ACTION_CHECK_TASK = "org.autojs.autojs.action.check_task"
    private const val LOG_TAG = "AlarmManagerProvider"
    private const val REQUEST_CODE_CHECK_TASK_REPEATEDLY = 4000
    private val INTERVAL = TimeUnit.MINUTES.toMillis(15)
    private val MIN_INTERVAL_GAP = TimeUnit.MINUTES.toMillis(5)
    private val SCHEDULE_TASK_MIN_TIME = TimeUnit.DAYS.toMillis(2)

    private var sCheckTasksPendingIntent: PendingIntent? = null

    internal class AlarmManagerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            autoJsLog("onReceiveRtcWakeUp")
            checkTasks(AlarmManagerProvider.context, false)
            setupNextRtcWakeup(context, System.currentTimeMillis() + INTERVAL)
        }
    }

    private var context: Application = App.app

    override fun enqueueWork(timedTask: TimedTask, timeWindow: Long) {
        autoJsLog("enqueue task:$timedTask")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val op = timedTask.createPendingIntent(context)
        setExactCompat(alarmManager, op, System.currentTimeMillis() + timeWindow)
    }

    override fun enqueuePeriodicWork(delay: Int) {
        autoJsLog("checkTasksRepeatedlyIfNeeded")
        checkTasksRepeatedlyIfNeeded(context)
    }

    override fun cancel(context: Application, timedTask: TimedTask) {
        autoJsLog("cancel task:$timedTask")
        val alarmManager = getAlarmManager(context)
        alarmManager.cancel(timedTask.createPendingIntent(context))
    }

    @SuppressLint("CheckResult")
    override fun cancelAllWorks() {
        autoJsLog("cancel all tasks")
        stopRtcRepeating(context)
        TimedTaskManager
            .allTasks
            .filter(TimedTask::isScheduled)
            .forEach { timedTask: TimedTask ->
                cancel(context, timedTask)
                timedTask.isScheduled = false
                timedTask.executed = false
                TimedTaskManager.updateTaskWithoutReScheduling(timedTask)
            }
    }

    override val isCheckWorkFine: Boolean
        get() = true

    fun runTask(context: Application, task: TimedTask) {
        Log.d(LOG_TAG, "run task: task = $task")
        val intent = task.createIntent()
        ScriptIntents.handleIntent(context, intent)
        TimedTaskManager.notifyTaskFinished(task.id)
        // 如果队列中有任务正在等待，直接取消
        cancel(context, task)
    }

    private fun setExactCompat(alarmManager: AlarmManager, op: PendingIntent, millis: Long) {
        var millis = millis
        var type = AlarmManager.RTC_WAKEUP
        val gapMillis = millis - System.currentTimeMillis()
        if (gapMillis <= MIN_INTERVAL_GAP) {
            val oldMillis = millis
            // 目标时间修改为真实时间
            // elapsedRealtime() and elapsedRealtimeNanos() 返回系统启动到现在的时间，
            // 包含设备深度休眠的时间。该时钟被保证是单调的，即使CPU在省电模式下，该时间也会继续计时。
            // 该时钟可以被使用在当测量时间间隔可能跨越系统睡眠的时间段。
            millis = SystemClock.elapsedRealtime() + gapMillis
            type = AlarmManager.ELAPSED_REALTIME_WAKEUP
            autoJsLog("less then 5 minutes, millis changed from $oldMillis to $millis")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(type, millis, op)
        } else {
            alarmManager.setAlarmClock(AlarmClockInfo(millis, null), op)
        }
    }

    private fun checkTasksRepeatedlyIfNeeded(context: Context) {
        autoJsLog("checkTasksRepeatedlyIfNeeded count:" + TimedTaskManager.countTasks())
        if (TimedTaskManager.countTasks() > 0) {
            // 设置周期性时间6分钟
            setupNextRtcWakeup(context, System.currentTimeMillis() + INTERVAL)
        }
    }

    private fun setupNextRtcWakeup(context: Context, millis: Long) {
        autoJsLog("setupNextRtcWakeup: at $millis")
        require(millis > 0) { "millis <= 0: $millis" }
        val alarmManager = getAlarmManager(context)
        setExactCompat(alarmManager, createTaskCheckPendingIntent(context), millis)
    }

    private fun stopRtcRepeating(context: Context) {
        autoJsLog("stopRtcRepeating")
        val alarmManager = getAlarmManager(context)
        alarmManager.cancel(createTaskCheckPendingIntent(context))
    }

    private fun getAlarmManager(context: Context): AlarmManager {
        return (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
    }

    private fun createTaskCheckPendingIntent(context: Context): PendingIntent {
        if (sCheckTasksPendingIntent == null) {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            sCheckTasksPendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE_CHECK_TASK_REPEATEDLY,
                Intent(ACTION_CHECK_TASK)
                    .setComponent(
                        ComponentName(
                            BuildConfig.APPLICATION_ID,
                            "org.autojs.autojs.timing.work.AlarmManagerProvider"
                        )
                    ),
                flags
            )
        }
        return sCheckTasksPendingIntent!!
    }

    override fun autoJsLog(content: String) {
        Log.d(LOG_TAG, content)
        super.autoJsLog(content)
    }

}