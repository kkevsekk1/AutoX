package com.stardust.autojs

import android.content.Context
import com.aiselp.autox.engine.NodeScriptEngine
import com.stardust.autojs.engine.ScriptEngine
import com.stardust.autojs.script.ScriptSource
import com.stardust.pio.PFiles
import com.stardust.util.UiHandler
import java.io.File

class TestNodeScriptEngineFactory(val context: Context) {
    val uiHandler = UiHandler(context)

    fun createNodeScriptEngine(): NodeScriptEngine {
        check(isInit) { "TestNodeScriptEngineFactory must be init first" }
        val engine = NodeScriptEngine(context, uiHandler)
        return engine
    }

    fun <T : ScriptSource> runScript(
        source: ScriptSource,
        engine: ScriptEngine<ScriptSource>
    ): Any? {
        try {
            engine.init()
            return engine.execute(source)
        } finally {
            engine.destroy()
        }
    }

    companion object {
        lateinit var scriptDir: File
        var isInit = false
        fun init(context: Context) {
            NodeScriptEngine.initModuleResource(context, true)
            scriptDir = File(context.cacheDir, "text_scripts")
            if (scriptDir.isDirectory) PFiles.deleteFilesOfDir(scriptDir)
            scriptDir.mkdirs()
            isInit = true
        }

        fun createCacheFile(text: String, suffix: String): File {
            val file = File(scriptDir, "${System.currentTimeMillis()}.$suffix")
            file.writeText(text)
            return file
        }
    }
}