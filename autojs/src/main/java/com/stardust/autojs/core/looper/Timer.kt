package com.stardust.autojs.core.looper

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.stardust.autojs.runtime.ScriptRuntime
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Created by Stardust on 2017/12/27.
 */
class Timer(
    runtime: ScriptRuntime,
    looper: Looper
) {
    private val myLooper: Looper = looper
    private val mHandlerCallbacks = ConcurrentHashMap<Int, Runnable?>()
    private val mRuntime: ScriptRuntime = runtime
    private val mHandler: Handler = Handler(looper)
    private val isUiLoop: Boolean = looper == Looper.getMainLooper()
    private val context: Context? by lazy { Context.getCurrentContext() }

    constructor(runtime: ScriptRuntime) : this(runtime, Looper.myLooper()!!)

    fun setTimeout(callback: Any, delay: Long, vararg args: Any?): Int {
        val id = createTimerId()
        val r = Runnable {
            callFunction(callback, null, args)
            mHandlerCallbacks.remove(id)
        }
        mHandlerCallbacks[id] = r
        postDelayed(r, delay)
        return id
    }

    private fun callFunction(callback: Any, thiz: Any?, args: Any?) {
        val func = callback as BaseFunction
        val map: Array<Any> =
            (args as? Array<*>)?.map { Context.javaToJS(it, callback.parentScope) }
                ?.toTypedArray() ?: emptyArray()
        try {
            func.call(
                context ?: Context.enter(), func.parentScope,
                thiz as? Scriptable ?: Undefined.SCRIPTABLE_UNDEFINED, map
            )
        } catch (e: Exception) {
            if (isUiLoop) {
                mRuntime.exit(e)
            } else throw e
        }finally {
            context ?: Context.exit()
        }
    }

    @Synchronized
    private fun createTimerId(): Int {
        var id: Int
        do {
            id = Random.nextInt()
        } while (mHandlerCallbacks.containsKey(id))
        mHandlerCallbacks[id] = EMPTY_RUNNABLE
        return id
    }

    fun setInterval(listener: Any, interval: Long, vararg args: Any?): Int {
        val id = createTimerId()
        val r: Runnable = object : Runnable {
            override fun run() {
                if (mHandlerCallbacks[id] == null) return
                callFunction(listener, null, args)
                postDelayed(this, interval)
            }
        }
        mHandlerCallbacks[id] = r
        postDelayed(r, interval)
        return id
    }

    fun postDelayed(r: Runnable, interval: Long) {
        synchronized(myLooper) {
            val uptime = SystemClock.uptimeMillis() + interval
            mHandler.postAtTime(r, uptime)
        }
    }

    fun post(r: Runnable) {
        synchronized(myLooper) {
            mHandler.post(r)
        }
    }

    fun clearInterval(id: Int): Boolean = clearCallback(id)
    fun clearImmediate(id: Int): Boolean = clearCallback(id)
    fun clearTimeout(id: Int): Boolean = clearCallback(id)

    fun setImmediate(listener: Any, vararg args: Any?): Int {
        val id = createTimerId()
        val r = Runnable {
            callFunction(listener, null, args)
            mHandlerCallbacks.remove(id)
        }
        mHandlerCallbacks[id] = r
        post(r)
        return id
    }


    private fun clearCallback(id: Int): Boolean {
        val callback = mHandlerCallbacks[id]
        if (callback != null) {
            mHandler.removeCallbacks(callback)
            mHandlerCallbacks.remove(id)
            if (mHandlerCallbacks.isEmpty()) mHandler.post(EMPTY_RUNNABLE)
            return true
        }
        return false
    }

    fun hasPendingCallbacks(): Boolean {
        return mHandlerCallbacks.size > 0
    }

    fun removeAllCallbacks() {
        mHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val LOG_TAG = "Timer"
        private val EMPTY_RUNNABLE = Runnable {}
    }
}