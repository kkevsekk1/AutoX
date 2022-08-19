package com.stardust.autojs.script

import java.io.File

/**
 * Created by Stardust on 2017/8/2.
 */
class AutoFileSource(val file: File) : ScriptSource(
    file.nameWithoutExtension
) {

    constructor(path: String) : this(File(path)) {}

    override val engineName: String
        get() = ENGINE

    override fun toString(): String {
        return file.toString()
    }

    companion object {
        @JvmField
        val ENGINE = AutoFileSource::class.java.name + ".Engine"
    }
}