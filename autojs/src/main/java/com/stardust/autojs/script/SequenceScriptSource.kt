package com.stardust.autojs.script

import com.stardust.io.ConcatReader
import java.io.Reader
import java.io.StringReader

/**
 * Created by Stardust on 2017/4/2.
 */
class SequenceScriptSource(
    name: String?,
    private val mFirstScriptSource: JavaScriptSource,
    private val mSecondScriptSource: JavaScriptSource
) : JavaScriptSource(
    name!!
) {
    private var mScript: String? = null
    override val script: String
        get() {
            concatScriptsIfNeeded()
            return mScript!!
        }

    private fun concatScriptsIfNeeded() {
        if (mScript != null) return
        mScript = mFirstScriptSource.script + mSecondScriptSource.script
    }

    override val scriptReader: Reader
        get() = if (mScript != null) StringReader(mScript) else ConcatReader(
            mFirstScriptSource.nonNullScriptReader,
            mSecondScriptSource.nonNullScriptReader
        )

    override fun toString(): String {
        return mSecondScriptSource.toString()
    }
}