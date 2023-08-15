package com.stardust.autojs.runtime

import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.annotations.JSFunction

/**
 * Created by Stardust on 2017/7/21.
 */
class ScriptBridges {
    interface Bridges {
        fun call(func: Any?, target: Any?, arg: Any?): Any
        fun toArray(o: Iterable<*>?): Any
        fun toString(obj: Any?): Any
        fun asArray(obj: Any?): Any

        companion object {
            val NO_ARGUMENTS = arrayOfNulls<Any>(0)
        }
    }

    var bridges: Bridges? = null
    fun callFunction(func: Any?, target: Any?, args: Array<*>): Any {
        val jsFn = func as BaseFunction
        val cx = Context.getCurrentContext()
        val scope = jsFn.parentScope
        val arr = args.map { Context.javaToJS(it, scope) }.toTypedArray()
        try {
            return jsFn.call(
                cx ?: Context.enter(),
                scope,
                (Context.javaToJS(target, scope) as? Scriptable) ?: Undefined.SCRIPTABLE_UNDEFINED,
                arr
            )
        } finally {
            cx ?: Context.exit()
        }
    }

    private fun checkBridges() {
        checkNotNull(bridges) { "no bridges set" }
    }

    @JSFunction
    fun toArray(c: Iterable<*>): Scriptable {
        val cx = Context.getCurrentContext()
        val te = cx ?: Context.enter()
        try {
            return te.newArray(te.initStandardObjects(), c.toList().toTypedArray())
        } finally {
            cx ?: Context.exit()
        }
    }

    fun toString(obj: Any?): String {
        return Context.toString(obj)
    }

    fun asArray(list: Any?): Any {
        val arr = toArray(emptyList<Any?>())

        checkBridges()
        return bridges!!.asArray(list)
    }
}