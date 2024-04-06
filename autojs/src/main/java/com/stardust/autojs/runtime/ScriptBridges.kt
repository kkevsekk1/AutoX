package com.stardust.autojs.runtime

import android.os.Looper
import com.stardust.autojs.engine.RhinoJavaScriptEngine
import com.stardust.automator.UiObjectCollection
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.BoundFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJavaMethod
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.annotations.JSFunction

/**
 * Created by Stardust on 2017/7/21.
 */
class ScriptBridges {
    var engine: RhinoJavaScriptEngine? = null
    fun setup(engine: RhinoJavaScriptEngine) {
        this.engine = engine
    }

    private fun <T> useJsContext(f: (context: Context) -> T): T {
        val context = Context.getCurrentContext()
        val cx: Context = context ?: with(Context.enter()) {
            engine?.setupContext(this)
            this
        }
        try {
            return f(cx)
        } finally {
            context ?: Context.exit()
        }
    }

    fun callFunction(func: Any?, target: Any?, args: Array<*>) = useJsContext<Any?> { context ->
        val jsFn = func as BaseFunction
        val scope = jsFn.parentScope
        val arg = args.map { Context.javaToJS(it, scope) }.toTypedArray()
        return@useJsContext try {
            jsFn.call(
                context, scope,
                (Context.javaToJS(target, scope) as? Scriptable) ?: Undefined.SCRIPTABLE_UNDEFINED,
                arg
            )
        } catch (e: Exception) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                engine?.runtime?.exit(e) ?: throw e
            }else throw e
        }
    }

    @JSFunction
    fun toArray(c: Iterable<*>): Scriptable = useJsContext<Scriptable> { context ->
        val scope = context.initStandardObjects()
        return@useJsContext context.newArray(
            scope, c.map { Context.javaToJS(it, scope) }.toTypedArray()
        )
    }

    fun toString(obj: Any?): String {
        return Context.toString(obj)
    }

    fun asArray(obj: UiObjectCollection): Any = useJsContext { context ->
        val arr = toArray(obj.mNodes)
        val thzs = NativeJavaObject(arr, obj, obj.javaClass)
        obj::class.java.methods.forEach {
            val name = it.name
            val method = NativeJavaMethod(it, name)
            val bound = BoundFunction(context, arr, method, thzs, emptyArray())
            arr.put(name, arr, bound)
        }
        return@useJsContext arr
    }
}