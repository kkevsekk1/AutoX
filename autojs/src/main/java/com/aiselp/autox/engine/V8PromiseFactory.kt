package com.aiselp.autox.engine

import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueFunction
import com.caoccao.javet.values.reference.V8ValueObject
import com.caoccao.javet.values.reference.V8ValuePromise

class V8PromiseFactory(val runtime: V8Runtime,  val eventLoopQueue: EventLoopQueue) {
    private val executor = runtime.getExecutor(
        """
        (()=>{
            const adapter = {};
         
            adapter.promise = new Promise(function(resolve, reject) {
                adapter.resolve = (r)=>{
                    resolve(r)
                    
                }
                adapter.reject = (r)=>{
                    reject(r)
                    
                }
            })
            return adapter
        })()
    """.trimIndent()
    )

    fun newPromiseAdapter(): PromiseAdapter {
        return PromiseAdapter(executor.execute(), eventLoopQueue)
    }

    class PromiseAdapter(
        private val adapter: V8ValueObject,
        private val eventLoopQueue: EventLoopQueue
    ):AutoCloseable {
        @Volatile
        private var promiseStatus = PENDING
        val promise: V8ValuePromise
            get() = adapter.get("promise")
        fun resolve(arg: Any?) {
            if (promiseStatus != PENDING) {
                return
            }

            eventLoopQueue.addTask {
                val resolve = adapter.get<V8ValueFunction>("resolve")
                resolve.use { it.callVoid(null, arg) }
                close()
            }
            promiseStatus = FULFILLED

        }

        fun reject(arg: Any?) {
            if (promiseStatus != PENDING) {
                return
            }
            eventLoopQueue.addTask {
                val reject = adapter.get<V8ValueFunction>("reject")
                reject.use { it.callVoid(null, arg) }
                close()
            }
            promiseStatus = REJECTED
        }

        companion object {
            const val PENDING = 0
            const val FULFILLED = 1
            const val REJECTED = 2
        }

        override fun close() {
            adapter.close()
        }
    }
}