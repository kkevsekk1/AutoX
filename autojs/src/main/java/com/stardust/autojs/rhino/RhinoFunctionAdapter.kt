package com.stardust.autojs.rhino

import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

private typealias RhinoFunction = (
    cx: Context?,
    scope: Scriptable?,
    thisObj: Scriptable?,
    args: Array<out Any>?
) -> Any?

class RhinoFunctionAdapter(
    private val name: String = "RhinoFunctionAdapter",
    private val length: Int = 0,
    private val rhinoFunction: RhinoFunction
) : BaseFunction() {

    override fun call(
        cx: Context?,
        scope: Scriptable?,
        thisObj: Scriptable?,
        args: Array<out Any>?
    ): Any? {
        try {
            return rhinoFunction(cx, scope, thisObj, args)
        } catch (err: Exception) {
            throw Context.throwAsScriptRuntimeEx(err)
        }
    }

    override fun getFunctionName() = name
    override fun getArity() = 1
    override fun getLength() = length
}

