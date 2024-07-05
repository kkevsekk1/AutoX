package com.aiselp.autox.engine

import com.caoccao.javet.interop.NodeRuntime
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
                    id++;
                    callbacks.set(id, fn);
                    return id;
                },
                emit: function(id, ...args){
                    callbacks.get(id)(...args);
                },
                removeCallback: function(id){
                    callbacks.delete(id);
                },
                keepRunning: function(){
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
            return V8Callback(id.asLong())
        }
    }

    fun removeV8Callback(callback: V8Callback) {
        callback.remove()
    }

    fun executeQueue(): Boolean = synchronized(this) {
        val executeQueue = currentQueue
        if (executeQueue.isEmpty()) {
            return false
        }
        currentQueue = if (currentQueue === queue) {
            queueX
        } else queue
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

    inner class V8Callback(val id: Long) {
        @Volatile
        private var removerd = false

        private fun call(vararg args: Any?): Any? {
            return runtime.converter.toObject<Any?>(util.invoke("emit", id, *args))
        }

        fun invoke(vararg args: Any?): CompletableDeferred<Any?> {
            check(!removerd) { "this callback has been removed" }
            val deferred = CompletableDeferred<Any?>(job)
            this@EventLoopQueue.addTask { deferred.complete(call(*args)) }
            return deferred
        }

        fun invokeSync(vararg args: Any?): Any? = runBlocking {
            invoke(*args).await()
        }

        suspend fun invokeAsync(vararg args: Any?): Any? {
            return invoke(*args).await()
        }

        fun remove() {
            removerd = true
            this@EventLoopQueue.addTask {
                util.invokeVoid("removeCallback", id)
            }
        }

        fun cancel() {

        }
    }

    companion object {
        private val TAG = "EventLoopQueue"
    }
}