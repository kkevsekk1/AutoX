package com.stardust.autojs.core.looper

import android.os.Looper
import android.os.MessageQueue
import android.util.Log
import com.stardust.autojs.rhino.AutoJsContext
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import com.stardust.lang.ThreadCompat
import org.mozilla.javascript.Context
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by Stardust on 2017/7/29.
 */
/**
 * update by aiselp on 2023/6/4
 * 调整内容：
 * 使此类只负责单loop线程生命周期管理，移除繁琐的调用链
 * 调整timer由此类创建
 *  通过向此类添加AsyncTask以监听线程退出事件
 */
class Loopers(val runtime: ScriptRuntime) {
    @Deprecated("使用AsyncTask代替")
    interface LooperQuitHandler {
        fun shouldQuit(): Boolean
    }

    open class AsyncTask(private val describe: String) {
        private val allBind = ConcurrentLinkedQueue<Loopers>()
        var isEnd: Boolean = false
            private set

        //线程即将退出时调用，返回true阻止线程退出，只要有一个task返回true线程就不会退出
        open fun onFinish(loopers: Loopers): Boolean {
            return true
        }

        fun end() {
            isEnd = true
        }

        //线程正在退出，这里应该结束任务的执行，回收资源
        open fun onStop(loopers: Loopers) {}
        override fun toString(): String {
            return "AsyncTask: $describe"
        }
    }

    private var waitWhenIdle: Boolean

    @Volatile
    private var mServantLooper: Looper? = null
    private var mMainLooperQuitHandler: LooperQuitHandler? = null
    private val allTasks = ConcurrentLinkedQueue<AsyncTask>()
    val mTimer: Timer
    val myLooper: Looper

    init {
        prepare()
        myLooper = Looper.myLooper()!!
        mTimer = Timer(runtime, myLooper)
        waitWhenIdle = myLooper == Looper.getMainLooper()
    }

    fun createAndAddAsyncTask(describe: String): AsyncTask {
        val task = AsyncTask(describe)
        allTasks.add(task)
        return task
    }

    fun addAsyncTask(task: AsyncTask) {
        synchronized(myLooper) {
            allTasks.add(task)
        }
    }

    fun removeAsyncTask(task: AsyncTask) {
        synchronized(myLooper) {
            allTasks.remove(task)
            mTimer.post(EMPTY_RUNNABLE)
        }
    }

    private fun checkTask(): Boolean {
        allTasks.removeAll(allTasks.filter { it.isEnd }.toSet())
        for (task in allTasks) {
            if (task.onFinish(this)) return true
        }
        return false
    }

    private fun shouldQuitLooper(): Boolean {
        synchronized(myLooper) {
            if (Thread.currentThread().isInterrupted) return true
            if (mTimer.hasPendingCallbacks()) return false
            //检查是否有运行中的线程
            if (checkTask()) return false
            if (waitWhenIdle) return false
            if ((Context.getCurrentContext() as AutoJsContext).hasPendingContinuation()) {
                return false
            }
            return true
        }
    }

    private fun initServantThread() {
        ThreadCompat {
            Looper.prepare()
            val lock = this@Loopers as java.lang.Object
            mServantLooper = Looper.myLooper()
            synchronized(lock) { lock.notifyAll() }
            Looper.loop()
        }.start()
    }

    val servantLooper: Looper
        get() {
            if (mServantLooper == null) {
                initServantThread()
                val lock = this as java.lang.Object
                synchronized(lock) {
                    try {
                        lock.wait()
                    } catch (e: InterruptedException) {
                        throw ScriptInterruptedException(e)
                    }
                }
            }
            return mServantLooper!!
        }

    @Deprecated("使用AsyncTask代替")
    fun waitWhenIdle(b: Boolean) {
        waitWhenIdle = b
    }

    fun recycle() {
        Log.d(LOG_TAG, "recycle")
        for (task in allTasks.filter { !it.isEnd }) {
            try {
                task.onStop(this)
            } catch (e: Exception) {
                Log.w(LOG_TAG, e)
            }
        }
        mServantLooper?.quit()
    }

    @Deprecated("使用AsyncTask代替")
    fun setMainLooperQuitHandler(mainLooperQuitHandler: LooperQuitHandler?) {
        mMainLooperQuitHandler = mainLooperQuitHandler
    }

    private fun prepare() {
        if (Looper.myLooper() == null) LooperHelper.prepare()
        Looper.myQueue().addIdleHandler(MessageQueue.IdleHandler {
            if (this == runtime.loopers) {
                Log.d(LOG_TAG, "main looper queueIdle")
                if (shouldQuitLooper() &&
                    mMainLooperQuitHandler != null &&
                    mMainLooperQuitHandler!!.shouldQuit()
                ) {
                    Log.d(LOG_TAG, "main looper quit")
                    Looper.myLooper()!!.quitSafely()
                }
            }else {
                Log.d(LOG_TAG, "looper queueIdle $this")
                if (shouldQuitLooper()) {
                    Log.d(LOG_TAG, "looper quit $this")
                    Looper.myLooper()!!.quitSafely()
                }
            }
            return@IdleHandler true
        })
    }

    fun notifyThreadExit(thread: TimerThread) {
        Log.d(LOG_TAG, "notifyThreadExit: $thread")
        //当子线程退成时，主线程需要检查自身是否退出（主线程在所有子线程执行完成后才能退出，如果主线程已经执行完任务仍然要等待所有子线程），
        //此时通过向主线程发送一个空的Runnable，主线程执行完这个Runnable后会触发IdleHandler，从而检查自身是否退出
        //mHandler.post(EMPTY_RUNNABLE)
    }

    companion object {
        private const val LOG_TAG = "Loopers"
        private val EMPTY_RUNNABLE = Runnable {}
    }
}