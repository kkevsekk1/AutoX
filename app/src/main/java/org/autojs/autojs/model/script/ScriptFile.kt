package org.autojs.autojs.model.script

import com.stardust.pio.PFile
import com.stardust.autojs.script.ScriptSource
import com.stardust.autojs.script.JavaScriptFileSource
import com.stardust.autojs.script.AutoFileSource
import java.io.File

/**
 * Created by Stardust on 2017/1/23.
 */
open class ScriptFile : PFile {
    private var mType = -1

    constructor(path: String) : super(path) {}
    constructor(parent: String, name: String) : super(parent, name) {}
    constructor(parent: File, child: String) : super(parent, child) {}
    constructor(file: File) : super(file.path) {}

    val type: Int
        get() {
            if (mType == -1) {
                mType =
                    if (name.endsWith(".js")) TYPE_JAVA_SCRIPT
                    else if (name.endsWith(".auto")) TYPE_AUTO
                    else TYPE_UNKNOWN
            }
            return mType
        }

    override fun getParentFile(): ScriptFile? {
        val p = this.parent ?: return null
        return ScriptFile(p)
    }

    open fun toSource(): ScriptSource? {
        return if (type == TYPE_JAVA_SCRIPT) {
            JavaScriptFileSource(this)
        } else {
            AutoFileSource(this)
        }
    }

    companion object {
        const val TYPE_UNKNOWN = 0
        const val TYPE_AUTO = 1
        const val TYPE_JAVA_SCRIPT = 2
    }
}