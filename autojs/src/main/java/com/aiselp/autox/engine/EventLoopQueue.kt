package com.aiselp.autox.engine

import android.util.Log
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.primitive.V8ValueLong
import com.caoccao.javet.values.reference.V8ValueFunction
import com.caoccao.javet.values.reference.V8ValueObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

class EventLoopQueue(val runtime: NodeRuntime) {
    private val queue = ArrayDeque<Runnable>()
    private val queueX = ArrayDeque<Runnable>()
    private var currentQueue = queue
    private val util = runtime.getExecutor(
        """
        (()=>{
            let id = 0n;
            let keepId
            const callbacks = new Map();
            return {
                addCallback: function(fn){
                    if (callbacks.has(fn)) {
                        return callbacks.get(fn)
                    }
                    id++;
                    callbacks.set(id, fn);
                    callbacks.set(fn, id);
                    return id;
                },
                emit: function(id, ...args){
                    return callbacks.get(id)?.(...args);
                },
                removeCallback: function(id){
                    callbacks.delete(callbacks.get(id));
                    callbacks.delete(id);
                },
                keepRunning: function(){
                    if (keepId)  return;
                    keepId = setInterval(()=>{}, 1000)
                },
                cancelKeepRunning: function(){
                    clearInterval(keepId)
                }
            }
        })()
    """.trimIndent()
    ).execute<V8ValueObject>()

    @Volatile
    var isKeepRunning = false
    private val job = Job()

    fun keepRunning() {
        if (!isKeepRunning) {
            isKeepRunning = true
            addTask { util.invokeVoid("keepRunning", null) }
        }
    }

    fun cancelKeepRunning() {
        if (isKeepRunning) {
            isKeepRunning = false
            addTask { util.invokeVoid("cancelKeepRunning", null) }
        }
    }

    fun createV8Callback(fn: V8ValueFunction): V8Callback {

        val id = util.invoke<V8ValueLong>("addCallback", fn)
        id.use {
            return LastingV8Callback(id.asLong())
        }
    }

    fun createWeakV8Callback(fn: V8ValueFunction): V8Callback {
        return WeakV8Callback(fn)
    }

    fun executeQueue(): Boolean {
        val executeQueue: ArrayDeque<Runnable>
        synchronized(this) {
            executeQueue = currentQueue
            if (executeQueue.isEmpty()) {
                return false
            }
            currentQueue = if (currentQueue === queue) {
                queueX
            } else queue
        }
        executeQueue.forEach { it.run() }
        executeQueue.clear()
        return true
    }

    fun addTask(task: Runnable) = synchronized(this) {
        currentQueue.add(task)
    }

    fun recycle() {
        job.cancel()
        cancelKeepRunning()
        util.close()
    }

    /**
     * 此回调会在js上下文中创建一个持久引用，使用完毕应调用close，
     * 此类实现了线程安全，可在任意线程中调用
     */
    inner class LastingV8Callback(val id: Long) : V8Callback {
        private var removerd = false

        private fun call(vararg args: Any?): Any? {
            val result = runtime.converter.toObject<Any?>(util.invoke("emit", id, *args))
            if (result is V8Value) {
                result.close()
                return null
            } else return result
        }

        override fun invoke(vararg args: Any?): CompletableDeferred<Any?> {
            if (removerd) {
                Log.w(TAG, "this callback[${id}] has been removed")
                return CompletableDeferred<Any?>().also { it.complete(null) }
            }
            val deferred = CompletableDeferred<Any?>(job)
            this@EventLoopQueue.addTask { deferred.complete(call(*args)) }
            return deferred
        }

        override fun invokeSync(vararg args: Any?): Any? = runBlocking {
            invoke(*args).await()
        }

        override suspend fun invokeAsync(vararg args: Any?): Any? {
            return invoke(*args).await()
        }

        override fun close() {
            removerd = true
            addTask {
                util.invokeVoid("removeCallback", id)
            }
        }
    }

    /**
     * 弱引用回调，当V8ValueFunction在js上下文失去引用并被回收后，此回调将失效
     */
    inner class WeakV8Callback(val fn: V8ValueFunction) : V8Callback {
        init {
            fn.setWeak()
        }

        private fun call(vararg args: Any?): Any? {
            if (fn.isClosed) {
                Log.d(TAG, "this callback[${fn}] has been closed")
                return null
            }
            val result = runtime.converter.toObject<Any?>(fn.call(null, *args))
            if (result is V8Value) {
                result.close()
                return null
            } else return result
        }

        override fun invoke(vararg args: Any?): CompletableDeferred<Any?> {
            val deferred = CompletableDeferred<Any?>(job)
            this@EventLoopQueue.addTask { deferred.complete(call(*args)) }
            return deferred
        }

        override fun invokeSync(vararg args: Any?): Any? = runBlocking {
            invoke(*args).await()
        }

        override suspend fun invokeAsync(vararg args: Any?): Any? {
            return invoke(*args).await()
        }

        override fun close() {
        }
    }

    interface V8Callback : AutoCloseable {
        fun invoke(vararg args: Any?): CompletableDeferred<Any?>
        fun invokeSync(vararg args: Any?): Any?
        suspend fun invokeAsync(vararg args: Any?): Any?
    }

    companion object {
        private const val TAG = "EventLoopQueue"
    }
}