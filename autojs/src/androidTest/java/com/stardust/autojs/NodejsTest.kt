package com.stardust.autojs

import androidx.test.runner.AndroidJUnit4
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.enums.V8AwaitMode
import com.caoccao.javet.interfaces.IJavetAnonymous
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.converters.JavetProxyConverter
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.primitive.V8ValueInteger
import com.caoccao.javet.values.reference.V8ValuePromise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

    @Test
    fun loopTest() {
        V8Host.getNodeInstance().createV8Runtime<NodeRuntime>().use { runtime ->
            val tasks = mutableSetOf<Runnable>()
            runtime.converter = JavetProxyConverter()
            runtime.createV8ValueObject().use {
                it.bind(object {
                    @V8Function
                    fun delay(n: Int): V8ValuePromise? {
                        val v8ValuePromise = runtime.createV8ValuePromise()
                        Thread {
                            Thread.sleep(n.toLong())
                            tasks.add {
                                v8ValuePromise.resolve(n)
                                runtime.await()
                            }
                        }.start()
                        return v8ValuePromise.promise
                    }

                    @V8Function
                    fun printCurrentThread() {
                        println("CurrentThread: ${Thread.currentThread()}")
                    }
                })
                runtime.globalObject.set("r", it)
            }
            runtime.getExecutor(
                """
                    r.printCurrentThread()
                    await r.delay(1000)
                    r.printCurrentThread()
                """.trimIndent()
            ).setModule(true).execute<V8ValuePromise>()
            runBlocking {
                withTimeout(3000) {
                    while (scope.isActive) {
                        // await blocks for one second, making it impossible to process other events in the loop
                        if (!runtime.await(V8AwaitMode.RunNoWait)) {
                            break
                        }
                        tasks.forEach { it.run() }
                        tasks.clear()
                    }
                }
            }
            Thread.sleep(2000)
            println("done")
        }
    }

    @Test
    fun loopTest2() {
        V8Host.getNodeInstance().createV8Runtime<NodeRuntime>().use { runtime ->
            println("ss: ${runtime.getExecutor("123").execute<V8ValueInteger>()}")
        }
    }
}