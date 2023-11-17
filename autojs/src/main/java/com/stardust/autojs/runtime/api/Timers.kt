package com.stardust.autojs.runtime.api

import android.os.Looper
import com.stardust.autojs.core.looper.Timer
import com.stardust.autojs.core.looper.TimerThread
import com.stardust.autojs.runtime.ScriptRuntime

/**
 * Created by Stardust on 2017/7/21.
 */
class Timers(private val mRuntime: ScriptRuntime) {
    private val mThreads: Threads = mRuntime.threads
    private val mUiTimer: Timer = Timer(mRuntime, Looper.getMainLooper())
    val mainTimer
        get() = mRuntime.loopers.mTimer

    val timerForCurrentThread: Timer
        get() = getTimerForThread(Thread.currentThread())

    fun getTimerForThread(thread: Thread): Timer {
        if (thread === mThreads.mainThread) {
            return mRuntime.loopers.mTimer
        }
        val timer = TimerThread.getTimerForThread(thread)
        return if (timer == null && Looper.myLooper() == Looper.getMainLooper()) {
            mUiTimer
        } else timer ?: mainTimer
    }

    fun setTimeout(vararg args: Any?): Int {
        val listener = args.elementAtOrNull(0)
        check(listener != null) { "callback cannot be null" }
        val delay = (args.elementAtOrNull(1) as? Double)?.toLong() ?: 1
        return timerForCurrentThread.setTimeout(listener, delay, *args.drop(2).toTypedArray())
    }


    fun setImmediate(vararg args: Any?): Int {
        val listener = args.elementAtOrNull(0)
        check(listener != null) { "callback cannot be null" }
        return timerForCurrentThread.setImmediate(listener, *args.drop(1).toTypedArray())
    }


    fun setInterval(vararg args: Any?): Int {
        val listener = args.elementAtOrNull(0)
        check(listener != null) { "callback cannot be null" }
        val delay = (args.elementAtOrNull(1) as? Double)?.toLong() ?: 1
        return timerForCurrentThread.setInterval(listener, delay, *args.drop(2).toTypedArray())
    }

    fun clearTimeout(id: Int): Boolean {
        return timerForCurrentThread.clearTimeout(id)
    }

    fun clearInterval(id: Int): Boolean {
        return timerForCurrentThread.clearInterval(id)
    }

    fun clearImmediate(id: Int): Boolean {
        return timerForCurrentThread.clearImmediate(id)
    }

    fun recycle() {
        mainTimer.removeAllCallbacks()
    }

    companion object {
        private const val LOG_TAG = "Timers"
    }
}