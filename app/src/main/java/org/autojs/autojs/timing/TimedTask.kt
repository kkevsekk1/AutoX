package org.autojs.autojs.timing

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.stardust.autojs.execution.ExecutionConfig
import org.autojs.autoxjs.BuildConfig
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.storage.database.BaseModel
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import java.util.concurrent.TimeUnit

class TimedTask : BaseModel {
    var timeFlag: Long = 0
    var isScheduled = false
    var delay: Long = 0
    var interval: Long = 0
    var loopTimes = 1
    var millis: Long = 0

    /**
     * 目标执行时间
     */
    var targetExecuteMillis: Long = 0

    /**
     * 已执行过
     */
    var executed = false
    var scriptPath: String? = null

    constructor() {}
    constructor(millis: Long, timeFlag: Long, scriptPath: String?, config: ExecutionConfig) {
        this.millis = millis
        this.timeFlag = timeFlag
        this.scriptPath = scriptPath
        delay = config.delay
        loopTimes = config.loopTimes
        interval = config.interval
        executed = false
        targetExecuteMillis = 0
        // 重新计算目标执行时间点
        nextTime
    }

    val isDisposable: Boolean
        get() = timeFlag == FLAG_DISPOSABLE.toLong()

    // 更新目标执行时间，并标记为未执行
    val nextTime: Long
        get() {
            if (isDisposable) {
                targetExecuteMillis = millis
                return millis
            }
            if (targetExecuteMillis < 10 || targetExecuteMillis < System.currentTimeMillis() && executed) {
                // 更新目标执行时间，并标记为未执行
                executed = false
                targetExecuteMillis = if (isDaily) nextTimeOfDailyTask else nextTimeOfWeeklyTask
            }
            return targetExecuteMillis
        }
    private val nextTimeOfDailyTask: Long
        get() {
            val time = LocalTime.fromMillisOfDay(millis)
            val nextTimeMillis = time.toDateTimeToday().millis
            return if (System.currentTimeMillis() > nextTimeMillis) {
                nextTimeMillis + TimeUnit.DAYS.toMillis(1)
            } else {
                nextTimeMillis
            }
        }
    private val nextTimeOfWeeklyTask: Long
        get() {
            var dayOfWeek = DateTime.now().dayOfWeek
            var nextTimeMillis = LocalTime.fromMillisOfDay(millis).toDateTimeToday().millis
            for (i in 0..7) {
                if (getDayOfWeekTimeFlag(dayOfWeek) and timeFlag != 0L) {
                    if (System.currentTimeMillis() <= nextTimeMillis) {
                        return nextTimeMillis
                    }
                }
                dayOfWeek++
                nextTimeMillis += TimeUnit.DAYS.toMillis(1)
            }
            throw IllegalStateException("Should not happen! timeFlag = " + timeFlag + ", dayOfWeek = " + DateTime.now().dayOfWeek)
        }
    val isDaily: Boolean
        get() = timeFlag == FLAG_EVERYDAY.toLong()

    fun createIntent(): Intent {
        return Intent(TaskReceiver.ACTION_TASK)
            .setComponent(
                ComponentName(
                    BuildConfig.APPLICATION_ID,
                    TaskReceiver::class.java.name
                )
            )
            .putExtra(TaskReceiver.EXTRA_TASK_ID, id)
            .putExtra(ScriptIntents.EXTRA_KEY_PATH, scriptPath)
            .putExtra(ScriptIntents.EXTRA_KEY_DELAY, delay)
            .putExtra(ScriptIntents.EXTRA_KEY_LOOP_TIMES, loopTimes)
            .putExtra(ScriptIntents.EXTRA_KEY_LOOP_INTERVAL, interval)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createPendingIntent(context: Context): PendingIntent {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(
            context, ((REQUEST_CODE + 1 + id) % 65535).toInt(),
            createIntent(), flags
        )
    }

    override fun toString(): String {
        return "TimedTask{" +
                "mId=" + id +
                ", mTimeFlag=" + timeFlag +
                ", mScheduled=" + isScheduled +
                ", mDelay=" + delay +
                ", mInterval=" + interval +
                ", mLoopTimes=" + loopTimes +
                ", mMillis=" + millis +
                ", targetExecuteMillis=" + targetExecuteMillis +
                ", executed=" + executed +
                ", mScriptPath='" + scriptPath + '\'' +
                '}'
    }

    fun hasDayOfWeek(dayOfWeek: Int): Boolean {
        return timeFlag and getDayOfWeekTimeFlag(dayOfWeek) != 0L
    }

    companion object {
        const val TABLE = "TimedTask"
        private const val FLAG_DISPOSABLE = 0
        const val FLAG_SUNDAY = 0x1
        const val FLAG_MONDAY = 0x2
        const val FLAG_TUESDAY = 0x4
        const val FLAG_WEDNESDAY = 0x8
        const val FLAG_THURSDAY = 0x10
        const val FLAG_FRIDAY = 0x20
        const val FLAG_SATURDAY = 0x40
        private const val FLAG_EVERYDAY = 0x7F
        private const val REQUEST_CODE = 2000
        @JvmStatic
        fun getDayOfWeekTimeFlag(dayOfWeek: Int): Long {
            var dayOfWeek = dayOfWeek
            dayOfWeek = (dayOfWeek - 1) % 7 + 1
            when (dayOfWeek) {
                DateTimeConstants.SUNDAY -> return FLAG_SUNDAY.toLong()
                DateTimeConstants.MONDAY -> return FLAG_MONDAY.toLong()
                DateTimeConstants.SATURDAY -> return FLAG_SATURDAY.toLong()
                DateTimeConstants.WEDNESDAY -> return FLAG_WEDNESDAY.toLong()
                DateTimeConstants.TUESDAY -> return FLAG_TUESDAY.toLong()
                DateTimeConstants.THURSDAY -> return FLAG_THURSDAY.toLong()
                DateTimeConstants.FRIDAY -> return FLAG_FRIDAY.toLong()
            }
            throw IllegalArgumentException("dayOfWeek = $dayOfWeek")
        }

        @JvmStatic
        fun dailyTask(time: LocalTime, scriptPath: String, config: ExecutionConfig): TimedTask {
            return TimedTask(
                time.millisOfDay.toLong(), FLAG_EVERYDAY.toLong(), scriptPath, config
            )
        }

        @JvmStatic
        fun disposableTask(
            dateTime: LocalDateTime,
            scriptPath: String,
            config: ExecutionConfig
        ): TimedTask {
            return TimedTask(
                dateTime.toDateTime().millis,
                FLAG_DISPOSABLE.toLong(),
                scriptPath,
                config
            )
        }

        @JvmStatic
        fun weeklyTask(
            time: LocalTime,
            timeFlag: Long,
            scriptPath: String,
            config: ExecutionConfig
        ): TimedTask {
            return TimedTask(time.millisOfDay.toLong(), timeFlag, scriptPath, config)
        }
    }
}