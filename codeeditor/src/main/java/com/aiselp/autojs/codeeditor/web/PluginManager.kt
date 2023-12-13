package com.aiselp.autojs.codeeditor.web

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.aiselp.autojs.codeeditor.web.interfaces.CallEvent
import com.aiselp.autojs.codeeditor.web.interfaces.CallbackEvent
import com.aiselp.autojs.codeeditor.web.interfaces.PluginInfo
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
class PluginManager(private val jsBridge: JsBridge, private val coroutineScope: CoroutineScope) {
    companion object {
        const val TAG = "PluginManager"
        const val PluginCallEventName = "__Plugin_Call"
        private val gson = Gson()
    }

    private val pluginMap = mutableMapOf<String, WebPlugin>()

    private val webHandle = JsBridge.Handle { data, handle ->
        val call: CallEvent
        try {
            check(handle != null)
            call = gson.fromJson(data, CallEvent::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "error-call: $data")
            Log.e(TAG, e.toString())
            return@Handle
        }
        if (call.type == CallEvent.LoadType) {
            val plugin = pluginMap[call.pluginId]
            if (plugin != null) {
                val pluginInfo = PluginInfoBuilder(plugin).build()
                return@Handle handle(gson.toJson(pluginInfo), null)
            } else return@Handle handle(null, null)
        } else if (call.type == CallEvent.CallType) {
            callPluginMethod(call, handle)
        }
    }

    init {
        jsBridge.registerHandler(PluginCallEventName, webHandle)
    }

    private fun callPluginMethod(call: CallEvent, handle: JsBridge.Handle) {
        val plugin = pluginMap[call.pluginId]
        val methodName = call.method
        check(plugin != null && methodName != null) { "call plugin:${call.pluginId} method:${call.method} error" }
        val method = plugin.methods[methodName]!!
        val webCall = WebCall(call.data, handle)
        coroutineScope.launch {
            try {
                method.invoke(plugin.plugin, webCall)
            } catch (e: Exception) {
                Log.e(TAG, "web call method:${call.method} error")
                e.printStackTrace()
                webCall.onError(e)
            }
        }
    }

    fun onWebInit() = coroutineScope.launch {

    }

    fun registerPlugin(id: String, plugin: Any) {
        pluginMap[id] = WebPlugin(id, plugin)

    }

    fun removePlugin(id: String, plugin: Any) {
        pluginMap.remove(id)
    }

    class WebCall(val data: String?, private val handle: JsBridge.Handle) {

        fun onSuccess(data: String?) {
            val callbackEvent = CallbackEvent()
            callbackEvent.type = CallbackEvent.SuccessType
            callbackEvent.data = data
            callbackEvent.errorMessage = null

            handle(gson.toJson(callbackEvent), null)
        }

        fun onError(err: Exception) {
            val callbackEvent = CallbackEvent()
            callbackEvent.type = CallbackEvent.ErrorType
            callbackEvent.data = null
            callbackEvent.errorMessage = err.message

            handle(gson.toJson(callbackEvent), null)
        }
    }

    class PluginInfoBuilder(val plugin: WebPlugin) {
        fun build(): PluginInfo {
            return PluginInfo().apply {
                pluginId = plugin.id
                methods = plugin.methods.keys.toTypedArray()
            }
        }
    }
}