package com.stardust.autojs.runtime.api

import android.content.Context
import android.graphics.drawable.Drawable
import com.stardust.autojs.core.graphics.ScriptCanvasView
import com.stardust.autojs.core.ui.inflater.DynamicLayoutInflater
import com.stardust.autojs.core.ui.inflater.ResourceParser
import com.stardust.autojs.core.ui.inflater.inflaters.CanvasViewInflater
import com.stardust.autojs.core.ui.inflater.inflaters.JsGridViewInflater
import com.stardust.autojs.core.ui.inflater.inflaters.JsImageViewInflater
import com.stardust.autojs.core.ui.inflater.inflaters.JsListViewInflater
import com.stardust.autojs.core.ui.widget.JsGridView
import com.stardust.autojs.core.ui.widget.JsImageView
import com.stardust.autojs.core.ui.widget.JsListView
import com.stardust.autojs.rhino.ProxyObject
import com.stardust.autojs.runtime.ScriptRuntime
import org.mozilla.javascript.Scriptable
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Stardust on 2017/5/14.
 */
class UI(private val mContext: Context, private val mRuntime: ScriptRuntime) : ProxyObject() {
    private val mProperties: MutableMap<String, Any> = ConcurrentHashMap()

    val resourceParser: ResourceParser = ResourceParser(Drawables())
    val layoutInflater: DynamicLayoutInflater =DynamicLayoutInflater(resourceParser)

    init {
        layoutInflater.context = mContext
        layoutInflater.registerViewAttrSetter(
            JsImageView::class.java.name,
            JsImageViewInflater(resourceParser)
        )
        layoutInflater.registerViewAttrSetter(
            JsListView::class.java.name,
            JsListViewInflater<JsListView?>(resourceParser, mRuntime)
        )
        layoutInflater.registerViewAttrSetter(
            JsGridView::class.java.name,
            JsGridViewInflater<JsGridView?>(resourceParser, mRuntime)
        )
        layoutInflater.registerViewAttrSetter(
            ScriptCanvasView::class.java.name,
            CanvasViewInflater(resourceParser, mRuntime)
        )
        mProperties["layoutInflater"] = layoutInflater
    }

    var bindingContext: Any?
        get() = mProperties["bindingContext"]
        set(context) {
            if (context == null) mProperties.remove("bindingContext") else mProperties["bindingContext"] =
                context
        }

    override fun getClassName(): String {
        return UI::class.java.simpleName
    }

    override fun get(name: String, start: Scriptable): Any {
        val value = mProperties[name]
        return value ?: super.get(name, start)
    }

    override fun put(name: String, start: Scriptable, value: Any) {
        if (mProperties.containsKey(name)) {
            mProperties[name] = value
        } else {
            super.put(name, start, value)
        }
    }

    fun recycle() {
        layoutInflater.context = null
    }

    private inner class Drawables : com.stardust.autojs.core.ui.inflater.util.Drawables() {
        override fun decodeImage(path: String): Drawable {
            return super.decodeImage(mRuntime.files.path(path))
        }
    }
}