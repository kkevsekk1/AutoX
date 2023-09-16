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

    fun setTimeout(callback: Any, delay: Long, vararg args: Any?): Int {
        return timerForCurrentThread.setTimeout(callback, delay, *args)
    }

    fun setTimeout(callback: Any): Int {
        return setTimeout(callback, 1)
    }

    fun setImmediate(listener: Any, vararg args: Any?): Int {
        return timerForCurrentThread.setImmediate(listener, *args)
    }


    fun setInterval(listener: Any, interval: Long, vararg args: Any?): Int {
        return timerForCurrentThread.setInterval(listener, interval, *args)
    }

    fun setInterval(listener: Any): Int {
        return setInterval(listener, 1)
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