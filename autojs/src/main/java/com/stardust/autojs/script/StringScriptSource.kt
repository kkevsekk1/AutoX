package com.stardust.autojs.script

import java.io.Reader

/**
 * Created by Stardust on 2017/4/2.
 */
class StringScriptSource : JavaScriptSource {

    override var script: String
        private set

    constructor(script: String) : super("Tmp") {
        this.script = script
    }

    constructor(name: String, script: String) : super(name) {
        this.script = script
    }

    override val scriptReader: Reader?
        get() = null
}