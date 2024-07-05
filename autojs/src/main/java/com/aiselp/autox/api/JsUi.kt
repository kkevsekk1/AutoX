package com.aiselp.autox.api

import android.content.Context
import android.util.Log
import androidx.compose.ui.Modifier
import com.aiselp.autox.activity.VueUiScriptActivity
import com.aiselp.autox.api.ui.ActivityEvent
import com.aiselp.autox.api.ui.ActivityEventDelegate
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.ComposeTextNode
import com.aiselp.autox.api.ui.ModifierBuilder
import com.aiselp.autox.api.ui.ScriptActivityBuilder
import com.aiselp.autox.engine.EventLoopQueue
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.interop.converters.JavetProxyConverter
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.primitive.V8ValueString
import com.caoccao.javet.values.reference.V8ValueFunction
import com.caoccao.javet.values.reference.V8ValueObject
import kotlinx.coroutines.CoroutineScope

class JsUi(
    private val scope: CoroutineScope,
    private val context: Context,
    private val eventLoopQueue: EventLoopQueue,
    private val converter: JavetProxyConverter
) : NativeApi {
    override val moduleId: String = "ui"
    override fun install(v8Runtime: V8Runtime, global: V8ValueObject): NativeApi.BindingMode {
        return NativeApi.BindingMode.PROXY
    }

    override fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {

    }

    @V8Function
    fun createComposeElement(
        tag: String,
        props: V8ValueObject,
        childrens: List<ComposeElement>
    ): ComposeElement {
        val element = ComposeElement(tag)
        converterV8ValueObject(props, element.props)
        element.children.addAll(childrens)
        return element
    }

    @V8Function
    fun createComposeText(text: String): ComposeTextNode {
        val textNode = ComposeTextNode(text)
        return textNode
    }

    @V8Function
    fun updateComposeElement(element: ComposeElement) {
        element.reRender(scope)
    }

    @V8Function
    fun patchProp(element: ComposeElement, key: String, value: V8Value?) {
        element.props[key] = converterValue(value)
    }

    @V8Function
    fun createModifierBuilder(modifier: Modifier?): ModifierBuilder {
        return ModifierBuilder(modifier ?: Modifier, eventLoopQueue = eventLoopQueue)
    }

    @V8Function
    fun startActivity(element: ComposeElement, listener: V8ValueFunction?) {
        val activityEventDelegate = createActivityEventDelegate(listener)
        val builder = ScriptActivityBuilder(element, activityEventDelegate)
        VueUiScriptActivity.startActivity(context, builder)
    }

    private fun createActivityEventDelegate(listener: V8ValueFunction? = null): ActivityEventDelegate {
        eventLoopQueue.keepRunning()
        return object :
            ActivityEventDelegate(listener?.let { eventLoopQueue.createV8Callback(it) }) {
            override fun emit(event: ActivityEvent, vararg args: Any?) {
                super.emit(event, *args)
                Log.d(TAG, "emit: ${event.value}")
                if (event == ActivityEvent.ON_DESTROY) {
                    eventLoopQueue.cancelKeepRunning()
                }
            }
        }
    }

    private fun converterValue(value: V8Value?): Any? {
        return if (value is V8ValueFunction) {
            eventLoopQueue.createV8Callback(value)
        } else {
            converter.toObject<Any?>(value)
        }
    }

    private fun converterV8ValueObject(
        v8Value: V8ValueObject,
        props: MutableMap<String, Any?> = mutableMapOf()
    ): MutableMap<String, Any?> {
        v8Value.forEach<V8Value, V8Value, Exception> { key, value ->
            if (key is V8ValueString) {
                props[key.toString()] = converterValue(value)
            }
        }
        return props
    }

    companion object {
        private const val TAG = "JsUi"
    }
}