package com.stardust.autojs.runtime

import android.os.Build
import androidx.annotation.RequiresApi
import com.stardust.automator.UiObjectCollection
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.annotations.JSFunction

/**
 * Created by Stardust on 2017/7/21.
 */
class ScriptBridges {
    companion object {
        fun <T> useJsContext(f: (context: Context) -> T): T {
            val context = Context.getCurrentContext()
            try {
                return f(context ?: Context.enter())
            } finally {
                context ?: Context.exit()
            }
        }
    }

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
    fun callFunction(func: Any?, target: Any?, args: Array<*>): Any = useJsContext<Any> { context ->
        val jsFn = func as BaseFunction
        val scope = jsFn.parentScope
        val arg = args.map { Context.javaToJS(it, scope) }.toTypedArray()

        return@useJsContext jsFn.call(
            context, scope,
            (Context.javaToJS(target, scope) as? Scriptable) ?: Undefined.SCRIPTABLE_UNDEFINED,
            arg
        )
    }

    @JSFunction
    fun toArray(c: Iterable<*>): Scriptable = useJsContext<Scriptable> { context ->
        val scope = context.initStandardObjects()
        return@useJsContext context.newArray(
            context.initStandardObjects(),
            c.map { Context.javaToJS(it, scope) }.toTypedArray()
        )
    }

    fun toString(obj: Any?): String {
        return Context.toString(obj)
    }

    fun asArray(obj: UiObjectCollection): Any = useJsContext { context ->
        val arr = toArray(obj.mNodes)
        obj::class.members.forEach {
            val name = it.name
            val method = object : BaseFunction() {
                override fun getFunctionName(): String {
                    return name
                }

                override fun call(
                    cx: Context?,
                    scope: Scriptable?,
                    thisObj: Scriptable?,
                    args: Array<out Any>?
                ): Any? {
                    return if (args != null) {
                        it.call(obj, *args)
                    } else it.call(obj)
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun getLength(): Int {
                    return it.parameters.size
                }
            }
            arr.put(name, arr, method)
        }
        return@useJsContext arr
    }
}