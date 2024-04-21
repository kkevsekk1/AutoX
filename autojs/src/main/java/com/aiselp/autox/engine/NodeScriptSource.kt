package com.aiselp.autox.engine

import com.stardust.autojs.script.ScriptSource
import java.io.File

class NodeScriptSource(val file: File) : ScriptSource(file.name) {


    init {

    }

    constructor(path: String) : this(File(path))

    fun getRawText(): String {
        return file.readText()
    }

    override fun toString(): String {
        return "NodeScript@${file.path}"
    }

    override val engineName: String
        get() = NodeScriptEngine.ID
}