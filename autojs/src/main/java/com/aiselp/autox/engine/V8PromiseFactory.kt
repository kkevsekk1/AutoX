package com.aiselp.autox.engine

import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueFunction
import com.caoccao.javet.values.reference.V8ValueObject
import com.caoccao.javet.values.reference.V8ValuePromise

class V8PromiseFactory(val runtime: V8Runtime, private val eventLoopQueue: EventLoopQueue) {
    private val executor = runtime.getExecutor(
        """
        (()=>{
            const adapter = {};
            const stmid = setInterval(() => { }, 1000);
            adapter.promise = new Promise(function(resolve, reject) {
                adapter.resolve = (r)=>{
                    resolve(r)
                    clearInterval(stmid)
                }
                adapter.reject = (r)=>{
                    reject(r)
                    clearInterval(stmid)
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
            val resolve = adapter.get<V8ValueFunction>("resolve")
            eventLoopQueue.addTask {
                resolve.use { it.callVoid(null, arg) }
            }
            promiseStatus = FULFILLED
            close()
        }

        fun reject(arg: Any?) {
            if (promiseStatus != PENDING) {
                return
            }
            val reject = adapter.get<V8ValueFunction>("reject")
            eventLoopQueue.addTask {
                reject.use { it.callVoid(null, arg) }
            }
            promiseStatus = REJECTED
            close()
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