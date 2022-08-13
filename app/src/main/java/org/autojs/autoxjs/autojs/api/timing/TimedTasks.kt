package org.autojs.autoxjs.autojs.api.timing

import com.stardust.autojs.execution.ExecutionConfig
import org.autojs.autoxjs.timing.TimedTask
import org.autojs.autoxjs.timing.TimedTaskManager
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime

object TimedTasks {

    fun daily(path: String, hour: Int, minute: Int) {
        TimedTaskManager.addTask(
            TimedTask.dailyTask(
                LocalTime(hour, minute),
                path,
                ExecutionConfig()
            )
        )
    }

    fun disposable(path: String, millis: Long) {
        TimedTaskManager.addTask(
            TimedTask.disposableTask(
                LocalDateTime(millis),
                path,
                ExecutionConfig()
            )
        )
    }

    fun weekly(path: String, millis: Long, timeFlag: Long) {
        TimedTaskManager.addTask(
            TimedTask.weeklyTask(
                LocalDateTime(millis).toLocalTime(),
                timeFlag,
                path,
                ExecutionConfig()
            )
        )
    }

}