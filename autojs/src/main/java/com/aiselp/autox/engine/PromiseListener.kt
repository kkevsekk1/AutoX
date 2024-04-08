package com.aiselp.autox.engine

import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.IV8ValuePromise
import com.caoccao.javet.values.reference.V8ValueError
import kotlinx.coroutines.CompletableDeferred

class PromiseListener : IV8ValuePromise.IListener {
    private val result = CompletableDeferred<String>()
    var stack: String? = null

    @Volatile
    var isCatchCalled = false

    @Volatile
    var isFulfilledCalled = false

    @Volatile
    var isRejectedCalled = false
    override fun onCatch(v8Value: V8Value?) {
        isCatchCalled = true
    }

    override fun onFulfilled(v8Value: V8Value?) {
        isFulfilledCalled = true
        result.complete(v8Value.toString())
    }

    override fun onRejected(v8Value: V8Value?) {
        isRejectedCalled = true
        if (v8Value is V8ValueError) {
            stack = v8Value.stack
        }
        result.complete(v8Value.toString())
    }

    suspend fun await(): String {
        return result.await()
    }

    fun cancel() = result.cancel()
}