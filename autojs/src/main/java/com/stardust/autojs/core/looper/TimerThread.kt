package com.stardust.autojs.core.looper

import android.os.Looper
import androidx.annotation.CallSuper
import com.stardust.autojs.engine.RhinoJavaScriptEngine
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import com.stardust.lang.ThreadCompat
import org.mozilla.javascript.Context

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
        }
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

    fun setTimeout(vararg args: Any?): Int {
        val listener = args.elementAtOrNull(0)
        check(listener != null) { "callback cannot be null" }
        val delay = (args.elementAtOrNull(1) as? Double)?.toLong() ?: 1
        return timer.setTimeout(listener, delay, *args.drop(2).toTypedArray())
    }

    fun setImmediate(vararg args: Any?): Int {
        val listener = args.elementAtOrNull(0)
        check(listener != null) { "callback cannot be null" }
        return timer.setImmediate(listener, *args.drop(1).toTypedArray())
    }

    fun setInterval(vararg args: Any?): Int {
        val listener = args.elementAtOrNull(0)
        check(listener != null) { "callback cannot be null" }
        val delay = (args.elementAtOrNull(1) as? Double)?.toLong() ?: 1
        return timer.setInterval(listener, delay, *args.drop(2).toTypedArray())
    }

    val timer: Timer
        get() {
            checkNotNull(mTimer) { "thread is not alive" }
            return mTimer as Timer
        }

    fun clearTimeout(id: Int): Boolean {
        return timer.clearTimeout(id)
    }

    fun clearInterval(id: Int): Boolean {
        return timer.clearInterval(id)
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
}