package com.aiselp.autox.api.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.aiselp.autox.api.ui.component.parseColor
import com.aiselp.autox.engine.EventLoopQueue
import com.caoccao.javet.values.reference.V8ValueFunction

private const val TAG = "JsModifierFactory"

class ModifierExtBuilder(private val ext: Modifier.(args: List<Any?>) -> Modifier) {
    fun createModifierExt(args: List<Any?>): ModifierExt {
        return ModifierExt(args)
    }

    inner class ModifierExt(val args: List<Any?>) {
        fun apply(modifier: Modifier): Modifier {
            return modifier.ext(args)
        }
    }
}

private val modifierExts = buildMap<String, ModifierExtBuilder> {
    put("fillMaxSize", ModifierExtBuilder { fillMaxSize() })
    put("fillMaxWidth", ModifierExtBuilder { fillMaxWidth() })
    put("fillMaxHeight", ModifierExtBuilder { fillMaxHeight() })
}

fun getModifierExtBuilder(key: String): ModifierExtBuilder? {
    return modifierExts[key]
}

class ModifierBuilder(
    var modifier: Modifier = Modifier,
    private val eventLoopQueue: EventLoopQueue
) {
    fun width(width: Int) {
        modifier = modifier.width(width.dp)
    }

    fun height(height: Int) {
        modifier = modifier.height(height.dp)
    }

    fun rotate(rotate: Float) {
        modifier = modifier.rotate(rotate)
    }

    fun padding(padding: Int) {
        modifier = modifier.padding(padding.dp)
    }

    fun padding(horizontal: Int, vertical: Int) {
        modifier = modifier.padding(horizontal.dp, vertical.dp)
    }

    fun padding(left: Int, top: Int, right: Int, bottom: Int) {
        modifier = modifier.padding(left.dp, top.dp, right.dp, bottom.dp)
    }

    fun fillMaxSize() {
        modifier = modifier.fillMaxSize()
    }

    fun fillMaxWidth() {
        modifier = modifier.fillMaxWidth()
    }

    fun fillMaxHeight() {
        modifier = modifier.fillMaxHeight()
    }

    fun background(c: Any) {
        parseColor(c)?.let {
            modifier = modifier.background(it)
        }
    }

    fun click(callback: V8ValueFunction) {
        val v8Callback = eventLoopQueue.createV8Callback(callback)
        modifier = modifier.clickable { v8Callback.invoke() }
    }
}

