package com.aiselp.autox.api.ui

import androidx.compose.ui.Modifier
import com.aiselp.autox.engine.EventLoopQueue

open interface ComposeNode {
    var id: Int
    var parentNode: ComposeElement?
    val props: MutableMap<String, Any?>
    var modifier: Modifier
    fun getEvent(name: String): EventLoopQueue.V8Callback? {
        return this.props[name] as? EventLoopQueue.V8Callback
    }
    fun getSlot(name: String): ComposeElement? {
        return this.props[name] as? ComposeElement
    }
}