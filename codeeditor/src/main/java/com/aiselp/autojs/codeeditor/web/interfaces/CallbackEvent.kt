package com.aiselp.autojs.codeeditor.web.interfaces

class CallbackEvent {
    companion object {
        const val SuccessType = "success"
        const val ErrorType = "error"
    }

    var type: String = "null"
    var data: String? = null
    var errorMessage: String? = null
}