package com.stardust.autojs.script

import java.io.Serializable

/**
 * Created by Stardust on 2017/4/2.
 */
abstract class ScriptSource(val name: String) : Serializable {
    abstract val engineName: String
}