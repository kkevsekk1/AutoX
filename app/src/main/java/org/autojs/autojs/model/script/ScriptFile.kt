package org.autojs.autojs.model.script

import java.io.File

/**
 * Created by Stardust on 2017/1/23.
 */
open class ScriptFile : com.stardust.autojs.script.ScriptFile {
    constructor(path: String) : super(path) {}
    constructor(parent: String, name: String) : super(parent, name) {}
    constructor(parent: File, child: String) : super(parent, child) {}
    constructor(file: File) : super(file.path) {}

    override fun getParentFile(): ScriptFile? {
        return this.parent?.let { ScriptFile(it) }
    }

}