package com.stardust.autojs

import androidx.test.runner.AndroidJUnit4
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interfaces.IJavetAnonymous
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.converters.JavetProxyConverter
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.V8ValuePromise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodejsTest {
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    @Test
    fun test1() {
        println(12345)
    }

    @Test
    fun test2(): Unit = V8Host.getNodeInstance().createV8Runtime<NodeRuntime>().use { runtime ->
        runtime.converter = JavetProxyConverter()
        runtime.globalObject.set("l", object {
            @V8Function
            fun log(msg: V8Value) = println(runtime.converter.toObject<Any?>(msg))
        })
        runtime.globalObject.set("a", object : IJavetAnonymous {
            @V8Function(thisObjectRequired = true)
            fun readFile(thisObj: V8Value, path: String): V8ValuePromise? {
                val v8ValuePromise = thisObj.v8Runtime.createV8ValuePromise()
                v8ValuePromise.resolve("hello")
                scope.launch {
                    delay(1000)
                    v8ValuePromise.resolve("hello2")
                }
                return v8ValuePromise.promise
            }
        })

        runtime.getExecutor(
            """
                l.log(123)
                var t = a.readFile("hello")
                l.log(t)
                if (await t !== "hello") throw new Error("fail")
            """.trimIndent()
        ).setModule(true).execute<V8ValuePromise>()
        runtime.await()
        println("done")
        runtime.globalObject.delete("l")
        runtime.globalObject.delete("log")
    }

}