package org.autojs.autojs.devplugin

import android.util.Log
import com.google.gson.JsonObject
import java.util.HashMap

/**
 * Created by Stardust on 2017/5/11.
 */
open class Router(val key: String) : Handler {

    private val handlerMap: MutableMap<String, Handler> = HashMap()

    fun handler(value: String, handler: Handler): Router {
        handlerMap[value] = handler
        return this
    }

    override fun handle(data: JsonObject): Boolean {
        Log.d(LOG_TAG, "handle: $data")
        val key = data[key]
        if (key == null || !key.isJsonPrimitive) {
            Log.w(LOG_TAG, "no such key: $key")
            return false
        }
        val handler = handlerMap[key.asString]
        return handleInternal(data, key.asString, handler)
    }

    protected open fun handleInternal(json: JsonObject, key: String?, handler: Handler?): Boolean {
        return handler != null && handler.handle(json)
    }

    class RootRouter(key: String) : Router(key) {
        override fun handleInternal(json: JsonObject, key: String?, handler: Handler?): Boolean {
            val data = json["data"]
            if (data == null || !data.isJsonObject) {
                Log.w(LOG_TAG, "json has no object data: $json")
                return false
            }
            return handler != null && handler.handle(data.asJsonObject)
        }
    }

    companion object {
        private const val LOG_TAG = "Router"
    }
}