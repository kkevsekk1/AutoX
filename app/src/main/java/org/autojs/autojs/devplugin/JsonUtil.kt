package org.autojs.autojs.devplugin

import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.StringReader

object JsonUtil {

    fun dispatchJson(json: String): JsonElement? {
        try {
            val reader = JsonReader(StringReader(json))
            reader.isLenient = true
            return JsonParser.parseReader(reader)
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
        return null
    }
}