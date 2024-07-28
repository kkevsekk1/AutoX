package org.autojs.autojs.ui.log

import android.content.Context
import android.content.Intent

class LogActivityKt : LogActivity() {

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, LogActivityKt::class.java))
        }
    }
}