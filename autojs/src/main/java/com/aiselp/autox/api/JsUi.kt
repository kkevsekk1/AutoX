package com.aiselp.autox.api

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import com.aiselp.autox.activity.VueUiScriptActivity
import com.aiselp.autox.api.ui.ActivityEvent
import com.aiselp.autox.api.ui.ActivityEventDelegate
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.ComposeTextNode
import com.aiselp.autox.api.ui.Default
import com.aiselp.autox.api.ui.Filled
import com.aiselp.autox.api.ui.Icons
import com.aiselp.autox.api.ui.ModifierExtBuilder
import com.aiselp.autox.api.ui.ModifierExtFactory
import com.aiselp.autox.api.ui.ScriptActivityBuilder
import com.aiselp.autox.engine.EventLoopQueue
import com.aiselp.autox.engine.NodeScriptEngine
import com.aiselp.autox.engine.V8PromiseFactory
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.interop.converters.JavetProxyConverter
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.primitive.V8ValueString
import com.caoccao.javet.values.reference.V8ValueFunction
import com.caoccao.javet.values.reference.V8ValueObject
import com.caoccao.javet.values.reference.V8ValuePromise
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.full.memberProperties

class JsUi(nodeScriptEngine: NodeScriptEngine) : NativeApi {
    override val moduleId: String = "ui"
    private val scope: CoroutineScope = nodeScriptEngine.scope
    private val context: Context = nodeScriptEngine.context
    private val eventLoopQueue: EventLoopQueue = nodeScriptEngine.eventLoopQueue
    private val converter: JavetProxyConverter = nodeScriptEngine.converter
    private val promiseFactory: V8PromiseFactory = nodeScriptEngine.promiseFactory

    private val modifierExtFactory = ModifierExtFactory(eventLoopQueue)
    private val activitys = mutableSetOf<Activity>()
    override fun install(v8Runtime: V8Runtime, global: V8ValueObject): NativeApi.BindingMode {
        return NativeApi.BindingMode.PROXY
    }

    override fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
        activitys.forEach { if (!it.isDestroyed) it.finish() }
        activitys.clear()
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
    fun loadIcon(group: String, name: String): ImageVector {
        val icon = Icons::class.memberProperties.find {
            it.name == name
        } ?: throw RuntimeException("not found icon: $group.$name")
        return when (group) {
            "Default" -> icon.get(Default) as ImageVector
            "Filled" -> icon.get(Filled) as ImageVector
            else -> throw RuntimeException("not found icon: $group.$name")
        }
    }

    @V8Function
    fun getModifierExtFactory(key: String): ModifierExtBuilder {
        return modifierExtFactory.getModifierExtBuilder(key)
            ?: throw RuntimeException("not found modifier ext: $key")
    }

    @V8Function
    fun patchProp(element: ComposeElement, key: String, value: V8Value?) {
        val value1 = converterValue(value)
        element.props[key]?.let {
            if (it is EventLoopQueue.V8Callback) {
                it.remove()
            }
        }
        element.props[key] = value1
    }


    @V8Function
    fun startActivity(element: ComposeElement, listener: V8ValueFunction?): V8ValuePromise {
        val promiseAdapter = promiseFactory.newPromiseAdapter()
        val activityEventDelegate = createActivityEventDelegate(listener)
        val builder = ScriptActivityBuilder(element, activityEventDelegate)
        VueUiScriptActivity.startActivity(context, builder, object : VueUiScriptActivity.Lifecycle {
            override fun onCreate(activity: VueUiScriptActivity) {
                activitys.add(activity)
                promiseAdapter.resolve(activity)
            }

            override fun onDestroy(activity: VueUiScriptActivity) {
                activitys.remove(activity)
            }

        })
        return promiseAdapter.promise
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