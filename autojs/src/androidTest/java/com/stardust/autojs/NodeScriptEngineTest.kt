package com.stardust.autojs

import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import com.aiselp.autox.engine.NodeScriptSource
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeScriptEngineTest {
    private val factory = TestNodeScriptEngineFactory(ApplicationProvider.getApplicationContext())

    init {
        //TestAutojs.init(factory.context)
        TestNodeScriptEngineFactory.init(factory.context)
    }

    @Test
    fun baseTest() {
        val engine = factory.createNodeScriptEngine()
        val file = TestNodeScriptEngineFactory.createCacheFile(
            """
            console.log('hello world')
            setTimeout(() => {
                console.log('hello world2')
            }, 1000)
            //export default "123"
        """.trimIndent(), "mjs"
        )

        val any = factory.runScript<NodeScriptSource>(NodeScriptSource(file), engine)
        println("result: $any")
    }
}