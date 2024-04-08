package com.stardust.autojs.script

import com.aiselp.autox.engine.NodeScriptSource
import com.stardust.pio.PFile
import java.io.File

open class ScriptFile : PFile {
    constructor(path: String) : super(path)
    constructor(parent: String, name: String) : super(parent, name)
    constructor(parent: File, child: String) : super(parent, child)
    constructor(file: File) : super(file.path)

    val type: Int by lazy {
        name.let {
            if (it.endsWith("node.js")) TYPE_NODE_SCRIPT
            else if (it.endsWith(".js")) TYPE_JAVA_SCRIPT
            else if (it.endsWith(".auto")) TYPE_AUTO
            else if (it.endsWith(".mjs")) TYPE_NODE_SCRIPT
            else if (it.endsWith(".cjs")) TYPE_NODE_SCRIPT
            else TYPE_UNKNOWN
        }
    }

    open fun toSource(): ScriptSource = when (type) {
        TYPE_JAVA_SCRIPT -> JavaScriptFileSource(this)
        TYPE_NODE_SCRIPT -> NodeScriptSource(this)
        else -> AutoFileSource(this)
    }


    companion object {
        const val TYPE_UNKNOWN = 0
        const val TYPE_AUTO = 1
        const val TYPE_JAVA_SCRIPT = 2
        const val TYPE_NODE_SCRIPT = 3
    }
}