package com.stardust.autojs.core.looper

import android.os.Looper
import androidx.annotation.CallSuper
import com.stardust.autojs.engine.RhinoJavaScriptEngine
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import com.stardust.lang.ThreadCompat
import org.mozilla.javascript.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Stardust on 2017/12/27.
 */
open class TimerThread(private val mRuntime: ScriptRuntime, private val mTarget: Runnable) :
    ThreadCompat(mTarget) {
    private var mTimer: Timer? = null
    private var mRunning = false
    private val mRunningLock = java.lang.Object()
    private val mAsyncTask = Loopers.AsyncTask("TimerThread")
    var loopers: Loopers? = null
    init {
        mRuntime.loopers.addAsyncTask(mAsyncTask)
    }
    override fun run() {
        loopers = Loopers(mRuntime)
        mTimer = loopers!!.mTimer
        sTimerMap[currentThread()] = mTimer!!
        (mRuntime.engines.myEngine() as RhinoJavaScriptEngine).enterContext()
        notifyRunning()
        mTimer!!.post(mTarget)
        try {
            Looper.loop()
        } catch (e: Throwable) {
            if (!ScriptInterruptedException.causedByInterrupted(e)) {
                mRuntime.console.error(currentThread().toString() + ": ", e)
            }
        } finally {
            //mRuntime.console.log("TimerThread exit");
            onExit()
            mTimer = null
            Context.exit()
            sTimerMap.remove(currentThread(), mTimer)
        }
    }

    override fun interrupt() {
        LooperHelper.quitForThread(this)
        super.interrupt()
    }

    private fun notifyRunning() {
        synchronized(mRunningLock) {
            mRunning = true
            mRunningLock.notifyAll()
        }
    }

    @CallSuper
    protected open fun onExit() {
        mRuntime.loopers.removeAsyncTask(mAsyncTask)
        mRuntime.loopers.notifyThreadExit(this)
    }

    fun setTimeout(callback: Any, delay: Long, vararg args: Any?): Int {
        return timer.setTimeout(callback, delay, *args as Array<out Any>)
    }

    fun setTimeout(callback: Any): Int {
        return setTimeout(callback, 1)
    }

    val timer: Timer
        get() {
            checkNotNull(mTimer) { "thread is not alive" }
            return mTimer as Timer
        }

    fun clearTimeout(id: Int): Boolean {
        return timer.clearTimeout(id)
    }

    fun setInterval(listener: Any?, interval: Long, vararg args: Any?): Int {
        return timer.setInterval(listener!!, interval, *args as Array<out Any>)
    }

    fun setInterval(listener: Any?): Int {
        return setInterval(listener, 1)
    }

    fun clearInterval(id: Int): Boolean {
        return timer.clearInterval(id)
    }

    fun setImmediate(listener: Any, vararg args: Any?): Int {
        return timer.setImmediate(listener, *args as Array<out Any>)
    }

    fun clearImmediate(id: Int): Boolean {
        return timer.clearImmediate(id)
    }

    @Throws(InterruptedException::class)
    fun waitFor() {
        synchronized(mRunningLock) {
            if (mRunning) return
            mRunningLock.wait()
        }
    }

    override fun toString(): String {
        return "Thread[$name,$priority]"
    }

    companion object {
        private val sTimerMap = ConcurrentHashMap<Thread, Timer?>()

        @JvmStatic
        fun getTimerForThread(thread: Thread): Timer? {
            return sTimerMap[thread]
        }

        val timerForCurrentThread: Timer?
            get() = getTimerForThread(currentThread())
    }
}