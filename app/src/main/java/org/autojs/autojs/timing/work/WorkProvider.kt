package org.autojs.autojs.timing.work

import android.app.Application
import org.autojs.autojs.timing.TimedTask

/**
 * Created by TonyJiangWJ(https://github.com/TonyJiangWJ).
 * From [TonyJiangWJ/Auto.js](https://github.com/TonyJiangWJ/Auto.js)
 */
interface WorkProvider {
    /**
     * 创建定时执行的任务
     *
     * @param timedTask 任务信息
     * @param timeWindow 延迟时间
     */
    fun enqueueWork(timedTask: TimedTask, timeWindow: Long)

    /**
     * 创建定期执行的任务
     *
     * @param delay 延迟启动时间
     */
    fun enqueuePeriodicWork(delay: Int)

    /**
     * 取消定时任务
     *
     * @param timedTask
     */
    fun cancel(context: Application, timedTask: TimedTask)
    fun cancelAllWorks()
    val isCheckWorkFine: Boolean
    fun checkTasks(context: Application, force: Boolean)
    fun scheduleTaskIfNeeded(context: Application, timedTask: TimedTask, force: Boolean)
    fun scheduleTask(context: Application, timedTask: TimedTask, millis: Long, force: Boolean)
}