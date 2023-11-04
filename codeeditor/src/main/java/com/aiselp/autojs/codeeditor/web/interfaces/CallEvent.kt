package com.aiselp.autojs.codeeditor.web.interfaces

class CallEvent {
    companion object {
        const val LoadType = "load"
        const val CallType = "call"
    }

    var type: String = "init"
    var pluginId: String = "null"
    var method: String? = null
    var data: String? = null
}