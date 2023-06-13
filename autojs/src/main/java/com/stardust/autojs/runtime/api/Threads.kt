package com.stardust.autojs.runtime.api

import android.os.Build
import androidx.annotation.RequiresApi
import com.stardust.autojs.core.looper.Loopers
import com.stardust.autojs.core.looper.MainThreadProxy
import com.stardust.autojs.core.looper.TimerThread
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import com.stardust.concurrent.VolatileDispose
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by Stardust on 2017/12/3.
 */
class Threads(private val mRuntime: ScriptRuntime) {
    private val mThreads = HashSet<Thread>()
    val mainThread: Thread = Thread.currentThread()
    private val mMainThreadProxy = MainThreadProxy(Thread.currentThread(), mRuntime)
    private var mSpawnCount = 0
    private var mTaskCount = AtomicLong(0)
    private var mExit = false
    private val looperTask = Loopers.AsyncTask("AsyncTaskThreadPool")
    private val threadPool = Executors.newFixedThreadPool(20, ThreadFactory {
        val thread = Thread(fun() {
            Context.enter()
            try {
                it.run()
            } finally {
                Context.exit()
            }
        })
        thread.name = mainThread.name + " (AsyncThread)"
        thread
    })

    fun currentThread(): Any {
        val thread = Thread.currentThread()
        return if (thread === mainThread) mMainThreadProxy else thread
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun runTaskForThreadPool(runnable: BaseFunction) {
        if (mTaskCount.addAndGet(1) == 1L) mRuntime.loopers.addAsyncTask(looperTask)
        threadPool.execute {
            try {
                runnable.call(
                    Context.getCurrentContext(), runnable.parentScope, runnable,
                    emptyArray()
                )
            } catch (e: Throwable) {
                if (!ScriptInterruptedException.causedByInterrupted(e)) {
                    mRuntime.console.error("$this: ", e)
                }
            } finally {
                if (mTaskCount.addAndGet(-1) == 0L) {
                    mRuntime.loopers.removeAsyncTask(looperTask)
                }
            }
        }
    }

    fun start(runnable: Runnable): TimerThread {
        val thread = createThread(runnable)
        synchronized(mThreads) {
            check(!mExit) { "script exiting" }
            mThreads.add(thread)
            thread.name = mainThread.name + " (Spawn-" + mSpawnCount + ")"
            mSpawnCount++
            thread.start()
        }
        return thread
    }

    private fun createThread(runnable: Runnable): TimerThread {
        return object : TimerThread(mRuntime, runnable) {
            override fun onExit() {
                synchronized(mThreads) { mThreads.remove(currentThread()) }
                super.onExit()
            }
        }
    }

    fun disposable(): VolatileDispose<*> {
        return VolatileDispose<Any?>()
    }

    fun atomic(value: Long): AtomicLong {
        return AtomicLong(value)
    }

    fun atomic() = AtomicLong()

    fun lock() = ReentrantLock()

    fun shutDownAll() {
        threadPool.shutdownNow()
        synchronized(mThreads) {
            for (thread in mThreads) {
                thread.interrupt()
            }
            mThreads.clear()
        }
    }

    fun exit() {
        synchronized(mThreads) {
            shutDownAll()
            mExit = true
        }
    }

    fun hasRunningThreads(): Boolean {
        synchronized(mThreads) { return mThreads.isNotEmpty() }
    }
}