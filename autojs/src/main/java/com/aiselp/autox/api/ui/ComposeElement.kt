package com.aiselp.autox.api.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aiselp.autox.engine.EventLoopQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class ComposeElement(val tag: String) : ComposeNode {
    override var id: Int = 0
    override var modifier: Modifier = Modifier
    override var parentNode: ComposeElement? = null
    override val props = mutableMapOf<String, Any?>()
    private val templateElements = mutableMapOf<String, ComposeElement>()
    val modifierExts = mutableListOf<ModifierExtFactory.ModifierExt>()
    val children = mutableListOf<ComposeElement>()
    var status = Status.UnMounted
    var update by mutableStateOf(false)

    fun removeChild(element: ComposeElement) {
        children.remove(element)
        element.parentNode = null
    }

    fun insertChild(child: ComposeElement, ref: ComposeElement?) {
        if (ref == null) {
            children.add(child)
        } else {
            val i = children.indexOf(ref)
            if (i != -1) {
                children.add(i, child)
            } else children.add(child)
        }
        child.parentNode = this
    }

    fun reRender(scope: CoroutineScope) {
        if (status != Status.Mounted) {
            return
        }
        status = Status.UnMounted
        scope.launch(Dispatchers.Main) {
            update = !update
        }
    }

    override fun getEvent(name: String): EventLoopQueue.V8Callback? {
        return super.getEvent(name)
    }

    fun addModifierExt(ext: ModifierExtFactory.ModifierExt) {
        modifierExts.add(ext)
    }

    fun clearModifierExts() {
        modifierExts.clear()
    }

    fun addTemplate(name: String, element: ComposeElement) {
        element.parentNode = this
        templateElements[name] = element
    }

    fun removeTemplate(name: String) {
        val element = templateElements.remove(name)
        element?.parentNode = null
    }

    fun findTemplate(name: String): ComposeElement? {
        return templateElements[name]
    }

    fun unMount() {
        status = Status.UnMounting
        parentNode?.removeChild(this)
        parentNode = null
        status = Status.UnMounted
    }

    fun reset() {
        status = Status.UnMounting
        parentNode?.removeChild(this)
        parentNode = null
        children.clear()
        templateElements.clear()
        status = Status.UnMounted
    }

    enum class Status {
        Mounted,
        Mounting,
        UnMounting,
        UnMounted
    }
}