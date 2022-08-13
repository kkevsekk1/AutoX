package org.autojs.autojs.timing

import org.autojs.autojs.storage.database.BaseModel
import android.content.IntentFilter
import android.content.IntentFilter.MalformedMimeTypeException

class IntentTask : BaseModel() {
    var scriptPath: String? = null
    var action: String? = null
    var category: String? = null
    var dataType: String? = null
    var isLocal = false
    val intentFilter: IntentFilter
        get() {
            val filter = IntentFilter()
            action?.let { filter.addAction(it) }
            category?.let { filter.addCategory(it) }
            try {
                dataType?.let { filter.addDataType(it) }
            } catch (e: MalformedMimeTypeException) {
                e.printStackTrace()
            }
            return filter
        }

    companion object {
        const val TABLE = "IntentTask"
    }
}