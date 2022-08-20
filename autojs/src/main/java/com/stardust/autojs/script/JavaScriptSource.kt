package com.stardust.autojs.script

import android.util.Log
import com.stardust.autojs.rhino.TokenStream
import com.stardust.util.MapBuilder
import org.mozilla.javascript.Token
import java.io.Reader
import java.io.StringReader

/**
 * Created by Stardust on 2017/8/2.
 */
abstract class JavaScriptSource(name: String) : ScriptSource(name) {
    private var mExecutionMode = -1
    abstract val script: String
    abstract val scriptReader: Reader?
    val nonNullScriptReader: Reader
        get() = scriptReader ?: StringReader(script)

    override fun toString(): String {
        return "$name.js"
    }

    val executionMode: Int
        get() {
            if (mExecutionMode == -1) {
                mExecutionMode = parseExecutionMode()
            }
            return mExecutionMode
        }

    protected open fun parseExecutionMode(): Int {
        val script = script
        val ts = TokenStream(StringReader(script), null, 1)
        var token = 0
        var count = 0
        try {
            while (count <= PARSING_MAX_TOKEN && ts.token.also { token = it } != Token.EOF) {
                count++
                if (token == Token.EOL || token == Token.COMMENT) {
                    continue
                }
                if (token == Token.STRING && ts.tokenLength > 2) {
                    val tokenString = script.substring(ts.tokenBeg + 1, ts.tokenEnd - 1)
                    if (ts.token != Token.SEMI) {
                        break
                    }
                    Log.d(LOG_TAG, "string = $tokenString")
                    return parseExecutionMode(tokenString.split(" ").toTypedArray())
                }
                break
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return EXECUTION_MODE_NORMAL
        }
        return EXECUTION_MODE_NORMAL
    }

    private fun parseExecutionMode(modeStrings: Array<String>): Int {
        var mode = 0
        for (modeString in modeStrings) {
            val i = EXECUTION_MODES[modeString]
            if (i != null) {
                mode = mode or i
            }
        }
        return mode
    }

    override val engineName: String
        get() = ENGINE

    companion object {
        const val ENGINE = "com.stardust.autojs.script.JavaScriptSource.Engine"
        const val EXECUTION_MODE_UI_PREFIX = "\"ui\";"
        const val EXECUTION_MODE_NORMAL = 0
        const val EXECUTION_MODE_UI = 0x00000001
        const val EXECUTION_MODE_AUTO = 0x00000002
        private const val LOG_TAG = "JavaScriptSource"
        private val EXECUTION_MODES = MapBuilder<String, Int>()
            .put("ui", EXECUTION_MODE_UI)
            .put("auto", EXECUTION_MODE_AUTO)
            .build()
        private const val PARSING_MAX_TOKEN = 300
    }
}